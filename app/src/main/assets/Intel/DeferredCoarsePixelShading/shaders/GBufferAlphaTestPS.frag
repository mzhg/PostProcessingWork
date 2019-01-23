#include "GBuffer.glsl"

layout(location = 0) out float4 normal_specular /*: SV_Target0*/;
layout(location = 1) out float4 albedo /*: SV_Target1*/;
layout(location = 2) out float4 biased_albedo    /*: SV_Target2*/;
layout(location = 3) out float2 positionZGrad /*: SV_Target3*/;
layout(location = 4) out float  positionZ        /*: SV_Target4*/;

void main()
{
    SurfaceData surface = ComputeSurfaceDataFromGeometry();
    normal_specular = float4(EncodeSphereMap(surface.normal),
                                           surface.specularAmount,
                                           surface.specularPower);
    albedo = surface.albedo;
    if (mUI.faceNormals) {
        biased_albedo = surface.biased_albedo;
    }
    positionZGrad = float2(ddx_coarse(surface.positionView.z),
                                         ddy_coarse(surface.positionView.z));
    positionZ = surface.positionView.z;

    // Alpha test
    if(albedo.a - 0.3f < 0.0)
        discard;

    // Always use face normal for alpha tested stuff since it's double-sided
    outputGBuffer.normal_specular.xy = EncodeSphereMap(normalize(ComputeFaceNormal(_input.positionView)));
}