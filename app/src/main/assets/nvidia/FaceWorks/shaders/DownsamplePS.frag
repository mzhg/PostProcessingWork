#include "SSSS_Common.glsl"

layout(binding=0) uniform sampler2D g_DepthTex;
layout(binding=1) uniform sampler2D g_StencilTex;

layout(location=0) out float Out_Depth;

in vec4 m_f4UVAndScreenPos;

void main()
{
    int2 pos = int2(gl_FragCoord.xy);
    if(texelFetch(g_StencilTex, pos, 0).x != material)
    {
        discard;
    }

    Out_Depth = texelFetch(g_DepthTex, pos, 0).x;
    gl_FragDepth = (projection.x * depth + projection.y) / depth * projection.z;
}