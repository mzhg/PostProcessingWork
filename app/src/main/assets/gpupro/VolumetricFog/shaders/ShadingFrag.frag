#include "Common.glsl"

in vec4 m_f4UVAndScreenPos;
layout(binding = 0) uniform sampler2D g_SceneDepth;
layout(binding = 1) uniform sampler2D g_SceneColor;
layout(binding = 2) uniform sampler3D g_ScatteringTex;

out float4 OutColor;

#define VISUAL_ACCUMULATION 0
#define VISUAL_BLEND 1
#define VISUAL_SCATTERING 2

#ifndef VISUAL_MODE
#define VISUAL_MODE VISUAL_BLEND
#endif

uniform int g_VisualMode = VISUAL_BLEND;
uniform int g_ScatteringSlice;
uniform bool g_Tonemap = false;

float3 tonemap(float3 C)
{
    // Filmic -- model film properties
    C = max(float3(0), C - 0.004);
    C = (C*(6.2*C+0.5))/(C*(6.2*C+1.7)+0.06);

    return pow(C, float3(2.2));
}

void main()
{
    float depth = textureLod(g_SceneDepth, m_f4UVAndScreenPos.xy, 0.0).x;
    float4 Color = textureLod(g_SceneColor, m_f4UVAndScreenPos.xy, 0.0);
    float3 ClipPos = float3(m_f4UVAndScreenPos.xy, depth);

    if(depth < 1.0){
        float3 GridCoordinate = ComputeCellGrid(ClipPos, float3(.0))/ VolumetricFog_GridSize;
        depth = GridCoordinate.z;
    }

    float4 Scattering = textureLod(g_ScatteringTex, float3(m_f4UVAndScreenPos.xy, depth), 0);

    /*ClipPos = ClipPos * 2 - 1;
    float4 worldPos = UnjitteredClipToTranslatedWorld * float4(ClipPos, 1);
    float distance = length(WorldCameraOrigin, worldPos.xyz/worldPos);*/
    if(g_VisualMode == VISUAL_BLEND)
    {
        OutColor.rgb = Color.rgb * Scattering.w + Scattering.rgb;
    }
    else if(g_VisualMode == VISUAL_ACCUMULATION)
    {
        OutColor.rgb = Color.rgb + Scattering.rgb;
    }
    else if(g_VisualMode == VISUAL_SCATTERING)
    {
        int3 size = textureSize(g_ScatteringTex, 0);
        depth = float(g_ScatteringSlice)/float(size.x);
        depth = clamp(depth, 0.0, 1.0);

        Scattering = textureLod(g_ScatteringTex, float3(m_f4UVAndScreenPos.xy, depth), 0);
        OutColor.rgb = Scattering.rgb;
    }

//    OutColor.rgb = Color.rgb * Scattering.w + Scattering.rgb;
    OutColor.a = Color.a;

    if(g_Tonemap)
    {
        OutColor.rgb = tonemap(OutColor.rgb);
    }
}