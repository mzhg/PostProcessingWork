#include "OceanLODData.glsl"

uniform float _Radius = 3.0;
uniform float _DampenStrength = 0.01f;
uniform float _Amplitude;
// TODO add this for all ocean inputs?
uniform float _Weight;
uniform float _SimCount;
uniform float _SimDeltaTime;

in float2 worldOffsetScaled;

layout(location = 0) out float4 OutColor;

#ifndef ADD_WAVE_PARTICLE
#define ADD_WAVE_PARTICLE 0
#endif

#ifndef ADD_WAVE_BUMP
#define ADD_WAVE_BUMP 0
#endif

void main()
{
    // power 4 smoothstep - no normalize needed
    // credit goes to stubbe's shadertoy: https://www.shadertoy.com/view/4ldSD2
    float r2 = dot(worldOffsetScaled.xy, worldOffsetScaled.xy);
    if (r2 > 1.0)
    {
        OutColor = float4(0);
        return;
    }
    r2 = 1.0 - r2;
    float val = r2 * r2;

#if ADD_WAVE_PARTICLE
    val *= _Amplitude;
    OutColor = float4(0.0, val * _Weight, 0.0, 0.0);
#elif ADD_WAVE_BUMP
    val = pow(val, 0.05);
    val *= _Amplitude;

    if (_SimCount > 0.0) // user friendly - avoid nans
        val /= _SimCount;

    // accelerate velocities
    OutColor =  float4(0.0, _SimDeltaTime * val, 0.0, 0.0);
#else
    float weight = val * _DampenStrength;
    OutColor = float4(0.0, 0.0, 0.0, weight);
#endif
}