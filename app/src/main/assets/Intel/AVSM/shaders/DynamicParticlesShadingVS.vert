#include "Common.glsl"

layout(location = 0) in float4 inPosition	/*: POSITION*/;
layout(location = 1) in float3 inUV			/*: TEXCOORD0*/;
layout(location = 2) in float  inOpacity	/*: TEXCOORD1*/;

out DynamicParticlePSIn Out;

void main()
{
    float size		= inUV.z * mParticleSize;

    // Make screen-facing
    float4 position;
    float2 offset	= inUV.xy - 0.5f.xx;
    position.xyz	= inPosition.xyz + size * (offset.xxx * mEyeRight.xyz + offset.yyy * mEyeUp.xyz);
    position.w		= 1.0;

    float4 projectedPosition = mul( position, mParticleWorldViewProj );

    Out.Position    = projectedPosition;

    Out.ObjPos      = position.xyz;
    Out.ViewPos 	= mul( position, mParticleWorldView ).xyz;
    Out.ViewCenter	= mul( float4(inPosition.xyz, 1.0f), mParticleWorldView).xyz;
    Out.UVS			= float3(inUV.xy, size);
    Out.Opacity		= inOpacity;
    Out.color		= float4(0,0,0,1);
    Out.ShadowInfo  = float2( 1, 1 );
}