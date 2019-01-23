#include "GBuffer.glsl"

layout(location = 0) out float4 Out_Color /*: SV_Target0*/;

void main()
{
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