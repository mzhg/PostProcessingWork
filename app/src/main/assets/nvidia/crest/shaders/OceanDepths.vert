#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;

out float depth;

void main()
{
    gl_Position = UnityObjectToClipPos(In_Position);

    float altitude = mul(unity_ObjectToWorld, In_Position).y;

    depth = _OceanCenterPosWorld.y - altitude;
}