#extension GL_NV_shader_buffer_load : enable
#extension GL_NV_bindless_texture : enable
#extension GL_NV_gpu_shader5 : enable

#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "ocean_shader_common.h"
#include "shader_common.fxh"
#include "atmospheric.glsl"

#define GFSDK_WAVEWORKS_SM5
#define GFSDK_WAVEWORKS_USE_TESSELLATION

#define GFSDK_WAVEWORKS_DECLARE_GEOM_VS_CONSTANT(Type,Label,Regoff) Type Label;
#define GFSDK_WAVEWORKS_BEGIN_GEOM_VS_CBUFFER(Label) cbuffer Label {
#define GFSDK_WAVEWORKS_END_GEOM_VS_CBUFFER };

#define GFSDK_WAVEWORKS_DECLARE_GEOM_HS_CONSTANT(Type,Label,Regoff) Type Label;
#define GFSDK_WAVEWORKS_BEGIN_GEOM_HS_CBUFFER(Label) cbuffer Label {
#define GFSDK_WAVEWORKS_END_GEOM_HS_CBUFFER };

#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Quadtree.glsl"

#define GFSDK_WAVEWORKS_DECLARE_ATTR_DS_SAMPLER(Label,TextureLabel,Regoff) sampler Label; texture2D TextureLabel;
#define GFSDK_WAVEWORKS_DECLARE_ATTR_DS_CONSTANT(Type,Label,Regoff) Type Label;
#define GFSDK_WAVEWORKS_BEGIN_ATTR_DS_CBUFFER(Label) cbuffer Label {
#define GFSDK_WAVEWORKS_END_ATTR_DS_CBUFFER };

#define GFSDK_WAVEWORKS_DECLARE_ATTR_PS_SAMPLER(Label,TextureLabel,Regoff) sampler Label; texture2D TextureLabel;
#define GFSDK_WAVEWORKS_DECLARE_ATTR_PS_CONSTANT(Type,Label,Regoff) Type Label;
#define GFSDK_WAVEWORKS_BEGIN_ATTR_PS_CBUFFER(Label) cbuffer Label {
#define GFSDK_WAVEWORKS_END_ATTR_PS_CBUFFER };

#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Attributes.glsl"

//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------

// Constant

uniform float		g_ZNear = 1.0;
uniform float		g_ZFar = 20000.0;


uniform float4x4	g_matViewProj;
uniform float4x4	g_matView;
uniform float4x4	g_matWorldToShip;

uniform float4      g_ViewRight;
uniform float4      g_ViewUp;
uniform float3      g_ViewForward;

uniform float4		g_GustUV;

uniform float4		g_ScreenSizeInv;
uniform float3		g_SkyColor;
uniform float4		g_DeepColor;
uniform float3		g_BendParam = {0.1f, -0.4f, 0.2f};

uniform float3		g_LightningPosition;
uniform float3		g_LightningColor;
uniform float4x4	g_matSceneToShadowMap;

uniform float3		g_LightDir;
uniform float3		g_LightColor;
uniform float3      g_WaterDeepColor={0.0,0.04,0.09};
uniform float3      g_WaterScatterColor={0.0,0.05,0.025};
uniform float		g_WaterSpecularIntensity = 0.4;
uniform float       g_WaterSpecularPower=100.0;
uniform float       g_WaterLightningSpecularPower=20.0;

uniform float		g_ShowSpraySim=0.0;
uniform float		g_ShowFoamSim=0.0;

uniform int			g_LightsNum;
uniform float4		g_SpotlightPosition[MaxNumSpotlights];
uniform float4		g_SpotLightAxisAndCosAngle[MaxNumSpotlights];
uniform float4		g_SpotlightColor[MaxNumSpotlights];

uniform float3		foam_underwater_color = {0.81f, 0.90f, 1.0f};

uniform float		g_GlobalFoamFade;

uniform float4		g_HullProfileCoordOffsetAndScale[MaxNumVessels];
uniform float4		g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[MaxNumVessels];

uniform float		g_CubeBlend;

uniform float2		g_SkyCube0RotateSinCos;
uniform float2		g_SkyCube1RotateSinCos;

uniform float3		g_SkyCubeMult;

uniform float		g_FogExponent;

uniform float       g_TimeStep = 0.1f;

uniform float		g_CloudFactor;

uniform bool		g_bGustsEnabled;
uniform bool		g_bWakeEnabled;

#if ENABLE_SHADOWS
uniform float4x4    g_SpotlightMatrix[MaxNumSpotlights];
uniform uint64_t  g_SpotlightResource[MaxNumSpotlights];
#endif

float3 rotateXY(float3 xyz, float2 sc)
{
    float3 result = xyz;
    float s = sc.x;
    float c = sc.y;
    result.x = xyz.x * c - xyz.y * s;
    result.y = xyz.x * s + xyz.y * c;
    return result;
}

#define FRESNEL_TERM_SUPERSAMPLES_RADIUS 1
#define FRESNEL_TERM_SUPERSAMPLES_INTERVALS (1 + 2*FRESNEL_TERM_SUPERSAMPLES_RADIUS)

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
layout(binding = 1) uniform sampler2D	g_texColorMap;
layout(binding = 2) uniform sampler2D	g_texBufferMap;
layout(binding = 3) uniform samplerCube	g_texCubeMap0;
layout(binding = 4) uniform samplerCube	g_texCubeMap1;
layout(binding = 5) uniform sampler2D	g_texFoamIntensityMap;
layout(binding = 6) uniform sampler2D	g_texFoamDiffuseMap;
layout(binding = 7) uniform sampler2D	g_texHullProfileMap[MaxNumVessels];

layout(binding = 8) uniform sampler2D	g_texWakeMap;
layout(binding = 9) uniform sampler2D	g_texShipFoamMap;
layout(binding = 10) uniform sampler2D	g_texGustMap;
layout(binding = 11) uniform sampler2D	g_texLocalFoamMap;

layout(binding = 12) uniform sampler2D	g_texReflection;
layout(binding = 13) uniform sampler2D	g_texReflectionPos;

struct SprayParticleData
{
    float4 position;
    float4 velocity;
};

//AppendStructuredBuffer<SprayParticleData> g_SprayParticleData : register(u1);
//StructuredBuffer<SprayParticleData>       g_SprayParticleDataSRV;

layout(binding = 0) buffer SprayParticleData0
{
    SprayParticleData g_SprayParticleData[];
};

layout(binding = 1) readonly buffer SprayParticleData1
{
    SprayParticleData g_SprayParticleDataSRV[];
};

layout (binding = 2, offset = 0) uniform atomic_uint g_SprayParticleCount;

// Blending map for ocean color
/*sampler g_samplerColorMap =
sampler_state
{
Filter = MIN_MAG_LINEAR_MIP_POINT;
AddressU = Clamp;
};

// Environment map
sampler g_samplerCubeMap =
sampler_state
{
Filter = MIN_MAG_MIP_LINEAR;
AddressU = Clamp;
AddressV = Clamp;
AddressW = Clamp;
};

// Standard trilinear sampler
sampler g_samplerTrilinear =
sampler_state
{
Filter = MIN_MAG_MIP_LINEAR;//ANISOTROPIC;
AddressU = Wrap;
AddressV = Wrap;
MaxAnisotropy = 1;
};

// Standard anisotropic sampler
sampler g_samplerAnisotropic =
sampler_state
{
Filter = ANISOTROPIC;
AddressU = Wrap;
AddressV = Wrap;
MaxAnisotropy = 16;
};

// Hull profile sampler
sampler g_samplerHullProfile = sampler_state
{
Filter = MIN_MAG_MIP_LINEAR;
AddressU = Clamp;
AddressV = Clamp;
};

sampler g_samplerHullProfileBorder
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Border;
    AddressV = Border;
    BorderColor = float4(0, 0, 0, 0);
};

sampler g_samplerTrilinearClamp
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Clamp;
    AddressV = Clamp;
};

sampler g_samplerBilinearClamp
{
    Filter = MIN_MAG_LINEAR_MIP_POINT;
    AddressU = Clamp;
    AddressV = Clamp;
};

sampler g_samplerPointClamp
{
    Filter = MIN_MAG_MIP_POINT;
    AddressU = Clamp;
    AddressV = Clamp;
};

struct VS_OUTPUT
{
    float4								worldspace_position	: VSO ;
    float								hull_proximity : HULL_PROX;
};

struct DS_OUTPUT
{
    precise float4								pos_clip	 : SV_Position;
    GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT NV_ocean_interp;
    float3								world_displacement: TEXCOORD4;
    float3								world_pos_undisplaced: TEXCOORD5;
    float3								world_pos: TEXCOORD6;
    float3								eye_pos: TEXCOORD7;
    float2								wake_uv: TEXCOORD8;
    float2								foam_uv: TEXCOORD9;
    float                               penetration : PENETRATION;
};

struct HS_ConstantOutput
{
// Tess factor for the FF HW block
    float fTessFactor[3]    : SV_TessFactor;
    float fInsideTessFactor : SV_InsideTessFactor;
    float fTargetEdgeLength[3] : TargetEdgeLength;
    float fHullProxMult[3] : HullProxMult;
};

struct Empty
{
};

struct InParticlePS
{
    float4 position : SV_Position;
    float4 color    : COLOR;
};

void ParticleVS()
{

}*/

const float2 ParticleOffsets[4] =float2[4]
(
float2(-1,  1),
float2(-1, -1),
float2( 1,  1),
float2( 1, -1)
);
