#include "GBuffer.glsl"

layout(location = 0) out float4 Out_Color /*: SV_Target0*/;

void main()
{

    // Alpha test: dead code and CSE will take care of the duplication here
    SurfaceData surface = ComputeSurfaceDataFromGeometry();
    if(surface.albedo.a - 0.3f < 0.0)
        discard;

    Out_Color = float4(0.0f);
}