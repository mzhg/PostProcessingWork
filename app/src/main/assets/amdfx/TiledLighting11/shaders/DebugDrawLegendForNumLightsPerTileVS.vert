#include "DebugDraw.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out vec2 m_TextureUV;

void main()
{
    // convert from screen space to homogeneous projection space
    gl_Position.x =  2.0f * ( In_Position.x / float(g_uWindowWidth) )  - 1.0f;
    gl_Position.y =  2.0f * ( In_Position.y / float(g_uWindowHeight) ) - 1.0f;
    gl_Position.z = 0.0;
    gl_Position.w = 1.0;

    // pass through
    m_TextureUV =In_Texcoord;
}