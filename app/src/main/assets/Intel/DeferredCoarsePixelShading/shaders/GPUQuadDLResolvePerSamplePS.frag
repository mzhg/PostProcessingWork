#include "GPUQuadDL.glsl"

void main()
{
    // Shade only sample 0
    if (mUI.visualizePerSampleShading) {
        Out_Color = float4(1, 0, 0, 1);
    } else {
        Out_Color = GPUQuadDLResolve(gl_SampleIndex);
    }
}