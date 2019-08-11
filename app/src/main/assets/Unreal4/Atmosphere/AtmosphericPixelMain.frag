#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in float2 OutTexCoord;
in float4 OutScreenVector;

layout(location = 0) out float4 OutColor;

#ifndef ATMOSPHERIC_NO_LIGHT_SHAFT
#define ATMOSPHERIC_NO_LIGHT_SHAFT 1
#endif

#ifndef USE_PREEXPOSURE
#define USE_PREEXPOSURE 0
#endif

void main()
{
   OutColor = GetAtmosphericFog(View.WorldCameraOrigin, ScreenVector.xyz, CalcSceneDepth(OutTexCoord), float3(0, 0, 0));
#if !ATMOSPHERIC_NO_LIGHT_SHAFT
   	float LightShaftMask = Texture2DSample(OcclusionTexture, /*OcclusionTextureSampler,*/ OutTexCoord).x;
   	OutColor.rgb = OutColor.rgb * LightShaftMask;
#endif

#if USE_PREEXPOSURE
   	OutColor.rgb *= View.PreExposure;
#endif
}