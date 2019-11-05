#include "OceanLODData.glsl"

uniform float _Radius;

out float2 worldOffsetScaledXZ;

layout(location = 0) in float3 positionOS;

void main()
{
//    float4 positionCS = UnityObjectToClipPos(positionOS);

    float4 worldPos = mul(unity_ObjectToWorld, float4(positionOS, 1.0));
    float3 centerPos = float3(unity_ObjectToWorld[3]);
    worldOffsetScaledXZ = worldPos.xz - centerPos.xz;

    // shape is symmetric around center with known radius - fix the vert positions to perfectly wrap the shape.
    worldOffsetScaledXZ = sign(worldOffsetScaledXZ);
    float4 newWorldPos = float4(centerPos, 1.);
    newWorldPos.xz += worldOffsetScaledXZ * _Radius;
    gl_Position = mul(UNITY_MATRIX_VP, newWorldPos);
}