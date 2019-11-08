#include "OceanLODData.glsl"

// IMPORTANT - this mirrors the constant with the same name in ShapeGerstnerBatched.cs, both must be updated together!
#define BATCH_SIZE 32

#ifndef PI
#define PI 3.141593
#endif

out Varyings
{
    float2 worldPos;
    float3 uv_slice;
}o;

void main()
{
    /*gl_Position = float4(In_Position.xy, 0.0, 0.5);

    float2 worldXZ = UVToWorld(In_UV);
    o.worldPos = worldXZ;
    o.uv_slice = float3(In_UV, _LD_SliceIndex);*/

    int idx = gl_VertexID % 3;  // allows rendering multiple fullscreen triangles
    vec2 In_UV = vec2((idx << 1) & 2, idx & 2);
    gl_Position = vec4(In_UV * 2.0 - 1.0, 0, 1);

    float2 worldXZ = UVToWorld(In_UV);
    o.worldPos = worldXZ;
    o.uv_slice = float3(In_UV, _LD_SliceIndex);
}