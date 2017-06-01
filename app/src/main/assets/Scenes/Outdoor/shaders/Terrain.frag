// Copyright 2013 Intel Corporation
// All Rights Reserved
//
// Permission is granted to use, copy, distribute and prepare derivative works of this
// software for any purpose and without fee, provided, that the above copyright notice
// and this statement appear in all copies.  Intel makes no representations about the
// suitability of this software for any purpose.  THIS SOFTWARE IS PROVIDED "AS IS."
// INTEL SPECIFICALLY DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, AND ALL LIABILITY,
// INCLUDING CONSEQUENTIAL AND OTHER INDIRECT DAMAGES, FOR THE USE OF THIS SOFTWARE,
// INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PROPRIETARY RIGHTS, AND INCLUDING THE
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Intel does not
// assume any responsibility for any errors which may appear in this software nor any
// responsibility to update it.

#include "../../../shader_libs/OutdoorSctr/Base.frag"

// Texturing modes
#define TM_HEIGHT_BASED 0             // Simple height-based texturing mode using 1D look-up table
#define TM_MATERIAL_MASK 1
#define TM_MATERIAL_MASK_NM 2

#ifndef TEXTURING_MODE
#   define TEXTURING_MODE TM_MATERIAL_MASK_NM
#endif

#ifndef NUM_TILE_TEXTURES
#	define NUM_TILE_TEXTURES 5
#endif

#ifndef NUM_SHADOW_CASCADES
#   define NUM_SHADOW_CASCADES 4
#endif

#define TEX2D_AMBIENT_SKY_LIGHT 0
#define TEX2D_OCCLUDED_NET_DENSITY_TO_ATMTOP 1
#define TEX2D_NORMAL_MAP 2
#define TEX2D_SHADOWMAP  3
#define TEX2D_MTRLMAP    4
#define TEX2D_TILE_TEXTURES  5
#define TEX2D_TILE_NORMAL_MAPS 10
#define TEX2D_ELEVATION_MAP   11

// Common uniform variable declared
uniform mat4 g_WorldViewProj;
uniform mat4 g_WorldToLightView;
uniform vec4 g_f4LightSpaceScale[NUM_SHADOW_CASCADES];
uniform vec4 g_f4LightSpaceScaledBias[NUM_SHADOW_CASCADES];
uniform float g_f4CascadeCamSpaceZEnd[NUM_SHADOW_CASCADES];
uniform float g_fBaseMtrlTilingScale = 100.0;
uniform vec4 g_f4CascadeColors[NUM_SHADOW_CASCADES];
uniform vec4 g_f4TilingScale = float4(100);
uniform bool g_bVisualizeCascades = false;
uniform float g_fFarPlaneZ;
uniform float g_fNearPlaneZ;

layout(binding = TEX2D_AMBIENT_SKY_LIGHT) 			   uniform sampler2D g_tex2DAmbientSkylight;
layout(binding = TEX2D_OCCLUDED_NET_DENSITY_TO_ATMTOP) uniform sampler2D g_tex2DOccludedNetDensityToAtmTop;
layout(binding = TEX2D_NORMAL_MAP) 					   uniform sampler2D g_tex2DNormalMap;
layout(binding = TEX2D_MTRLMAP) 					   uniform sampler2D g_tex2DMtrlMap;
layout(binding = TEX2D_SHADOWMAP) 					   uniform sampler2DArrayShadow g_tex2DShadowMap;
layout(binding = TEX2D_TILE_TEXTURES) 				   uniform sampler2D g_tex2DTileTextures[NUM_TILE_TEXTURES];
layout(binding = TEX2D_TILE_NORMAL_MAPS) 			   uniform sampler2D g_tex2DTileNormalMaps[NUM_TILE_TEXTURES];
layout(binding = TEX2D_ELEVATION_MAP) 			   	   uniform sampler2D g_tex2DElevationMap;