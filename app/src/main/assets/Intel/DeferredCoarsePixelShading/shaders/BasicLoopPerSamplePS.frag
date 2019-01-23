#include "GBuffer.glsl"

layout(location = 0) out vec4 Out_Color;

void main()
{
    if (mUI.visualizePerSampleShading) {
        Out_Color = float4(1, 0, 0, 1);
    } else {
        Out_Color = BasicLoop(gl_SampleIndex);
    }
}