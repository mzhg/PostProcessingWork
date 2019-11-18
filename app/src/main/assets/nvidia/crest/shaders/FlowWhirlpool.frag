#include "OceanLODData.glsl"

//in float2 m_uv;

out float2 OutColor;

uniform float _EyeRadiusProportion;
uniform float _MaxSpeed;

in float2 worldPos;

void main()
{
    float2 flow = float2(0,0);

//    float2 pointToCenter = (float2(0.5, 0.5) - m_uv) * 2.0;
//    float  distToCenter2 = dot(pointToCenter, pointToCenter);

    float3 centerPos = float3(unity_ObjectToWorld[3][0],unity_ObjectToWorld[3][1],unity_ObjectToWorld[3][2]);
    float2 pointToCenter = worldPos - centerPos.xz;
    float distToCenter2 = dot(pointToCenter, pointToCenter);

    float _Radius = 80.f;
    float R2 = _Radius * _Radius;
    distToCenter2 /= R2;

    if (distToCenter2 < 1.0 && distToCenter2 > _EyeRadiusProportion * _EyeRadiusProportion)
    {
        float distToCenter = sqrt(distToCenter2);

        float centerProp = 1.0 - (distToCenter - _EyeRadiusProportion) / (1.0 - _EyeRadiusProportion);
        pointToCenter /= distToCenter;

        // Whirlpool 'swirlyness', can vary from 0 - 1
        const float swirl = 0.6;

        // Dynamically calculate current value of velocity field
        flow = _MaxSpeed * centerProp * normalize(
        swirl * centerProp * float2(-pointToCenter.y, pointToCenter.x) +
        (swirl - 1.0) * (centerProp - 1.0) * pointToCenter);
    }

    OutColor = flow;
}