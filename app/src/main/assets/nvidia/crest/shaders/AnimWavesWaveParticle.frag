#include "OceanLODData.glsl"

uniform float _Radius;
uniform float _Amplitude;
// TODO add this for all ocean inputs?
uniform float _Weight;

in float2 worldOffsetScaledXZ;

out float4 OutColor;

void main()
{
    // power 4 smoothstep - no normalize needed
    // credit goes to stubbe's shadertoy: https://www.shadertoy.com/view/4ldSD2
    float r2 = dot( worldOffsetScaledXZ, worldOffsetScaledXZ);
    if( r2 > 1.0 ){
        OutColor = float4(0);
        return;
    }

    r2 = 1.0 - r2;
    float y = r2 * r2 * _Amplitude;
    OutColor =  float4(0.0, y * _Weight, 0.0, 0.0);
}