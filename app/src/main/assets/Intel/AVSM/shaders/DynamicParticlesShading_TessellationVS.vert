#include "Common.glsl"
#include "GBuffer.glsl"

layout(location = 0) in block
{
    vec4 inPosition	/*: POSITION*/;
    vec3 inUV			/*: TEXCOORD0*/;
    layout(location = 2) float  inOpacity	/*: TEXCOORD1*/;
};

out VS_OUTPUT_HS_INPUT Out;
void main()
{
    float size		= inUV.z * mParticleSize;

    // Make screen-facing
    float4 position;
    float2 offset	= inUV.xy - 0.5f;
    position.xyz	= inPosition.xyz + size * (offset.xxx * mEyeRight.xyz + offset.yyy * mEyeUp.xyz);
    position.w		= 1.0;

    float4 projectedPosition = mul( position, mParticleWorldViewProj );

    Out.vScreenPos    = projectedPosition;
    Out.vScreenPos.x = -0.5;
    Out.vWorldPos = inPosition;
    Out.texCoord = inUV;
    Out.inOpacity = inOpacity;
}