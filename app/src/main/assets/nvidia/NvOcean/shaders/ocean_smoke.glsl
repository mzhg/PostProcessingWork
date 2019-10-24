#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#include "ocean_shader_common.h"
#include "ocean_psm.glsl"
#include "inoise.glsl"

//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------

uniform float4x4	g_matProj;
uniform float4x4	g_matView;
uniform float3		g_LightDirection;
uniform float3		g_LightColor;
uniform float3		g_AmbientColor;
uniform float		g_FogExponent;
uniform float2		g_ParticleBeginEndScale;
uniform float		g_InvParticleLifeTime;
uniform float		g_NoiseTime;
//Buffer<float4> g_RenderInstanceData;

layout(binding = 0) uniform samplerBuffer g_RenderInstanceData;

uniform float3		g_LightningPosition;
uniform float3		g_LightningColor;

uniform uint g_ParticleIndexOffset;
uniform uint g_ParticleCount;
uniform float g_TimeStep;
uniform float g_PreRollEndTime;

//RWBuffer<float> g_SimulationInstanceData;
//RWBuffer<float> g_SimulationVelocities;

layout(binding = 0) buffer SimulationInstanceData
{
    float g_SimulationInstanceData[];
};

layout(binding = 1) buffer SimulationVelocities
{
    float g_SimulationVelocities[];
};

uniform float4x3 g_CurrEmitterMatrix;
uniform float4x3 g_PrevEmitterMatrix;
uniform float2 g_EmitAreaScale;
uniform float3 g_EmitMinMaxVelocityAndSpread;
uniform float2 g_EmitInterpScaleAndOffset;
uniform float4 g_WindVectorAndNoiseMult;
uniform float3 g_BuoyancyParams;
uniform float g_WindDrag;

uniform float g_NoiseSpatialScale;
uniform float g_NoiseTimeScale;

//Buffer<float2> g_RandomUV;
layout(binding = 1) uniform samplerBuffer g_RenderInstanceData;
uniform uint g_RandomOffset;

uniform float  g_PSMOpacityMultiplier;
uniform float  g_PSMFadeMargin;

struct DepthSortEntry {
    int ParticleIndex;
    float ViewZ;
};

//RWStructuredBuffer <DepthSortEntry> g_ParticleDepthSortUAV;
//StructuredBuffer <DepthSortEntry> g_ParticleDepthSortSRV;

layout(binding = 2) buffer ParticleDepthSortUAV
{
    DepthSortEntry g_ParticleDepthSortUAV[];
};

layout(binding = 3) readonly buffer ParticleDepthSortSRV
{
    DepthSortEntry g_ParticleDepthSortSRV[];
};

uniform uint g_iDepthSortLevel;
uniform uint g_iDepthSortLevelMask;
uniform uint g_iDepthSortWidth;
uniform uint g_iDepthSortHeight;

//------------------------------------------------------------------------------------
// Constants
//------------------------------------------------------------------------------------
const float2 kParticleCornerCoords[4] = float2[4](
float2(-1, 1),
float2( 1, 1),
float2(-1,-1),
float2( 1,-1)
);

#ifndef PI
const float PI = 3.141592654f;
#endif

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
texture2D g_texDiffuse;
/*sampler g_samplerDiffuse = sampler_state
{
Filter = MIN_MAG_MIP_LINEAR;
AddressU = Clamp;
AddressV = Clamp;
};

//--------------------------------------------------------------------------------------
// DepthStates
//--------------------------------------------------------------------------------------
DepthStencilState ReadOnlyDepth
{
    DepthEnable = TRUE;
    DepthWriteMask = ZERO;
    DepthFunc = LESS_EQUAL;
    StencilEnable = FALSE;
};

DepthStencilState NoDepthStencil
{
    DepthEnable = FALSE;
    StencilEnable = FALSE;
};

//--------------------------------------------------------------------------------------
// RasterStates
//--------------------------------------------------------------------------------------
RasterizerState SolidNoCull
{
    FillMode = SOLID;
    CullMode = NONE;

    MultisampleEnable = True;
};

//--------------------------------------------------------------------------------------
// BlendStates
//--------------------------------------------------------------------------------------
BlendState TranslucentBlendRGB
{
    BlendEnable[0] = TRUE;
    RenderTargetWriteMask[0] = 0xF;

    SrcBlend = SRC_ALPHA;
    DestBlend = INV_SRC_ALPHA;
    BlendOp = Add;

    SrcBlendAlpha = ZERO;
    DestBlendAlpha = INV_SRC_ALPHA;
    BlendOpAlpha = Add;
};

BlendState Opaque
{
    BlendEnable[0] = FALSE;
    RenderTargetWriteMask[0] = 0xF;
};

//--------------------------------------------------------------------------------------
// Structs
//--------------------------------------------------------------------------------------
struct VS_DUMMY_PARTICLE_OUTPUT
{
};

struct GS_SCENE_PARTICLE_OUTPUT
{
    float4 Position               : SV_Position;
    float3 TextureUVAndOpacity    : TEXCOORD0;
    float3 PSMCoords              : PSMCoords;
    float  FogFactor              : FogFactor;
};

struct GS_PSM_PARTICLE_OUTPUT
{
    float4 Position                      : SV_Position;
    nointerpolation uint LayerIndex      : SV_RenderTargetArrayIndex;
    float3 TextureUVAndOpacity           : TEXCOORD0;
    nointerpolation uint SubLayer        : TEXCOORD1;
};
*/
struct GS_PARTICLE_COORDS {
    float3 ViewPos;
    float3 TextureUVAndOpacity;
};

//--------------------------------------------------------------------------------------
// Functions
//--------------------------------------------------------------------------------------
float4 GetParticleInstanceData(in uint PrimID)
{
    uint particle_index = g_ParticleDepthSortSRV[PrimID].ParticleIndex;
    return g_RenderInstanceData.Load(particle_index);
}

void CalcParticleCoords( float4 InstanceData, in float2 CornerCoord, out float3 ViewPos, out float2 UV, out float Opacity)
{
    const float life_param = g_InvParticleLifeTime * InstanceData.w;

    // Transform to camera space
    ViewPos = mul(float4(InstanceData.xyz,1), g_matView).xyz;

    // Inflate corners, applying scale from instance data
    const float scale = lerp(g_ParticleBeginEndScale.x,g_ParticleBeginEndScale.y,life_param);
    ViewPos.xy += CornerCoord * scale;

    UV = 0.5f * (CornerCoord + 1.f);

    Opacity = 1.f - life_param * life_param;
}

GS_PARTICLE_COORDS CalcParticleCoords(in float4 InstanceData, int i)
{
    GS_PARTICLE_COORDS result;
    CalcParticleCoords( InstanceData, kParticleCornerCoords[i], result.ViewPos, result.TextureUVAndOpacity.xy, result.TextureUVAndOpacity.z);
    return result;
}

float4 GetParticleRGBA(SamplerState s, float2 uv, float alphaMult)
{
    const float base_alpha = 16.f/255.f;

    float4 raw_tex = g_texDiffuse.Sample(s, uv);
    raw_tex.a = saturate((raw_tex.a - base_alpha)/(1.f - base_alpha));
    raw_tex.a *= alphaMult * 0.3f;
    return raw_tex;
}

float noise_3_octave(float4 pos_time)
{
    return inoise(pos_time) + 0.5f * inoise(2.f*pos_time) + 0.25f * inoise(4.f*pos_time);
}

 const float4 noise_r_offset = float4(0.01,0.02,0.03,0.04);
 const float4 noise_g_offset = float4(0.05,0.06,0.07,0.08);
 const float4 noise_b_offset = float4(0.09,0.10,0.11,0.12);

float3 xyz_noise_3_octave(float4 pos_time)
{
    float3 result;
    result.x = noise_3_octave(pos_time + noise_r_offset);
    result.y = noise_3_octave(pos_time + noise_g_offset);
    result.z = noise_3_octave(pos_time + noise_b_offset);
    return result;
}

float3 wind_potential(float4 pos_time)
{
    float4 noise_coord;
    noise_coord.xyz = pos_time.xyz * g_NoiseSpatialScale;
    noise_coord.w = pos_time.w * g_NoiseTimeScale;
    float3 noise_component = g_WindVectorAndNoiseMult.w * xyz_noise_3_octave(noise_coord);
    float3 gross_wind_component = -cross(pos_time.xyz,g_WindVectorAndNoiseMult.xyz);
    return noise_component + gross_wind_component;
}

float3 wind_function(float4 pos_time)
{
    const float delta = 0.001f;
    float4 dx = float4(delta,0.f,0.f,0.f);
    float4 dy = float4(0.f,delta,0.f,0.f);
    float4 dz = float4(0.f,0.f,delta,0.f);

    float3 dx_pos = wind_potential(pos_time + dx);
    float3 dx_neg = wind_potential(pos_time - dx);

    float3 dy_pos = wind_potential(pos_time + dy);
    float3 dy_neg = wind_potential(pos_time - dy);

    float3 dz_pos = wind_potential(pos_time + dz);
    float3 dz_neg = wind_potential(pos_time - dz);

    float x = dy_pos.z - dy_neg.z - dz_pos.y + dz_neg.y;
    float y = dz_pos.x - dz_neg.x - dx_pos.z + dx_neg.z;
    float z = dx_pos.y - dx_neg.y - dy_pos.x + dy_neg.x;

    return float3(x,y,z)/(2.f*delta);
}

void simulate(inout float4 instance_data, inout float4 velocity, float elapsed_time)
{
    float3 wind_velocity = wind_function(float4(instance_data.xyz,g_NoiseTime));

    instance_data.xyz += elapsed_time * velocity.xyz;
    instance_data.w += elapsed_time;

    float3 relative_velocity = velocity.xyz - wind_velocity;
    float3 accel = -relative_velocity * g_WindDrag;
    accel.y += velocity.w;	// buoyancy
    velocity.xyz += accel * elapsed_time;

    // Reduce buoyancy with heat loss
    velocity.w *= exp(g_BuoyancyParams.z * elapsed_time);
}