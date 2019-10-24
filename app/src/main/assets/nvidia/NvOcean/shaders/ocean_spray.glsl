#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#include "ocean_shader_common.h"
#include "ocean_spray_common.h"
#include "atmospheric.glsl"
#include "shader_common.fxh"
#include "ocean_psm.glsl"

//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------

uniform float4x4	g_matProj;
uniform float4x4	g_matView;
uniform float4x4	g_matProjInv;
uniform float3		g_LightDirection;
uniform float3		g_LightColor;
uniform float3		g_AmbientColor;
uniform float		g_FogExponent;
uniform float		g_InvParticleLifeTime;

uniform float3		g_LightningPosition;
uniform float3		g_LightningColor;
uniform float		g_SimpleParticles;

uniform int			g_LightsNum;
uniform float4		g_SpotlightPosition[MaxNumSpotlights];
uniform float4		g_SpotLightAxisAndCosAngle[MaxNumSpotlights];
uniform float4		g_SpotlightColor[MaxNumSpotlights];

#ifndef ENABLE_SHADOWS
#define ENABLE_SHADOWS 0
#endif

#if ENABLE_SHADOWS
uniform float4x4    g_SpotlightMatrix[MaxNumSpotlights];
uniform Texture2D   g_SpotlightResource[MaxNumSpotlights];
#endif

//Buffer<float4> g_RenderInstanceData;
//Buffer<float4> g_RenderOrientationAndDecimationData;
//Buffer<float4> g_RenderVelocityAndTimeData;

layout(binding = 0) uniform samplerBuffer g_RenderInstanceData;
layout(binding = 1) uniform samplerBuffer g_RenderOrientationAndDecimationData;
layout(binding = 2) uniform samplerBuffer g_RenderVelocityAndTimeData;

uniform float  g_PSMOpacityMultiplier;

// Data for GPU simulation
struct SprayParticleData
{
    float4 position_and_mass;
    float4 velocity_and_time;
};

uniform int			g_ParticlesNum;
uniform float		g_SimulationTime;
uniform float3		g_WindSpeed;

// We use these for ensuring particles do not intersect ship
uniform float4x4	g_worldToVessel;
uniform float4x4	g_vesselToWorld;

// We use these to kill particles
uniform float2		g_worldToHeightLookupScale;
uniform float2		g_worldToHeightLookupOffset;
uniform float2		g_worldToHeightLookupRot;
uniform Texture2D   g_texHeightLookup;

// We use these to feed particles back into foam map
uniform float4x4	g_matWorldToFoam;

 const float kVesselLength = 63.f;
 const float kVesselWidth = 9.f;
 const float kVesselDeckHeight = 0.f;
 const float kMaximumCollisionAcceleration = 10.f;
 const float kCollisionAccelerationRange = 0.5f;

 const float kSceneParticleTessFactor = 8.f;

struct DepthSortEntry {
    int ParticleIndex;
    float ViewZ;
};

//StructuredBuffer <DepthSortEntry> g_ParticleDepthSortSRV;
//StructuredBuffer<SprayParticleData> g_SprayParticleDataSRV;

//RWStructuredBuffer <DepthSortEntry> g_ParticleDepthSortUAV			: register(u0);
//AppendStructuredBuffer<SprayParticleData> g_SprayParticleData		: register(u1);

layout(binding = 0) readonly buffer  ParticleDepthSortSRV
{
    DepthSortEntry[] g_ParticleDepthSortSRV;
};

layout(binding = 1) readonly buffer  SprayParticleDataSRV
{
    SprayParticleData[] g_SprayParticleDataSRV;
};

layout(binding = 2)  buffer  ParticleDepthSortUAV
{
    DepthSortEntry[] g_ParticleDepthSortUAV;
};

layout(binding = 3)  buffer SprayParticleData
{
    SprayParticleData[] g_SprayParticleData;
};

layout(binding = 4) uniform atomic_uint g_SprayParticleCount;

uniform uint g_iDepthSortLevel;
uniform uint g_iDepthSortLevelMask;
uniform uint g_iDepthSortWidth;
uniform uint g_iDepthSortHeight;

uniform float4 g_AudioVisualizationRect; // litterbug
uniform float2 g_AudioVisualizationMargin;
uniform float g_AudioVisualizationLevel;

//------------------------------------------------------------------------------------
// Constants
//------------------------------------------------------------------------------------
 const float2 kParticleCornerCoords[4] = float2[4](
 float2(-1, 1),
float2( 1, 1),
float2(-1,-1),
float2( 1,-1)
);

 const float3 kFoamColor = float3(0.5f, 0.5f, 0.5f);

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
layout(binding = 3) uniform sampler2D	g_texSplash;

sampler g_SamplerTrilinearClamp
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Clamp;
    AddressV = Clamp;
};

//--------------------------------------------------------------------------------------
// Structs
//--------------------------------------------------------------------------------------


struct PARTICLE_INSTANCE_DATA {
    float4 position_and_mass			/*: PosMass*/;
    float3 orientation_and_decimation	/*: OriDec;*/
    float3 velocity						/*: Vel*/;
    float time							/*: T*/;
};

/*struct VS_SCENE_PARTICLE_OUTPUT {
    PARTICLE_INSTANCE_DATA InstanceData;
    float FogFactor               *//*: FogFactor*//*;
};*/

struct HS_PARTICLE_COORDS {
    float3 ViewPos;
    float3 TextureUVAndOpacity;
};

/*struct HS_SCENE_PARTICLE_OUTPUT {
    float3 ViewPos                : ViewPos;
    float3 TextureUVAndOpacity    : TEXCOORD0;
// NOT USED float3 PSMCoords              : PSMCoords;
    float FogFactor               : FogFactor;
};

struct DS_SCENE_PARTICLE_OUTPUT {
    float4 Position               : SV_Position;
    float3 ViewPos                : ViewPos;
    float3 TextureUVAndOpacity    : TEXCOORD0;
// NOT USED float3 PSMCoords              : PSMCoords;
    float FogFactor               : FogFactor;
    float3 Lighting               : LIGHTING;
};

struct HS_SCENE_PARTICLE_OUTPUT_CONST
{
    float fTessFactor[4]       : SV_TessFactor;
    float fInsideTessFactor[2] : SV_InsideTessFactor;
};

struct GS_FOAM_PARTICLE_OUTPUT {
    float4 Position               : SV_Position;
    float3 ViewPos                : ViewPos;
    float3 TextureUVAndOpacity    : TEXCOORD0;
    float  FoamAmount			  : FOAMAMOUNT;
};

struct GS_PSM_PARTICLE_OUTPUT
{
    float4 Position                      : SV_Position;
    nointerpolation uint LayerIndex      : SV_RenderTargetArrayIndex;
    float3 TextureUVAndOpacity           : TEXCOORD0;
    nointerpolation uint SubLayer        : TEXCOORD1;
};*/

#ifndef ENABLE_GPU_SIMULATION
#error "Missing ENABLE_GPU_SIMULATION defined"
#endif

#ifndef SPRAY_PARTICLE_SORTING
#error "Missing SPRAY_PARTICLE_SORTING defined"
#endif


//--------------------------------------------------------------------------------------
// Functions
//--------------------------------------------------------------------------------------
PARTICLE_INSTANCE_DATA GetParticleInstanceData(in uint PrimID)
{
    uint particle_index = PrimID; // one day? - g_ParticleDepthSortSRV[PrimID].ParticleIndex;

    PARTICLE_INSTANCE_DATA result;
    #if ENABLE_GPU_SIMULATION
    #if SPRAY_PARTICLE_SORTING
    particle_index = g_ParticleDepthSortSRV[particle_index].ParticleIndex;
    #endif
    SprayParticleData particleData = g_SprayParticleDataSRV[particle_index];
    result.position_and_mass = particleData.position_and_mass;
    result.orientation_and_decimation.xy = float2(1.0, 0.0);
    result.velocity = particleData.velocity_and_time.xyz;
    result.time = particleData.velocity_and_time.w;

    result.orientation_and_decimation.z = 1.f;
    #else
    result.position_and_mass = g_RenderInstanceData.Load(particle_index);
    result.orientation_and_decimation = g_RenderOrientationAndDecimationData.Load(particle_index).xyz;
    result.velocity = float3(0);
    #endif

    return result;
}

float CalcVelocityScale(float speed)
{
    return log2(speed * 0.2f + 2.0f);
}

float CalcTimeScale(float time)
{
    return 0.5+0.5*time;
}

void CalcParticleCoords( PARTICLE_INSTANCE_DATA InstanceData, in float2 CornerCoord, out float3 ViewPos, out float2 UV, out float Opacity)
{
    // Transform to camera space
    ViewPos = mul(float4(InstanceData.position_and_mass.xyz,1), g_matView).xyz;

    float2 coords = CornerCoord*CalcTimeScale(InstanceData.time);
    coords *= 0.7f; // Make particles a little smaller to keep the look crisp

    float3 velocityView = mul(InstanceData.velocity.xyz, float3x3(g_matView)).xyz;
    float velocityScale = CalcVelocityScale(length(velocityView.xy));
    coords.x /= (velocityScale * 0.25f + 0.75f);
    coords.y *= velocityScale;

    float angle = atan2(velocityView.x,velocityView.y);

    float2 orientation = float2(cos(angle), sin(angle));//InstanceData.orientation_and_decimation.xy;
    float2 rotatedCornerCoord;
    rotatedCornerCoord.x =  coords.x * orientation.x + coords.y * orientation.y;
    rotatedCornerCoord.y = -coords.x * orientation.y + coords.y * orientation.x;

    // Inflate corners, applying scale from instance data
    ViewPos.xy += g_SimpleParticles>0?CornerCoord*0.02:rotatedCornerCoord;

    const float mass = InstanceData.position_and_mass.w;
    float cosAngle = cos(mass * 8.0f);
    float sinAngle = sin(mass * 8.0f);

    float2 rotatedUV = CornerCoord;
    UV.x = rotatedUV.x * cosAngle + rotatedUV.y * sinAngle;
    UV.y =-rotatedUV.x * sinAngle + rotatedUV.y * cosAngle;

    UV = 0.5f * (UV + 1.f);

    Opacity = InstanceData.orientation_and_decimation.z;
}

HS_PARTICLE_COORDS CalcParticleCoords(in PARTICLE_INSTANCE_DATA InstanceData, int i)
{
    HS_PARTICLE_COORDS result;
    CalcParticleCoords( InstanceData, kParticleCornerCoords[i], result.ViewPos, result.TextureUVAndOpacity.xy, result.TextureUVAndOpacity.z);
    return result;
}

float4 GetParticleRGBA(SamplerState s, float2 uv, float alphaMult)
{
    float4 splash = g_texSplash.SampleBias(s, uv, -1.0);
    float alpha_threshold = 1.0 - alphaMult * 0.6;
    clip(splash.r-alpha_threshold);
    return float4(kFoamColor, 1.0);
}
