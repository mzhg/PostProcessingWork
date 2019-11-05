#include "OceanLODData.glsl"

uniform float _HorizDisplace;
uniform float _DisplaceClamp;
uniform float _CrestTime;

in float4 m_f4UVAndScreenPos;

out float4 OutColor;

void Flow(out float2 offsets, out float2 weights)
{
    const float period = 3.0 * _LD_Params[_LD_SliceIndex].x;
    const float half_period = period / 2.0;
    offsets = mod(float2(_CrestTime, _CrestTime + half_period), float2(period));
    weights.x = offsets.x / half_period;
    if (weights.x > 1.0) weights.x = 2.0 - weights.x;
    weights.y = 1.0 - weights.x;
}

#ifndef _DYNAMIC_WAVE_SIM_ON
#define _DYNAMIC_WAVE_SIM_ON 0
#endif

#ifndef _FLOW_ON
#define _FLOW_ON 0
#endif

void main()
{
    float3 uv_thisLod = float3(m_f4UVAndScreenPos.xy, _LD_SliceIndex);

    // go from uv out to world for the current shape texture
    const float2 worldPosXZ = UVToWorld(m_f4UVAndScreenPos.xy);

    // sample the shape 1 texture at this world pos
    const float3 uv_nextLod = WorldToUV_BiggerLod(worldPosXZ);

    float3 result = float3(0.0);
    float sss = 0.0;

    #if _FLOW_ON
    float2 flow = float2(0);
    SampleFlow(_LD_TexArray_Flow, uv_thisLod, 1.0, flow);

    float2 offsets, weights;
    Flow(offsets, weights);

    float3 uv_thisLod_flow_0 = WorldToUV(worldPosXZ - offsets[0] * flow);
    float3 uv_thisLod_flow_1 = WorldToUV(worldPosXZ - offsets[1] * flow);
    SampleDisplacements(_LD_TexArray_WaveBuffer, uv_thisLod_flow_0, weights[0], result, sss);
    SampleDisplacements(_LD_TexArray_WaveBuffer, uv_thisLod_flow_1, weights[1], result, sss);
    #else
    float4 data = textureLod(_LD_TexArray_WaveBuffer, uv_thisLod, 0.0);
    result += data.xyz;
    sss = data.w;
    #endif

    float w, h, d;
//    _LD_TexArray_AnimatedWaves.GetDimensions(w, h, d);
    {
        int3 dim = textureSize(_LD_TexArray_AnimatedWaves,0);
        w = dim.x;
        h = dim.y;
        d = dim.z;
    }

    // waves to combine down from the next lod up the chain
    if (_LD_SliceIndex < d - 1.0)
    {
        float4 dataNextLod = textureLod(_LD_TexArray_AnimatedWaves, uv_nextLod, 0.0);  //LODData_linear_clamp_sampler
        result += dataNextLod.xyz;
        sss += dataNextLod.w;
    }

#if _DYNAMIC_WAVE_SIM_ON
    {
        // convert dynamic wave sim to displacements

        float waveSimY = SampleLod(_LD_TexArray_DynamicWaves, uv_thisLod).x;
        result.y += waveSimY;

        const float2 invRes = float2(_LD_Params[_LD_SliceIndex].w, 0.0);
        const float waveSimY_px = SampleLod(_LD_TexArray_DynamicWaves, uv_thisLod + float3(invRes.xy, 0)).x;
        const float waveSimY_nx = SampleLod(_LD_TexArray_DynamicWaves, uv_thisLod - float3(invRes.xy, 0)).x;
        const float waveSimY_pz = SampleLod(_LD_TexArray_DynamicWaves, uv_thisLod + float3(invRes.yx, 0)).x;
        const float waveSimY_nz = SampleLod(_LD_TexArray_DynamicWaves, uv_thisLod - float3(invRes.yx, 0)).x;
        // compute displacement from gradient of water surface - discussed in issue #18 and then in issue #47

        // For gerstner waves, horiz displacement is proportional to derivative of vertical displacement multiplied by the wavelength
        const float wavelength_mid = 2.0 * _LD_Params[_LD_SliceIndex].x * 1.5;
        const float wavevector = 2.0 * 3.14159 / wavelength_mid;
        const float2 dydx = (float2(waveSimY_px, waveSimY_pz) - float2(waveSimY_nx, waveSimY_nz)) / (2.0 * _LD_Params[_LD_SliceIndex].x);
        float2 dispXZ = _HorizDisplace * dydx / wavevector;

        const float maxDisp = _LD_Params[_LD_SliceIndex].x * _DisplaceClamp;
        dispXZ = clamp(dispXZ, -maxDisp, maxDisp);

        result.xz += dispXZ;
        }
#endif // _DYNAMIC_WAVE_SIM_ON

        OutColor = half4(result, sss);
}