#include "OceanHelpers.glsl"

uniform sampler2D _MainTex;
uniform float4 _MainTex_ST;
uniform float _Alpha;

// MeshScaleLerp, FarNormalsWeight, LODIndex (debug), unused
uniform float4 _InstanceData;

out float2 uv;

layout(location = 0) in float4 In_Position;
layout(location = 1) in float2 In_UV;


void main()
{
    // move to world
    float3 worldPos;
    worldPos.xz = mul(unity_ObjectToWorld, In_Position).xz;
    worldPos.y = 0.0;

    // vertex snapping and lod transition
    float lodAlpha = ComputeLodAlpha(worldPos, _InstanceData.x);

    // sample shape textures - always lerp between 2 scales, so sample two textures

    // sample weights. params.z allows shape to be faded out (used on last lod to support pop-less scale transitions)
    float wt_smallerLod = (1.0 - lodAlpha) * _LD_Params[_LD_SliceIndex].z;
    float wt_biggerLod = (1.0 - wt_smallerLod) * _LD_Params[_LD_SliceIndex + 1].z;
    // sample displacement textures, add results to current world pos / normal / foam
    const float2 wxz = worldPos.xz;
    float foam = 0.0;
    float sss = 0.;
    SampleDisplacements(_LD_TexArray_AnimatedWaves, WorldToUV(wxz), wt_smallerLod, worldPos, sss);
    SampleDisplacements(_LD_TexArray_AnimatedWaves, WorldToUV_BiggerLod(wxz), wt_biggerLod, worldPos, sss);

    // move to sea level
    worldPos.y += _OceanCenterPosWorld.y;

    // view-projection
    gl_Position = mul(UNITY_MATRIX_VP, float4(worldPos, 1.0));
    uv = In_UV;
}