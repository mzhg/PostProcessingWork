#include "Common.glsl"
#include "GBuffer.glsl"

layout(location = 0) in block
{
    vec4 inPosition	/*: POSITION*/;
    vec3 inUV			/*: TEXCOORD0*/;
    layout(location = 2) float  inOpacity	/*: TEXCOORD1*/;
};

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

    gl_Position    = projectedPosition;

    Out.ObjPos      = position.xyz;
    Out.ViewPos 	= mul( position, mParticleWorldView ).xyz;
    Out.ViewCenter	= mul( float4(inPosition.xyz, 1.0f), mParticleWorldView).xyz;
    Out.UVS			= float3(inUV.xy, size);
    Out.Opacity		= inOpacity;
    Out.color        = float4(1,1,1,1);
    Out.ShadowInfo = float2( 1, 1 );

    if( mUI.vertexShaderShadowLookup ) //#ifdef CALCULATE_AVSM_IN_VS
    {
       DynamicParticlePSIn	vsShadowIn = Out;
       vsShadowIn.UVS.z *= 2.0;

       float3 entry, exit;
       float  shadowTerm = 1.0f;
       float  segmentTransmittance = 1.0f;
//       [flatten]
       if( IntersectDynamicParticle( vsShadowIn, entry, exit, segmentTransmittance ) )
       {
          float2 lightTexCoord = ProjectIntoLightTexCoord(entry);

          SurfaceData LitSurface = ConstructSurfaceData(entry, 0.0f.xxx);
          if (mUI.enableVolumeShadowLookup)
          {
             shadowTerm = ShadowContrib(LitSurface, vsShadowIn);
          }
          //Out.ShadowInfo = LitSurface.positionView.xy;
       }
       Out.ShadowInfo = float2( shadowTerm, 0.0 );
    } //#endif
}