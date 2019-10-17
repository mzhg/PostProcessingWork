#include "OceanLODData.glsl"

in Varyings
{
    float2 worldPos;
    float3 uv_slice;
}_input;

layout(location = 0) out float4 OutColor;

void main()
{
    float2 displacementNormalized = float2(0.0);

    const float4 oneMinusAttenuation = 1.0 - (float4)_AttenuationInShallows;

    // sample ocean depth (this render target should 1:1 match depth texture, so UVs are trivial)
    const float depth = texture(_LD_TexArray_SeaFloorDepth, _input.uv_slice).x;

    // Preferred wave directions
#if _DIRECT_TOWARDS_POINT
    float2 offset = _input.worldPos.xy - _TargetPointData.xy;
    float preferDist = length(offset);
    float preferWt = smoothstep(_TargetPointData.w, _TargetPointData.z, preferDist);
    half2 preferredDir = preferWt * offset / preferDist;
    float4 preferredDirX = preferredDir.x;
    float4 preferredDirZ = preferredDir.y;
#endif

    float3 result = float3(0);

    // attenuate waves based on ocean depth. if depth is greater than 0.5*wavelength, water is considered Deep and wave is
    // unaffected. if depth is less than this, wave velocity decreases. waves will then bunch up and grow in amplitude and
    // eventually break. i model "Deep" water, but then simply ramp down waves in non-deep water with a linear multiplier.
    // http://hyperphysics.phy-astr.gsu.edu/hbase/Waves/watwav2.html
    // http://hyperphysics.phy-astr.gsu.edu/hbase/watwav.html#c1
    // optimisation - do this outside the loop below - take the median wavelength for depth weighting, intead of computing
    // per component. computing per component makes little difference to the end result
    float depth_wt = saturate(depth * _TwoPiOverWavelengths[_NumWaveVecs / 2].x / PI);
    float4 wt = _AttenuationInShallows * depth_wt + oneMinusAttenuation;

    // gerstner computation is vectorized - processes 4 wave components at once
    for (uint vi = 0; vi < _NumWaveVecs; vi++)
    {
        // direction
        float4 Dx = _WaveDirX[vi];
        float4 Dz = _WaveDirZ[vi];

        // Peferred wave direction
#if _DIRECT_TOWARDS_POINT
        wt *= max((1.0 + Dx * preferredDirX + Dz * preferredDirZ) / 2.0, 0.1);
#endif

        // wave number
        float4 k = _TwoPiOverWavelengths[vi];
        // spatial location
        float4 x = Dx * _input.worldPos.x + Dz * _input.worldPos.y;
        float4 angle = k * x + _Phases[vi];

        // dx and dz could be baked into _ChopAmps
        float4 disp = _ChopAmps[vi] * sin(angle);
        float4 resultx = disp * Dx;
        float4 resultz = disp * Dz;

        float4 resulty = _Amplitudes[vi] * cos(angle);

        // sum the vector results
        result.x += dot(resultx, wt);
        result.y += dot(resulty, wt);
        result.z += dot(resultz, wt);

        float4 sssFactor = min(1.0, _TwoPiOverWavelengths[vi]);
        displacementNormalized.x += dot(resultx * sssFactor, wt);
        displacementNormalized.y += dot(resultz * sssFactor, wt);
    }

    float sss = length(displacementNormalized);

    OutColor =  _Weight * float4(result, sss);
}