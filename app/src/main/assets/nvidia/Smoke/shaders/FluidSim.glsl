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
// Shaders to implement a "stable fluids" style semi-Lagrangian solver for 3D smoke
//--------------------------------------------------------------------------------------
// It assumes the velocity and pressure grids are collocated
// It handles boundary conditions for static obstacles stored as an in/out voxel volume
// MACCORMACK is supported for smoke density advection
// The diffusion step is skipped
//--------------------------------------------------------------------------------------

#define FT_SMOKE  0
#define FT_FIRE   1
#define FT_LIQUID 2

layout(binding = 0) uniform sampler2D Texture_inDensity;

layout(binding = 1) uniform sampler3D Texture_pressure;
layout(binding = 2) uniform sampler3D Texture_velocity;
layout(binding = 3) uniform sampler3D Texture_vorticity;
layout(binding = 4) uniform sampler3D Texture_divergence;

layout(binding = 5) uniform sampler3D Texture_phi;
layout(binding = 6) uniform sampler3D Texture_phi_hat;
layout(binding = 7) uniform sampler3D Texture_phi_next;
layout(binding = 8) uniform sampler3D Texture_levelset;

layout(binding = 9) uniform sampler3D Texture_obstacles;
layout(binding = 10) uniform sampler3D Texture_obstvelocity;

//--------------------------------------------------------------------------------------
// Variables
//--------------------------------------------------------------------------------------

uniform int         fluidType = FT_SMOKE;
uniform bool        advectAsTemperature = false;
uniform bool        treatAsLiquidVelocity = false;

uniform int         drawTextureNumber = 1;

uniform float       textureWidth;
uniform float       textureHeight;
uniform float       textureDepth;

uniform float       liquidHeight = 24;

// NOTE: The spacing between simulation grid cells is \delta x  = 1, so it is omitted everywhere
uniform float       timestep                = 1.0f;
uniform float       decay                   = 1.0f; // this is the (1.0 - dissipation_rate). dissipation_rate >= 0 ==> decay <= 1
uniform float       rho                     = 1.2f; // rho = density of the fluid
uniform float       viscosity               = 5e-6f;// kinematic viscosity
uniform float       vortConfinementScale    = 0.0f; // this is typically a small value >= 0
uniform vec3        gravity                 = vec3(0);    // note this is assumed to be given as pre-multiplied by the timestep, so it's really velocity: cells per step
uniform float       temperatureLoss         = 0.003;// a constant amount subtracted at every step when advecting a quatnity as tempterature

uniform float       radius;
uniform float3      center;
uniform float4      color;

uniform float4      obstBoxVelocity = float4(0, 0, 0, 0);
uniform float3      obstBoxLBDcorner;
uniform float3      obstBoxRTUcorner;

//parameters for attenuating velocity based on porous obstacles.
//these values are not hooked into CPP code yet, and so this option is not used currently
uniform bool        doVelocityAttenuation = false;
uniform float       maxDensityAmount = 0.7;
uniform float       maxDensityDecay = 0.95;
//--------------------------------------------------------------------------------------
// Pipeline State definitions
//--------------------------------------------------------------------------------------



/*
struct GS_OUTPUT_FLUIDSIM
{
    float4 pos               : SV_Position; // 2D slice vertex coordinates in homogenous clip space
    float3 cell0             : TEXCOORD0;   // 3D cell coordinates (x,y,z in 0-dimension range)
    float3 texcoords         : TEXCOORD1;   // 3D cell texcoords (x,y,z in 0-1 range)
    float2 LR                : TEXCOORD2;   // 3D cell texcoords for the Left and Right neighbors
    float2 BT                : TEXCOORD3;   // 3D cell texcoords for the Bottom and Top neighbors
    float2 DU                : TEXCOORD4;   // 3D cell texcoords for the Down and Up neighbors
    uint RTIndex             : SV_RenderTargetArrayIndex;  // used to choose the destination slice
};
*/

#define LEFTCELL    float3 (_input.LR.x, _input.texcoords.y, _input.texcoords.z)
#define RIGHTCELL   float3 (_input.LR.y, _input.texcoords.y, _input.texcoords.z)
#define BOTTOMCELL  float3 (_input.texcoords.x, _input.BT.x, _input.texcoords.z)
#define TOPCELL     float3 (_input.texcoords.x, _input.BT.y, _input.texcoords.z)
#define DOWNCELL    float3 (_input.texcoords.x, _input.texcoords.y, _input.DU.x)
#define UPCELL      float3 (_input.texcoords.x, _input.texcoords.y, _input.DU.y)

//--------------------------------------------------------------------------------------
// Helper functions
//--------------------------------------------------------------------------------------

float4 GetObstVelocity( float3 cellTexCoords )
{
//    return Texture_obstvelocity.SampleLevel(samPointClamp, cellTexCoords, 0);
    return textureLod(Texture_obstvelocity, cellTexCoords, 0.);
}

bool IsNonEmptyCell( float3 cellTexCoords )
{
    return (//Texture_obstacles.SampleLevel(samPointClamp, cellTexCoords, 0).r
        textureLod(Texture_obstacles, cellTexCoords, 0.).r
    <= OBSTACLE_BOUNDARY);
}

bool IsNonEmptyNonBoundaryCell( float3 cellTexCoords )
{
    float obst = //Texture_obstacles.SampleLevel(samPointClamp, cellTexCoords, 0).r;
                    textureLod(Texture_obstacles, cellTexCoords, 0.).r;
    return  (obst < OBSTACLE_BOUNDARY);
}

bool IsBoundaryCell( float3 cellTexCoords )
{
    return (/*Texture_obstacles.SampleLevel(samPointClamp, cellTexCoords, 0).r*/
     textureLod(Texture_obstacles, cellTexCoords, 0.).r
     == OBSTACLE_BOUNDARY);
}
bool IsOutsideSimulationDomain( float3 cellTexcoords )
{
    if( treatAsLiquidVelocity )
    {
//        if( Texture_levelset.SampleLevel(samPointClamp, cellTexcoords, 0 ).r <= 0 )
        if(textureLod(Texture_levelset, cellTexcoords, 0.).r <= 0.)
            return false;
        else
            return true;
    }
    else
    {
        return false;
    }
}
