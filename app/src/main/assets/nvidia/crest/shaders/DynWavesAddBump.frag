#include "OceanLODData.glsl"

uniform float _Radius;
uniform float _Amplitude;
uniform float _SimCount = 0;
uniform float _SimDeltaTime = 0;

in float2 worldOffsetScaledXZ;

out float4 OutColor;

void main()
{
    // power 4 smoothstep - no normalize needed
    // credit goes to stubbe's shadertoy: https://www.shadertoy.com/view/4ldSD2
    float r2 = dot(worldOffsetScaledXZ, worldOffsetScaledXZ);
    if (r2 > 1.0){
        OutColor = float4(0);
        return;
    }

    r2 = 1.0 - r2;

    float y = r2 * r2;
    y = pow(y, 0.05);
    y *= _Amplitude;

    if (_SimCount > 0.0) // user friendly - avoid nans
        y /= _SimCount;

    // accelerate velocities
    OutColor = float4(0.0, _SimDeltaTime * y, 0.0, 0.0);
}