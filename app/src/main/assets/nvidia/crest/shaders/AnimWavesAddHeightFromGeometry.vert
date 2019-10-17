#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;

out float3 worldPos;

void main()
{
    gl_Position = UnityObjectToClipPos(In_Position);
    worldPos = mul(unity_ObjectToWorld, In_Position).xyz;
}