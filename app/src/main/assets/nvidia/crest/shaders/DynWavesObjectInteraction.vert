#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 2) in float3 In_Normal;

uniform float _FactorParallel;
uniform float _FactorOrthogonal;
uniform float3 _Velocity;
uniform float _SimDeltaTime;
uniform float _Strength;
uniform float _Weight;

out Varyings
{
//    float4 positionCS; SV_POSITION;
    float3 normal ;// NORMAL;
    float4 col ;// COLOR;
    float offsetDist ;// TEXCOORD0;
}o;

void main()
{
    float3 vertexWorldPos = mul(unity_ObjectToWorld, In_Position);

    o.normal = normalize(mul(unity_ObjectToWorld, float4(In_Normal, 0.)).xyz);

    float3 vel = _Velocity / 30.;

    float velMag = max(length(vel), 0.001);
    float3 velN = vel / velMag;
    float angleFactor = dot(velN, o.normal);

    if (angleFactor < 0.)
    {
        // this helps for when velocity exactly perpendicular to some faces
        if (angleFactor < -0.0001)
        {
            vel = -vel;
        }

        angleFactor *= -1.;
    }

    float3 offset = o.normal * _FactorOrthogonal * pow(saturate(1. - angleFactor), .2) * velMag;
    offset += vel * _FactorParallel * pow(angleFactor, .5);
    o.offsetDist = length(offset);
    vertexWorldPos += offset;

    gl_Position = mul(UNITY_MATRIX_VP, float4(vertexWorldPos, 1.));

    o.col = 1.0;
}