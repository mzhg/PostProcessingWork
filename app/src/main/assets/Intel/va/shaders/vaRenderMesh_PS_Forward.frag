
#include "vaShared.glsl"

#include "vaSimpleShadowMap.glsl"

in RenderMeshStandardVertexInput
{
//    float4 Position             : SV_Position;
    float4 Color                ;//: COLOR;
    float4 ViewspacePos         ;//: TEXCOORD0;
    float4 ViewspaceNormal      ;//: NORMAL0;
    float4 ViewspaceTangent     ;//: NORMAL1;
    float4 ViewspaceBitangent   ;//: NORMAL2;
    float4 Texcoord0            ;//: TEXCOORD1;
}_input;

layout(binding = RENDERMESH_TEXTURE_SLOT0) uniform sampler2D g_textureAlbedo;
layout(binding = RENDERMESH_TEXTURE_SLOT1) uniform sampler2D g_textureNormal;
layout(binding = RENDERMESH_TEXTURE_SLOT2) uniform sampler2D g_textureSpecular;

void GetNormalAndTangentSpace( /*const GenericSceneVertexTransformed input,*/ out float3 normal, out float3x3 tangentSpace )
{
#if VA_RMM_HASNORMALMAPTEXTURE
//    float3 normal       = UnpackNormal( g_textureNormal.Sample( g_samplerAnisotropicWrap, input.Texcoord0.xy ) );
    normal          = UnpackNormal(texture(g_textureNormal, _input.Texcoord0.xy).xyz);
#else
    normal          = float3( 0.0, 0.0, 1.0 );
#endif

    tangentSpace    = float3x3(
			                    normalize( _input.ViewspaceTangent.xyz   ),
			                    normalize( _input.ViewspaceBitangent.xyz ),
                                normalize( _input.ViewspaceNormal.xyz    ) );

    normal          = mul( normal, tangentSpace );
    normal          = normalize( normal );
}

LocalMaterialValues GetLocalMaterialValues( /*const in GenericSceneVertexTransformed input, */const bool isFrontFace )
{
    LocalMaterialValues ret;

    ret.IsFrontFace     = isFrontFace;
    ret.Albedo          = _input.Color;

    GetNormalAndTangentSpace( /*input,*/ ret.Normal, ret.TangentSpace );

#if VA_RMM_HASALBEDOTEXTURE
//    ret.Albedo *= g_textureAlbedo.Sample( g_samplerAnisotropicWrap, _input.Texcoord0.xy );
    ret.Albedo *= texture(g_textureAlbedo, _input.Texcoord0.xy);
#endif

    return ret;
}

layout(location = 0) out vec4 Out_Color;

float4 MeshColor( /*const GenericSceneVertexTransformed input,*/ LocalMaterialValues lmv, float shadowTerm )
{
    const float3 diffuseLightVector     = -g_Global.Lighting.DirectionalLightViewspaceDirection.xyz;

    float3 color    = lmv.Albedo.rgb;
    float3 normal   = (lmv.IsFrontFace)?(lmv.Normal):(-lmv.Normal);

    float3 viewDir                      = normalize(_input.ViewspacePos.xyz );

    // start calculating final colour
    float3 lightAccum       = color.rgb * g_Global.Lighting.AmbientLightIntensity.rgb;

    // directional light
    float nDotL             = dot( normal, diffuseLightVector );

    float3 reflected        = diffuseLightVector - 2.0*nDotL*normal;
	float rDotV             = saturate( dot(reflected, viewDir) );
    float specular          = saturate( pow( saturate( rDotV ), 8.0 ) );

    // facing towards light: front and specular
    float lightFront        = saturate( nDotL ) * shadowTerm;
    lightAccum              += lightFront * color.rgb * g_Global.Lighting.DirectionalLightIntensity.rgb;
    //lightAccum += fakeSpecularMul * specular;

    float3 finalColor       = saturate( lightAccum );
    finalColor              = saturate( finalColor );

    // input.ViewspacePos.w is distance to camera
    finalColor              = FogForwardApply( finalColor, _input.ViewspacePos.w );

    return float4( finalColor, lmv.Albedo.a );
}

#if 0
float4 PS_Forward( const in GenericSceneVertexTransformed input, const bool isFrontFace : SV_IsFrontFace ) : SV_Target
#else
void main()
#endif
{

    if( g_Global.WireframePass > 0.0 )
    {
        Out_Color = float4( 0.5, 0.0, 0.0, 1.0 );
        return;
    }


    bool isFrontFace = gl_FrontFacing;
    float shadowTerm = 1; //SimpleShadowMapSample( input.ViewspacePos.xyz );

    LocalMaterialValues lmv = GetLocalMaterialValues(/* input,*/ isFrontFace );

#if VA_RMM_ALPHATEST
    if( lmv.Albedo.a < 0.5 ) // g_RenderMeshMaterialGlobal.AlphaCutoff
        discard;
#endif

    Out_Color = MeshColor(/* input,*/ lmv, shadowTerm );
}