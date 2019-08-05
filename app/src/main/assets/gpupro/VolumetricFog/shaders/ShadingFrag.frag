#include "Common.glsl"

in vec4 m_f4UVAndScreenPos;
layout(binding = 0) uniform sampler2D g_SceneDepth;
layout(binding = 1) uniform sampler2D g_SceneColor;
layout(binding = 2) uniform sampler3D g_ScatteringTex;

out float4 OutColor;

void main()
{
    float depth = textureLod(g_SceneDepth, m_f4UVAndScreenPos.xy, 0.0).x;
    float4 Color = textureLod(g_SceneColor, m_f4UVAndScreenPos.xy, 0.0);
    float3 ClipPos = float3(m_f4UVAndScreenPos.xy, depth);

    int3 GridCoordinate = ComputeCellGrid(ClipPos, float3(.0));
    float4 Scattering = texelFetch(g_ScatteringTex, GridCoordinate, 0);

    /*ClipPos = ClipPos * 2 - 1;
    float4 worldPos = UnjitteredClipToTranslatedWorld * float4(ClipPos, 1);
    float distance = length(WorldCameraOrigin, worldPos.xyz/worldPos);*/

//    OutColor.rgb = Color.rgb * (1 - Scattering.w) + Scattering.rgb;
//    OutColor.rgb = Color.rgb * Scattering.w + Scattering.rgb;
    OutColor.rgb = Color.rgb + Scattering.rgb;
    OutColor.a = Color.a;
}