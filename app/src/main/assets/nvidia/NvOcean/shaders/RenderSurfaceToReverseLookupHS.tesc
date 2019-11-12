#include "ocean_surface_heights.glsl"

out HS_OUT
{
    float2 m_uv;
}O[];

in VS_OUT
{
    float2 m_uv;
}I[];

layout (vertices = 4) out;

void main()
{
    O[gl_InvocationID].m_uv = I[gl_InvocationID].m_uv;

    gl_TessLevelOuter[0] = 1;
    gl_TessLevelOuter[1] = 1;
    gl_TessLevelOuter[2] = 1;
    gl_TessLevelInner[0] = 1;
    gl_TessLevelInner[1] = 1;
}