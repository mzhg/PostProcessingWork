#include "CommonHeader.glsl"

in VS_OUTPUT_SCENE
{
//    float4 Position     : SV_POSITION; // vertex position
    float3 Normal       /*: NORMAL*/;      // vertex normal vector
    float2 TextureUV    /*: TEXCOORD0*/;   // vertex texture coords
    float3 Tangent      /*: TEXCOORD1*/;   // vertex tangent vector
}Input;

layout(location = 0) out float4 RT0 /*: SV_TARGET0*/;  // Diffuse
layout(location = 1) out float4 RT1 /*: SV_TARGET1*/;  // Normal
#if ( NUM_GBUFFER_RTS >= 3 )
layout(location = 2) out float4 RT2 /*: SV_TARGET2*/;  // Dummy
#endif
#if ( NUM_GBUFFER_RTS >= 4 )
layout(location = 3) out float4 RT3 /*: SV_TARGET3*/;  // Dummy
#endif
#if ( NUM_GBUFFER_RTS >= 5 )
layout(location = 4) out float4 RT4 /*: SV_TARGET4*/;  // Dummy
#endif

layout(binding = 0) uniform sampler2D g_TxDiffuse;
layout(binding = 1) uniform sampler2D g_TxNormal;

//--------------------------------------------------------------------------------------
// This shader calculates diffuse and specular lighting for all lights.
//--------------------------------------------------------------------------------------
//PS_OUTPUT RenderSceneToGBufferPS( VS_OUTPUT_SCENE Input )
void main()
{
    // diffuse rgb, and spec mask in the alpha channel
    float4 DiffuseTex = texture( g_TxDiffuse, Input.TextureUV );

#if ( USE_ALPHA_TEST == 1 )
    float fAlpha = DiffuseTex.a;
    if( fAlpha < g_fAlphaTest ) discard;
#endif

    // get normal from normal map
    float3 vNorm = texture( g_TxNormal, Input.TextureUV ).xyz;
    vNorm *= 2;
    vNorm -= float3(1,1,1);

    // transform normal into world space
    float3 vBinorm = normalize( cross( Input.Normal, Input.Tangent ) );
    float3x3 BTNMatrix = float3x3( vBinorm, Input.Tangent, Input.Normal );
    vNorm = normalize(mul( vNorm, BTNMatrix ));

#if ( USE_ALPHA_TEST == 1 )
    RT0 = DiffuseTex;
    RT1 = float4(0.5*vNorm + 0.5, 0);
#else
    RT0 = float4(DiffuseTex.rgb, 1);
    RT1 = float4(0.5*vNorm + 0.5, DiffuseTex.a);
#endif

    // write dummy data to consume more bandwidth,
    // for performance testing
#if ( NUM_GBUFFER_RTS >= 3 )
    RT2 = float4(1,1,1,1);
#endif
#if ( NUM_GBUFFER_RTS >= 4 )
    RT3 = float4(1,1,1,1);
#endif
#if ( NUM_GBUFFER_RTS >= 5 )
    RT4 = float4(1,1,1,1);
#endif
}