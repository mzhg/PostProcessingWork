#include "OceanLODData.glsl"

out float2 m_uv;

layout(location = 0) in float3 positionOS;
layout(location = 1) in float2 uv;

void main()
{
    gl_Position = UnityObjectToClipPos(float4(positionOS, 1));
    m_uv.x = (positionOS.x + 1.0) * 0.5f;
    m_uv.y = (positionOS.z + 1.0) * 0.5f;

    m_uv = uv;

    float4 worldPos = unity_ObjectToWorld * float4(positionOS, 1);
    float3 centerPos = float3(unity_ObjectToWorld[3]);

//    pointToCenter = float2(worldPos.x - centerPos.x, worldPos.z - centerPos.z)/;

}