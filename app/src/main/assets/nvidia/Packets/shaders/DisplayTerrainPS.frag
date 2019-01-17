#include "WavePackets.glsl"

out vec4 oColor;

in vec2 Tex;
in vec3 Pos; // world space position

void main()
{
    if (Pos.y < -0.1)
        discard;
    oColor.xyz = (0.25+0.75*Pos.y)*float3(0.75, 0.75, 0.75);
    oColor.w = 1.0;
}