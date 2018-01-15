#include "Common.glsl"
#include "GBuffer.glsl"

layout(location = 0) in float4 inPosition	/*: POSITION*/;
layout(location = 1) in float3 inUV			/*: TEXCOORD0*/;
layout(location = 2) in float  inOpacity	/*: TEXCOORD1*/;

//out VS_OUTPUT_HS_INPUT Out;

out _VS_OUTPUT_HS_INPUT
{
    float4 vWorldPos /*: WORLDPOS*/;
    float4 vScreenPos /*: SCREENPOS*/;
    float3 texCoord  /*: TEXCOORD0*/;
    float  inOpacity /*: TEXCOORD1*/;
}Out;

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