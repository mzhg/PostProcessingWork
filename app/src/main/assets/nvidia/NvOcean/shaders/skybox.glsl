#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "atmospheric.fxh"

//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------

// XForm Matrix
uniform float4x4	g_matViewProj;
uniform float4x4	g_matProjInv;
uniform float3		g_FogColor;
uniform float		g_FogExponent;
uniform float3		g_LightningColor;
uniform float		g_CloudFactor;

const float kEarthRadius = 6400000;
const float kCloudbaseHeight = 250;
const float kMaxCloudbaseDistance = 2000;
const float kMinFogFactor = 0.f;


uniform float3 g_LightPos;												// The direction to the light source

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
layout(binding = 0) uniform samplerCube	g_texSkyCube0;
layout(binding = 1) uniform samplerCube	g_texSkyCube1;
uniform float g_SkyCubeBlend;
uniform float2 g_SkyCube0RotateSinCos;
uniform float2 g_SkyCube1RotateSinCos;
uniform float4 g_SkyCubeMult;

/*Texture2D g_texColor;
Texture2DMS<float> g_texDepthMS;
Texture2D<float> g_texDepth;*/

float3 rotateXY(float3 xyz, float2 sc)
{
    float3 result = xyz;
    float s = sc.x;
    float c = sc.y;
    result.x = xyz.x * c - xyz.y * s;
    result.y = xyz.x * s + xyz.y * c;
    return result;
}