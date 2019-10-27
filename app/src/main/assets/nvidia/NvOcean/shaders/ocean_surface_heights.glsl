#include "ocean_shader_common.h"

//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------
uniform float g_numQuadsW;
uniform float g_numQuadsH;
uniform float4 g_quadScale;
uniform float2 g_quadUVDims;
uniform float4 g_srcUVToWorldScale;
uniform float4 g_srcUVToWorldRot;
uniform float4 g_srcUVToWorldOffset;
uniform float2 g_worldToClipScale;
uniform float2 g_clipToWorldRot;
uniform float2 g_clipToWorldOffset;

uniform float2 g_worldToUVScale;
uniform float2 g_worldToUVOffset;
uniform float2 g_worldToUVRot;
uniform float4x4 g_matViewProj;
uniform float4x4 g_matWorld;
//------------------------------------------------------------------------------------
// Water hooks
//------------------------------------------------------------------------------------
struct GFSDK_WAVEWORKS_VERTEX_INPUT
{
    float2 src_uv;
};

float2 rotate_2d(float2 v, float2 rot)
{
    return float2(v.x * rot.x + v.y * rot.y, v.x * -rot.y + v.y * rot.x);
}

float3 GFSDK_WaveWorks_GetUndisplacedVertexWorldPosition(float2 In)
{
    return float3(g_srcUVToWorldOffset.xy+rotate_2d(In*g_srcUVToWorldScale.xy,g_srcUVToWorldRot.xy),0.f);
}

#define GFSDK_WAVEWORKS_SM5
#define GFSDK_WAVEWORKS_USE_TESSELLATION

#define GFSDK_WAVEWORKS_DECLARE_ATTR_DS_SAMPLER(Label,TextureLabel,Regoff) sampler Label; texture2D TextureLabel;
#define GFSDK_WAVEWORKS_DECLARE_ATTR_DS_CONSTANT(Type,Label,Regoff) Type Label;
#define GFSDK_WAVEWORKS_BEGIN_ATTR_DS_CBUFFER(Label) cbuffer Label {
#define GFSDK_WAVEWORKS_END_ATTR_DS_CBUFFER };

#define GFSDK_WAVEWORKS_DECLARE_ATTR_PS_SAMPLER(Label,TextureLabel,Regoff) sampler Label; texture2D TextureLabel;
#define GFSDK_WAVEWORKS_DECLARE_ATTR_PS_CONSTANT(Type,Label,Regoff) Type Label;
#define GFSDK_WAVEWORKS_BEGIN_ATTR_PS_CBUFFER(Label) cbuffer Label {
#define GFSDK_WAVEWORKS_END_ATTR_PS_CBUFFER };

#include "GFSDK_WaveWorks_Attributes.fxh"

//------------------------------------------------------------------------------------
// Constants
//------------------------------------------------------------------------------------
 const float2 kQuadCornerUVs[4] = float2[4](
float2(0.f,0.f),
float2(0.f,1.f),
float2(1.f,0.f),
float2(1.f,1.f)
);

 const float3 kMarkerCoords[12] = float3[12](
float3( 0.f, 0.f, 0.f),
float3( 1.f, 1.f, 1.f),
float3( 1.f,-1.f, 1.f),
float3( 0.f, 0.f, 0.f),
float3( 1.f,-1.f, 1.f),
float3(-1.f,-1.f, 1.f),
float3( 0.f, 0.f, 0.f),
float3(-1.f,-1.f, 1.f),
float3(-1.f, 1.f, 1.f),
float3( 0.f, 0.f, 0.f),
float3(-1.f, 1.f, 1.f),
float3( 1.f, 1.f, 1.f)
);

const float kMarkerSeparation = 5.f;

layout(binding = 8) uniform sampler2D g_texDiffuse;