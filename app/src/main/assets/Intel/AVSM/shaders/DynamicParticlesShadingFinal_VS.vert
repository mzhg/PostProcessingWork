#include "Common.glsl"
#include "GBuffer.glsl"

layout(location = 0) in float4 inPosition	/*: POSITION*/;
layout(location = 1) in float3 inUV			/*: TEXCOORD0*/;
layout(location = 2) in float  inOpacity	/*: TEXCOORD1*/;

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

SurfaceData ConstructSurfaceData(float3 PosView, float3 Normal)
{
    SurfaceData Surface;
    Surface.positionView = PosView;

    /*Surface.positionViewDX = ddx(Surface.positionView);
    Surface.positionViewDY = ddy(Surface.positionView);
    Surface.normal = Normal;
    Surface.albedo = float4(0,0,0,1);
    Surface.lightSpaceZ = mul(float4(Surface.positionView.xyz, 1.0f), mCameraViewToLightProj).z;
    Surface.lightTexCoord = ProjectIntoLightTexCoord(Surface.positionView.xyz);
    Surface.lightTexCoordDX = ddx(Surface.lightTexCoord);
    Surface.lightTexCoordDY = ddy(Surface.lightTexCoord); */

    return Surface;
}

void main()
{
    float size		= inUV.z * mParticleSize;

    // Make screen-facing
    float4 position;
    float2 offset	= inUV.xy - 0.5f.xx;
    position.xyz	= inPosition.xyz + size * (offset.xxx * mEyeRight.xyz + offset.yyy * mEyeUp.xyz);
    position.w		= 1.0;

    float4 projectedPosition = mul( position, mParticleWorldViewProj );

    gl_Position    = projectedPosition;

    Out.ObjPos      = position.xyz;
    Out.ViewPos 	= mul( position, mParticleWorldView ).xyz;
    Out.ViewCenter	= mul( float4(inPosition.xyz, 1.0f), mParticleWorldView).xyz;
    Out.UVS			= float3(inUV.xy, size);
    Out.Opacity		= inOpacity;
    Out.color        = float4(1,1,1,1);
    Out.ShadowInfo = float2( 1, 1 );

    if( mUI.vertexShaderShadowLookup != 0u) //#ifdef CALCULATE_AVSM_IN_VS
    {
       DynamicParticlePSIn	vsShadowIn;
       vsShadowIn.UVS = Out.UVS;
       vsShadowIn.Opacity = Out.Opacity;
       vsShadowIn.ViewPos = Out.ViewPos;
       vsShadowIn.ObjPos = Out.ObjPos;
       vsShadowIn.ViewCenter = Out.ViewCenter;
       vsShadowIn.color = Out.color;
       vsShadowIn.ShadowInfo = Out.ShadowInfo;

       vsShadowIn.UVS.z *= 2.0;

       float3 entry, exit;
       float  shadowTerm = 1.0f;
       float  segmentTransmittance = 1.0f;
//       [flatten]
       if( IntersectDynamicParticle( vsShadowIn, entry, exit, segmentTransmittance ) )
       {
          float2 lightTexCoord = ProjectIntoLightTexCoord(entry);

          SurfaceData LitSurface = ConstructSurfaceData(entry, 0.0f.xxx);
          if (mUI.enableVolumeShadowLookup != 0u)
          {
             shadowTerm = ShadowContrib(LitSurface, vsShadowIn);
          }
          //Out.ShadowInfo = LitSurface.positionView.xy;
       }
       Out.ShadowInfo = float2( shadowTerm, 0.0 );
    } //#endif
}