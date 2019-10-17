#include "OceanLODData.glsl"
layout(location = 0) out float4 OutColor;

uniform float _FactorParallel;
uniform float _FactorOrthogonal;
uniform float3 _Velocity;
uniform float _SimDeltaTime;
uniform float _Strength;
uniform float _Weight;

in Varyings
{
//    float4 positionCS; SV_POSITION;
    float3 normal ;// NORMAL;
    float4 col ;// COLOR;
    float offsetDist ;// TEXCOORD0;
}_input;

void main()
{
    float4 col = float4(0);
    col.x = _Strength * (length(_input.offsetDist)) * abs(_input.normal.y) * sqrt(length(_Velocity)) / 10.;

    if (dot(_input.normal, _Velocity) < -0.1)
    {
        col.x *= -.5;
    }

    // Accelerated velocities
    OutColor = _Weight * float4(0., col.x*_SimDeltaTime, 0., 0.);
}