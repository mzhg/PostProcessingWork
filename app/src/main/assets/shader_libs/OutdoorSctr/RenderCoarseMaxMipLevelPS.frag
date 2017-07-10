#include "CloudsCommon.glsl"

in float4 m_f4UVAndScreenPos;
layout(location = 0) out float Out_Color;

// This shader renders next coarse level of the maximum density mip map
//float RenderCoarseMaxMipLevelPS(SScreenSizeQuadVSOutput In) : SV_Target
void main()
{
    // Compute maximum density of 4 finer-level texels
    float2 f2MipSize;
    float fMipLevels;
    float fSrcMip = g_GlobalCloudAttribs.f4Parameter.x-1.0;
//    g_tex2MaxDensityMip.GetDimensions(fSrcMip, f2MipSize.x, f2MipSize.y, fMipLevels);
    fMipLevels = float(textureQueryLevels(g_tex2MaxDensityMip));
    f2MipSize = float2(textureSize(g_tex2MaxDensityMip, 0));
    // Note that since dst level is 2 times smaller than the src level, we have to
    // align texture coordinates
    //   _____ _____
    //  |     |     |  1
    //  |_____._____|
    //  |  .  |     |  0
    //  |_____|_____|
    //     0      1
    float2 f2UV = ProjToUV(In.m_f2PosPS.xy) - 0.5f / f2MipSize;
    float fMaxDensity = 0;
    for(int i=0; i <= +1; ++i)
        for(int j=0; j <= +1; ++j)
        {
//            float fCurrDensity = g_tex2MaxDensityMip.SampleLevel(samPointWrap, f2UV, fSrcMip, int2(i,j));
            float fCurrDensity = textureLod(g_tex2MaxDensityMip, f2UV, fSrcMip, int2(i,j));
            fMaxDensity = max(fMaxDensity, fCurrDensity);
        }
    Out_Color = fMaxDensity;
}