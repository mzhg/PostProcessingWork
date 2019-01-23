#include "GBuffer.glsl"

layout(location = 0) out float4 Out_Color /*: SV_Target0*/;

void main()
{
    // Always use face normal for alpha tested stuff since it's double-sided
    float3 normal = ComputeFaceNormal(_input.positionView);

    // Alpha test: dead code and CSE will take care of the duplication here
    SurfaceData surface = ComputeSurfaceDataFromGeometry(normal);
    if(surface.albedo.a - 0.3f < 0.0)
        discard;

    // How many total lights?
    uint totalLights, dummy;
    gLight.GetDimensions(totalLights, dummy);

    float3 lit = float3(0.0f, 0.0f, 0.0f);

    if (mUI.visualizeLightCount) {
        lit = (float(totalLights) * rcp(255.0f)).xxx;
    } else {
        SurfaceData surface = ComputeSurfaceDataFromGeometry();
        for (uint lightIndex = 0; lightIndex < totalLights; ++lightIndex) {
            PointLight light = gLight[lightIndex];
            AccumulateBRDF(surface, light, lit);
        }
    }

    Out_Color = float4(lit, 1.0f);
}