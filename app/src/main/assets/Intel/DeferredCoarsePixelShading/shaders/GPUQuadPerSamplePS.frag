#include "GPUQuad.glsl"

layout(location = 0) out float4 Out_Color /*: SV_Target0*/;
in flat int lightIndex /*: lightIndex*/;

void main()
{
    // Shade only sample 0
    if (mUI.visualizePerSampleShading) {
        Out_Color = float4(1, 0, 0, 1);
    } else {
        Out_Color = GPUQuad(gl_FragCoord.xy, lightIndex, gl_SampleIndex);
    }
}