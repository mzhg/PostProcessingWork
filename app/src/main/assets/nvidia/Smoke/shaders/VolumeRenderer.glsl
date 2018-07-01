//----------------------------------------------------------------------------------
// File:   VolumeRenderer.glsl
// Author: Sarah Tariq and Ignacio Llamas
// Email:  sdkfeedback@nvidia.com
//
// Copyright (c) 2007 NVIDIA Corporation. All rights reserved.
//
// TO  THE MAXIMUM  EXTENT PERMITTED  BY APPLICABLE  LAW, THIS SOFTWARE  IS PROVIDED
// *AS IS*  AND NVIDIA AND  ITS SUPPLIERS DISCLAIM  ALL WARRANTIES,  EITHER  EXPRESS
// OR IMPLIED, INCLUDING, BUT NOT LIMITED  TO, IMPLIED WARRANTIES OF MERCHANTABILITY
// AND FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT SHALL  NVIDIA OR ITS SUPPLIERS
// BE  LIABLE  FOR  ANY  SPECIAL,  INCIDENTAL,  INDIRECT,  OR  CONSEQUENTIAL DAMAGES
// WHATSOEVER (INCLUDING, WITHOUT LIMITATION,  DAMAGES FOR LOSS OF BUSINESS PROFITS,
// BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, OR ANY OTHER PECUNIARY LOSS)
// ARISING OUT OF THE  USE OF OR INABILITY  TO USE THIS SOFTWARE, EVEN IF NVIDIA HAS
// BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
//
//
//----------------------------------------------------------------------------------
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#define RM_SMOKE    0
#define RM_FIRE     1
#define RM_LEVELSET 2
//--------------------------------------------------------------------------------------
// Textures
//--------------------------------------------------------------------------------------
layout(binding = 0) uniform sampler3D   volumeTex;
layout(binding = 0) uniform sampler2D   rayDataTex;
layout(binding = 0) uniform sampler2D   rayDataTexSmall;
layout(binding = 0) uniform sampler2D   rayCastTex;
layout(binding = 0) uniform sampler2D   sceneDepthTex;
layout(binding = 0) uniform sampler2D   edgeTex;
layout(binding = 0) uniform sampler2D   jitterTex;
layout(binding = 0) uniform sampler2D   fireTransferFunction;
layout(binding = 0) uniform sampler2D   glowTex;
layout(binding = 0) uniform samplerCube envMapTex;
//--------------------------------------------------------------------------------------
// Variables
//--------------------------------------------------------------------------------------
uniform float       RTWidth;
uniform float       RTHeight;

uniform float4x4    WorldViewProjection;
uniform float4x4    InvWorldViewProjection;
uniform float4x4    WorldView;

uniform float4x4    Grid2World;

uniform float       ZNear = 0.05f;
uniform float       ZFar = 1000.0f;

uniform float3      gridDim;
uniform float3      recGridDim;
uniform float       maxGridDim;
uniform float       gridScaleFactor = 1.0;
uniform float3      eyeOnGrid;

uniform float       edgeThreshold = 0.2;

uniform float       tan_FovXhalf;
uniform float       tan_FovYhalf;

//gaussian with a sigma of 3, and a miu of 0
const float gaussian_3[5] =  float[]
(
    0.132981, 0.125794, 0.106483, 0.080657, 0.05467,
);

uniform bool useGlow               = true;
uniform float glowContribution     = 0.81f;
uniform float finalIntensityScale  = 22.0f;
uniform float finalAlphaScale      = 0.95f;
uniform float smokeColorMultiplier = 2.0f;
uniform float smokeAlphaMultiplier = 0.1f;
uniform float fireAlphaMultiplier  = 0.4;
uniform int   rednessFactor        = 5;

uniform bool        g_bRaycastBisection      = true; // true: compute more accurate ray-surface intersection; false: use first hit position
uniform bool        g_bRaycastFilterTricubic = true; // true: tricubic; false: trilinear
uniform bool        g_bRaycastShadeAsWater   = true; // true: shade using reflection+refraction from environment map; false: output gradient

#define OCCLUDED_PIXEL_RAYVALUE     float4(1, 0, 0, 0)
#define NEARCLIPPED_PIXEL_RAYPOS    float3(0, -1, 0)