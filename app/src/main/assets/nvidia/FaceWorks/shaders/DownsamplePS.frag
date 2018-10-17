#include "SSSS_Common.glsl"

layout(binding=0) uniform sampler2D g_DepthTex;
layout(binding=1) uniform sampler2D g_StencilTex;

layout(location=0) out float Out_Depth;

in vec4 m_f4UVAndScreenPos;

float linearlizeDepth(float dDepth)
{
    float mZFar =far;
    float mZNear = near;
    return mZFar*mZNear/(mZFar-dDepth*(mZFar-mZNear));
}

void main()
{
    ivec2 size = textureSize(g_DepthTex, 0);
    int2 pos = int2(size * m_f4UVAndScreenPos.xy);
    if(texelFetch(g_StencilTex, pos, 0).x != material)
    {
        discard;
    }

    Out_Depth = texelFetch(g_DepthTex, pos, 0).x;
    gl_FragDepth = linearlizeDepth(Out_Depth);
}