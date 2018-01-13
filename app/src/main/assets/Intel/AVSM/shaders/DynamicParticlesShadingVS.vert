#include "Common.glsl"

layout(location = 0) in float4 inPosition	/*: POSITION*/;
layout(location = 1) in float3 inUV			/*: TEXCOORD0*/;
layout(location = 2) in float  inOpacity	/*: TEXCOORD1*/;

#include "ConstantBuffers.glsl"

//out DynamicParticlePSIn Out;

out _DynamicParticlePSIn
{
//    float4 Position  : SV_POSITION;
    float3 UVS		 /*: TEXCOORD0*/;
    float  Opacity	 /*: TEXCOORD1*/;
    float3 ViewPos	 /*: TEXCOORD2*/;
    float3 ObjPos    /*: TEXCOORD3*/;
    float3 ViewCenter/*: TEXCOORD4*/;
    float4 color      /*: COLOR*/;
    float2 ShadowInfo /*: TEXCOORD5*/;
}Out;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    float size		= inUV.z * mParticleSize;

    // Make screen-facing
    float4 position;
    float2 offset	= inUV.xy - 0.5f.xx;
    position.xyz	= inPosition.xyz + size * (offset.xxx * mEyeRight.xyz + offset.yyy * mEyeUp.xyz);
    position.w		= 1.0;

    float4 projectedPosition = mul( position, mParticleWorldViewProj );

    gl_Position     = projectedPosition;

    Out.ObjPos      = position.xyz;
    Out.ViewPos 	= mul( position, mParticleWorldView ).xyz;
    Out.ViewCenter	= mul( float4(inPosition.xyz, 1.0f), mParticleWorldView).xyz;
    Out.UVS			= float3(inUV.xy, size);
    Out.Opacity		= inOpacity;
    Out.color		= float4(0,0,0,1);
    Out.ShadowInfo  = float2( 1, 1 );
}