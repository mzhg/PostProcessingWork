#include "AtmosphereCommon.ush"
#include "AtmospherePrecomputeCommon.ush"

in vec2 OutTexCoord;
layout(location = 0) out float4 OutColor;

void main()
{
    float3 UVW = float3(OutTexCoord, (float(AtmosphereLayer) + 0.5f) / float(View.AtmosphericFogInscatterAltitudeSampleNum) );
    float4 Ray = Texture3DSample(AtmosphereInscatterTexture, /*AtmosphereInscatterTextureSampler,*/ UVW);
    const float Thresh = 0.7f;
    const float Thresh2 = 0.47f;

    // Remove artifact with various height density scale
    if (UVW.y > Thresh || UVW.y < Thresh2)
    {
        float3 UVW3 = float3(UVW.x, UVW.y - 2.f / (InscatterMuNum), UVW.z);
        float4 Ray3 = Texture3DSample(AtmosphereInscatterTexture, AtmosphereInscatterTextureSampler, UVW3);
        float3 UVW4 = float3(UVW.x, UVW.y + 2.f / (InscatterMuNum), UVW.z);
        float4 Ray4 = Texture3DSample(AtmosphereInscatterTexture, AtmosphereInscatterTextureSampler, UVW4);

        if (Ray.b < Ray3.b && Ray.b < Ray4.b)
        {
            Ray.b = (Ray3.b + Ray4.b) * 0.5f;
        }

        // Remove purple color which break basic formular
        if (UVW.y > Thresh && Ray.r > Ray.g)
        {
            float3 UVW2 = float3(UVW.x, Thresh, UVW.z);
            float4 Ray2 = Texture3DSample(AtmosphereInscatterTexture, AtmosphereInscatterTextureSampler, UVW2);
            Ray.b = Ray2.b;
            Ray.g = min(Ray.g, Ray2.g);
        }
    }

    OutColor = Ray;
}