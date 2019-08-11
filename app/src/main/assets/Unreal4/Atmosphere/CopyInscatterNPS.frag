#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec2 OutTexCoord;
layout(location = 0) out float4 OutColor;

void main()
{
    float Mu, MuS, Nu;
    GetMuMuSNu(Input.Vertex.OutTexCoord, AtmosphereR, DhdH, Mu, MuS, Nu);
    float3 UVW = float3(Input.Vertex.OutTexCoord, (float(AtmosphereLayer) + 0.5f) / float(View.AtmosphericFogInscatterAltitudeSampleNum) );
    float4 Ray = Texture3DSample(AtmosphereDeltaSRTexture, /*AtmosphereDeltaSRTextureSampler,*/ UVW) / PhaseFunctionR(Nu);
    OutColor = float4(Ray.rgb, 0.f);
}