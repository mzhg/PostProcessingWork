#include "OceanLODData.glsl"


out float2 worldPos;

//layout(location = 0) in float3 positionOS;

void main()
{
    float _Radius = 3;
//    float4 positionCS = UnityObjectToClipPos(positionOS);

    int idx = gl_VertexID % 3;  // allows rendering multiple fullscreen triangles
    vec2 In_UV = vec2((idx << 1) & 2, idx & 2);
    gl_Position = vec4(In_UV * 2.0 - 1.0, 0, 1);

    worldPos = UVToWorld(In_UV);
/*

//    float4 worldPos = mul(unity_ObjectToWorld, float4(positionOS, 1.0));
    float3 centerPos = float3(unity_ObjectToWorld[3][0],unity_ObjectToWorld[3][1],unity_ObjectToWorld[3][2]);
    worldOffsetScaledXZ = worldPos - centerPos.xz;
    m_LengthToCenter = length(worldOffsetScaledXZ);

    // shape is symmetric around center with known radius - fix the vert positions to perfectly wrap the shape.
    worldOffsetScaledXZ = sign(worldOffsetScaledXZ);
*/
//    float4 newWorldPos = float4(centerPos, 1.);
//    newWorldPos.xz += worldOffsetScaledXZ * _Radius;
//    gl_Position = mul(newWorldPos, UNITY_MATRIX_VP);
}