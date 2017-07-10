#include "CloudsCommon.glsl"

in float4 m_f4UVAndScreenPos;
layout(location = 0) out float Out_Color;
// This shader computes level 0 of the maximum density mip map
//float RenderMaxMipLevel0PS(SScreenSizeQuadVSOutput In) : SV_Target
void main()
{
    // Compute maximum density in the 3x3 neighborhood
    // Since dimensions of the source and destination textures are the same,
    // rasterization locations are aligned with source texels
    float2 f2UV = m_f4UVAndScreenPos.xy; // ProjToUV(In.m_f2PosPS.xy);
    float fMaxDensity = 0.0;
    for(int i=-1; i <= +1; ++i)
        for(int j=-1; j <= +1; ++j)
        {
//            float fCurrDensity = g_tex2DCloudDensity.SampleLevel(samPointWrap, f2UV, 0, int2(i,j));
            float fCurrDensity = textureLodOffset(g_tex2DCloudDensity, f2UV, 0.0, int2(i,j)).x;
            fMaxDensity = max(fMaxDensity, fCurrDensity);
        }
    Out_Color =  fMaxDensity;
}