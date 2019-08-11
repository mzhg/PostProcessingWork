#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec2 OutTexCoord;
layout(location = 0) out float4 OutColor;

void main()
{
    float3 UVW = float3(OutTexCoord, (float(AtmosphereLayer) + 0.5f) / float(View.AtmosphericFogInscatterAltitudeSampleNum) );
    float4 Ray = Texture3DSample(AtmosphereDeltaSRTexture, /*AtmosphereDeltaSRTextureSampler,*/ UVW);
    OutColor = Ray;
}