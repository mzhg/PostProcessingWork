#include "Fire_Common.glsl"

layout(location=0) out vec4 Out_Color;

in MeshVertex
{
    float3 Pos /*: POSITION*/;
    float3 Normal /*: NORMAL*/;
    float2 TexCoord /*: TEXCOORD0*/;
}In;

float ShadowCalculation()
{
    float3 realPosLight = In.Pos - LightPos;
    float length = length(realPosLight);

    float depthInMap = texture(ShadowMap, realPosLight).x;
    float mZFar =200.0;
    float mZNear = 0.2;
    float fCamSpaceZ = mZFar*mZNear/(mZFar-depthInMap*(mZFar-mZNear));

    return length - 0.05 > fCamSpaceZ ? 1.0 : 0.0;
}

void main()
{
    float3 lightDir = normalize( LightPos - In.Pos );
    float lightCol = saturate( dot( lightDir, In.Normal ) );
    float4 shadeColor = float4( 1.0f, 1.0f, 1.0f, 1.0f );
    float4 diffuseColor = texture (SceneTexture, In.TexCoord);  // SamplerRepeat

#if 0
    float4 realPosLight = float4( In.Pos - LightPos, 1.0f );
    float maxCoord = max( abs( realPosLight.x ), max( abs( realPosLight.y ), abs( realPosLight.z ) ) );
    // the math is: -1.0f / maxCoord * (Zn * Zf / (Zf - Zn) + Zf / (Zf - Zn) (should match the shadow projection matrix)
    float projectedDepth = -(1.0f / maxCoord) * (200.0 * 0.2 / (200.0 - 0.2)) + (200.0 / (200.0 - 0.2));

    float shadow = projectedDepth - 0.001f > texture(ShadowMap , In.Pos - LightPos ).r ? 1 : 0;  // SamplerClamp
#else
    float shadow = ShadowCalculation();
#endif

    // Compute the final color

    shadeColor -= float4( float3( 0.6f, 0.6f, 0.6f ), 1.0f ) * shadow;

    Out_Color = (diffuseColor * float4( lightCol,lightCol,lightCol, 1.0f ) * shadeColor * LightIntensity * pow( saturate( 150.0f / length( LightPos - In.Pos ) ), 3.0f ));
}

