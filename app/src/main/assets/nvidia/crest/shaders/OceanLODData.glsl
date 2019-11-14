#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

// Crest Ocean System

// This file is subject to the MIT License as seen in the root of this folder structure (LICENSE)

// Ocean LOD data - data, samplers and functions associated with LODs

// NOTE: This must match the value in LodDataMgr.cs, as it is used to allow the
// C# code to check if any parameters are within the MAX_LOD_COUNT limits
#define MAX_LOD_COUNT 15


// NOTE: these MUST match the values in PropertyWrapper.cs
#define THREAD_GROUP_SIZE_X 8
#define THREAD_GROUP_SIZE_Y 8

// 'Current' target/source slice index
uniform int _LD_SliceIndex;
#define Texture2DArray sampler2DArray

// Samplers and data associated with a LOD.
// _LD_Params: float4(world texel size, texture resolution, shape weight multiplier, 1 / texture resolution)
layout(binding = 0) uniform sampler2DArray _LD_TexArray_AnimatedWaves;
layout(binding = 1) uniform sampler2DArray _LD_TexArray_WaveBuffer;
layout(binding = 2) uniform sampler2DArray _LD_TexArray_SeaFloorDepth;
layout(binding = 3) uniform sampler2DArray _LD_TexArray_Foam;
layout(binding = 4) uniform sampler2DArray _LD_TexArray_Flow;
layout(binding = 5) uniform sampler2DArray _LD_TexArray_DynamicWaves;
layout(binding = 6) uniform sampler2DArray _LD_TexArray_Shadow;
// _LD_Params: float4(world texel size, texture resolution, shape weight multiplier, 1 / texture resolution)
uniform float4 _LD_Params[MAX_LOD_COUNT + 1];
uniform float4 _LD_Pos_Scale[MAX_LOD_COUNT + 1];

// These are used in lods where we operate on data from
// previously calculated lods. Used in simulations and
// shadowing for example.
layout(binding = 7) uniform sampler2DArray _LD_TexArray_AnimatedWaves_Source;
layout(binding = 8) uniform sampler2DArray _LD_TexArray_WaveBuffer_Source;
layout(binding = 9) uniform sampler2DArray _LD_TexArray_SeaFloorDepth_Source;
layout(binding = 10) uniform sampler2DArray _LD_TexArray_Foam_Source;
layout(binding = 11) uniform sampler2DArray _LD_TexArray_Flow_Source;
layout(binding = 12) uniform sampler2DArray _LD_TexArray_DynamicWaves_Source;
layout(binding = 13) uniform sampler2DArray _LD_TexArray_Shadow_Source;
uniform float4 _LD_Params_Source[MAX_LOD_COUNT + 1];
uniform float4 _LD_Pos_Scale_Source[MAX_LOD_COUNT + 1];

uniform mat4 UNITY_MATRIX_VP;
uniform mat4 unity_ObjectToWorld;

vec4 UnityObjectToClipPos(vec4 worldPos)
{
    return UNITY_MATRIX_VP * unity_ObjectToWorld * worldPos;
}

//SamplerState LODData_linear_clamp_sampler;
//SamplerState LODData_point_clamp_sampler;

// Bias ocean floor depth so that default (0) values in texture are not interpreted as shallow and generating foam everywhere
#define CREST_OCEAN_DEPTH_BASELINE 1000.0

// Conversions for world space from/to UV space. All these should *not* be clamped otherwise they'll break fullscreen triangles.
float2 LD_WorldToUV(in float2 i_samplePos, in float2 i_centerPos, in float i_res, in float i_texelSize)
{
    return (i_samplePos - i_centerPos) / (i_texelSize * i_res) + 0.5;
}

float3 WorldToUV(in float2 i_samplePos, in int i_sliceIndex) {
    const float2 result = LD_WorldToUV(
    i_samplePos,
    _LD_Pos_Scale[i_sliceIndex].xy,
    _LD_Params[i_sliceIndex].y,
    _LD_Params[i_sliceIndex].x
    );
    return float3(result, i_sliceIndex);
}

float3 WorldToUV_BiggerLod(in float2 i_samplePos, in int i_sliceIndex_BiggerLod) {
    const float2 result = LD_WorldToUV(
    i_samplePos, _LD_Pos_Scale[i_sliceIndex_BiggerLod].xy,
    _LD_Params[i_sliceIndex_BiggerLod].y,
    _LD_Params[i_sliceIndex_BiggerLod].x
    );
    return float3(result, i_sliceIndex_BiggerLod);
}

float3 WorldToUV_Source(in float2 i_samplePos, in int i_sliceIndex_Source) {
    const float2 result = LD_WorldToUV(
    i_samplePos,
    _LD_Pos_Scale_Source[i_sliceIndex_Source].xy,
    _LD_Params_Source[i_sliceIndex_Source].y,
    _LD_Params_Source[i_sliceIndex_Source].x
    );
    return float3(result, i_sliceIndex_Source);
}

float2 LD_UVToWorld(in float2 i_uv, in float2 i_centerPos, in float i_res, in float i_texelSize)
{
    return i_texelSize * i_res * (i_uv - 0.5) + i_centerPos;
}

float2 UVToWorld(in float2 i_uv, in int i_sliceIndex) { return LD_UVToWorld(i_uv, _LD_Pos_Scale[i_sliceIndex].xy, _LD_Params[i_sliceIndex].y, _LD_Params[i_sliceIndex].x); }

// Shortcuts if _LD_SliceIndex is set
float3 WorldToUV(in float2 i_samplePos) { return WorldToUV(i_samplePos, _LD_SliceIndex); }
float3 WorldToUV_BiggerLod(in float2 i_samplePos) { return WorldToUV_BiggerLod(i_samplePos, _LD_SliceIndex + 1); }
float2 UVToWorld(in float2 i_uv) { return UVToWorld(i_uv, _LD_SliceIndex); }

// Convert compute shader id to uv texture coordinates
float2 IDtoUV(in float2 i_id, in float i_width, in float i_height)
{
    return (i_id + 0.5) / float2(i_width, i_height);
}


// Sampling functions
void SampleDisplacements(Texture2DArray i_dispSampler, in float3 i_uv_slice, in float i_wt, inout float3 io_worldPos, inout float io_sss)
{
    const half4 data = textureLod(i_dispSampler, i_uv_slice, 0.0);   //LODData_linear_clamp_sampler
    io_worldPos += i_wt * data.xyz;
    io_sss += i_wt * data.a;
}

void SampleDisplacementsNormals(Texture2DArray i_dispSampler, in float3 i_uv_slice, in float i_wt, in float i_invRes, in float i_texelSize, inout float3 io_worldPos, inout half2 io_nxz, inout float io_sss)
{
    const float4 data = textureLod(i_dispSampler, i_uv_slice, 0.0);   // LODData_linear_clamp_sampler
    io_sss += i_wt * data.a;
    const float3 disp = data.xyz;
    io_worldPos += i_wt * disp;

    float3 n;
    {
        float3 dd = float3(i_invRes, 0.0, i_texelSize);
        half3 disp_x = dd.zyy + textureLod(i_dispSampler, i_uv_slice + float3(dd.xy, 0.0), dd.y).xyz;  // LODData_linear_clamp_sampler
        half3 disp_z = dd.yyz + textureLod(i_dispSampler, i_uv_slice + float3(dd.yx, 0.0), dd.y).xyz;
        n = normalize(cross(disp_z - disp, disp_x - disp));
    }
    io_nxz += i_wt * n.xz;
}

void SampleFoam(in Texture2DArray i_oceanFoamSampler, in float3 i_uv_slice, in float i_wt, inout float io_foam)
{
    io_foam += i_wt * textureLod(i_oceanFoamSampler, i_uv_slice, 0.0).x;   // LODData_linear_clamp_sampler
}

void SampleFlow(in Texture2DArray i_oceanFlowSampler, in float3 i_uv_slice, in float i_wt, inout float2 io_flow)
{
    io_flow += i_wt * textureLod(i_oceanFlowSampler, i_uv_slice, 0.0).xy;  // LODData_linear_clamp_sampler
}

void SampleSeaDepth(in Texture2DArray i_oceanDepthSampler, in float3 i_uv_slice, in float i_wt, inout float io_oceanDepth)
{
    io_oceanDepth += i_wt * (textureLod(i_oceanDepthSampler, i_uv_slice, 0.0).x - CREST_OCEAN_DEPTH_BASELINE);  // LODData_linear_clamp_sampler
}

void SampleShadow(in Texture2DArray i_oceanShadowSampler, in float3 i_uv_slice, in float i_wt, inout half2 io_shadow)
{
    io_shadow += i_wt * textureLod(i_oceanShadowSampler, i_uv_slice, 0.0).xy;
}

    #define SampleLod(i_lodTextureArray, i_uv_slice) textureLod(i_lodTextureArray, i_uv_slice, 0.0)
    #define SampleLodLevel(i_lodTextureArray, i_uv_slice, mips) textureLod(i_lodTextureArray, i_uv_slice, mips)

// Geometry data
// x: Grid size of lod data - size of lod data texel in world space.
// y: Grid size of geometry - distance between verts in mesh.
// zw: normalScrollSpeed0, normalScrollSpeed1
uniform float4 _GeomData;
uniform float3 _OceanCenterPosWorld;

uniform float4 ScreenParams;  // xy: Screen Size; zw: Invert screen size.

//float4 pos, the input of ComputeScreenPos(), is [-w,w]
//usually we input the result of MVP transform directly, just like the reply above
float4 ComputeScreenPos (float4 pos) {

    const float2 _ProjectionParams = float2(1,1);
    float4 o = pos * 0.5f; //now o.xy is [-0.5w,0.5w], and o.w is half of pos.w also

    //UNITY_HALF_TEXEL_OFFSET is only for DirectX9, which is quite old in 2016, still Unity
    //will support it
    #if defined(UNITY_HALF_TEXEL_OFFSET)
    o.xy = float2(o.x, o.y*_ProjectionParams.x) + o.w * _ScreenParams.zw;
    #else

    o.xy = float2(o.x, o.y*_ProjectionParams.x) + o.w;
    //now result o.xy is [-0.5w + 0.5w,0.5w + 0.5w] = [0,w]
    //opengl & directx have different conventions of clip space y(start from top/start from bottom)
    //o.y*_ProjectionParams.x will make it behave the same in different platform
    //otherwise you will see the sampled texture flipped upsidedown
    #endif
    o.zw = pos.zw; //must keep the w, for tex2Dproj() to use
    return o;
}

float4 ComputeGrabScreenPos(float4 clipPos)
{
    return ComputeScreenPos(clipPos);
}

uniform float3 _WorldSpaceCameraPos;
uniform float4 _WorldSpaceLightPos0; // Directional lights: (world space direction, 0). Other lights: (world space position, 1).
uniform samplerCube unity_SpecCube0;
uniform float2 _CameraRange;  // x: Near; y: Far

float LinearEyeDepth(float dDepth)
{
    float mZFar =_CameraRange.y;
    float mZNear = _CameraRange.x;
    float fCamSpaceZ = mZFar*mZNear/(mZFar-dDepth*(mZFar-mZNear));

    return fCamSpaceZ;
}
