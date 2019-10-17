#include "OceanLODData.glsl"

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;

// IMPORTANT - this mirrors the constant with the same name in ShapeGerstnerBatched.cs, both must be updated together!
#define BATCH_SIZE 32

#ifndef PI
#define PI 3.141593
#endif

uniform float _Weight;
uniform float _AttenuationInShallows;
uniform uint _NumWaveVecs;

uniform float4 _TwoPiOverWavelengths[BATCH_SIZE / 4];
uniform float4 _Amplitudes[BATCH_SIZE / 4];
uniform float4 _WaveDirX[BATCH_SIZE / 4];
uniform float4 _WaveDirZ[BATCH_SIZE / 4];
uniform float4 _Phases[BATCH_SIZE / 4];
uniform float4 _ChopAmps[BATCH_SIZE / 4];

uniform float4 _TargetPointData;

out Varyings
{
    float2 worldPos;
    float3 uv_slice;
}o;

void main()
{
    gl_Position = float4(In_Position.xy, 0.0, 0.5);

    float2 worldXZ = UVToWorld(In_UV);
    o.worldPos = worldXZ;
    o.uv_slice = float3(In_UV, _LD_SliceIndex);
}