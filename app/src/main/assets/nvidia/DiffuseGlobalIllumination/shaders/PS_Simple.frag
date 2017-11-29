#include "SimpleShading.glsl"

layout(location = 0) out float4 Color    /*: SV_Target0*/;
void main()
{
    Color = g_color;
}