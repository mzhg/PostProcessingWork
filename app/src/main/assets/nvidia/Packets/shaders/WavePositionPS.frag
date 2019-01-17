#include "WavePackets.glsl"

out vec4 oColor;

in vec2 Tex;
in vec3 Pos; // world space position

void main()
{
    oColor = vec4(Pos, 1);
}