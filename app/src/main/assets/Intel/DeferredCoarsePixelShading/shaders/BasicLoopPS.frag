#include "GBuffer.glsl"

layout(location = 0) out vec4 Out_Color;

void main()
{
    Out_Color = BasicLoop(0);
}