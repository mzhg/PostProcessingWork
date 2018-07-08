//----------------------------------------------------------------------------------
// File:   FluidSim.fx
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

// These are the values used when filling the obstacle textures
#define OBSTACLE_EXTERIOR  1.0f
#define OBSTACLE_BOUNDARY  128.0f/255.0f
#define OBSTACLE_INTERIOR  0.0f

//--------------------------------------------------------------------------------------
// Textures
//--------------------------------------------------------------------------------------
//Texture2D<uint2>      stencilbufferTex2D;
layout(binding = 0) uniform usampler2D stencilbufferTex2D;

//--------------------------------------------------------------------------------------
// Variables
//--------------------------------------------------------------------------------------
uniform float4x4   WorldViewProjection /*: WORLDVIEWPROJECTION*/;

uniform float2 projSpacePixDim; // the dimensions of a pixel in projection space i.e. (2.0/rtWidth, 2.0/rtHeight)
uniform float3 gridDim;
uniform float recTimeStep;

uniform int sliceIdx;       // index of the slice we want to output into
uniform float sliceZ;       // z in the range [-0.5, 0.5] for the slice we are outputting into
uniform float velocityMultiplier = 1.0; //if rendering fire we reduce the effect of the mesh on the velocity field to 20%