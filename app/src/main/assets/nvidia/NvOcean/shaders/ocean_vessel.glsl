#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "ocean_shader_common.h"
#include "shader_common.fxh"
#include "atmospheric.glsl"

//------------------------------------------------------------------------------------
// Global variables
//------------------------------------------------------------------------------------

// XForm Matrix
uniform float4x4	g_matWorldViewProj;
uniform float4x4	g_matWorldView;
uniform float4x4	g_matWorld;
uniform float4		g_DiffuseColor;
uniform float3		g_LightDirection;
uniform float3		g_LightColor;
uniform float3		g_AmbientColor;
uniform float3		g_LightningPosition;
uniform float3		g_LightningColor;

uniform float		g_FogExponent;

uniform int			g_LightsNum;
uniform float4		g_SpotlightPosition[MaxNumSpotlights];
uniform float4		g_SpotLightAxisAndCosAngle[MaxNumSpotlights];
uniform float4		g_SpotlightColor[MaxNumSpotlights];

#if ENABLE_SHADOWS
uniform float4x4    g_SpotlightMatrix[MaxNumSpotlights];
uniform sampler2DShadow   g_SpotlightResource[MaxNumSpotlights];
#endif

//-----------------------------------------------------------------------------------
// Texture & Samplers
//-----------------------------------------------------------------------------------
uniform sampler2D g_texDiffuse;
uniform sampler2D g_texRustMap;
uniform sampler2D g_texRust;
uniform sampler2D g_texBump;