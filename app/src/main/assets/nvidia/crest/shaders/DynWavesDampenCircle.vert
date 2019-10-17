#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;

uniform float _Radius = 3.0;
uniform float _DampenStrength = 0.01f;

out float2 worldOffsetScaled;

void main()
{
    gl_Position = UnityObjectToClipPos(In_Position);

    float3 worldPos = mul(unity_ObjectToWorld, In_Position).xyz;
    float3 centerPos; // = unity_ObjectToWorld._m03_m13_m23;
    centerPos.x = unity_ObjectToWorld.m30;
    centerPos.y = unity_ObjectToWorld.m31;
    centerPos.z = unity_ObjectToWorld.m32;
    worldOffsetScaled.xy = worldPos.xz - centerPos.xz;

    // shape is symmetric around center with known radius - fix the vert positions to perfectly wrap the shape.
    worldOffsetScaled.xy = sign(worldOffsetScaled.xy);
    float4 newWorldPos = float4(centerPos, 1.0);
    newWorldPos.xz += worldOffsetScaled.xy * _Radius;
    gl_Position = mul(UNITY_MATRIX_VP, newWorldPos);
}