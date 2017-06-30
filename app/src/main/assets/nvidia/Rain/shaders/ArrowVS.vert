#include "Rain_Common.glsl"

layout(location = 0) in vec3 In_Pos;

void main()
{
    gl_Position = g_mWorldViewProj * float4(In_Pos,1);
}