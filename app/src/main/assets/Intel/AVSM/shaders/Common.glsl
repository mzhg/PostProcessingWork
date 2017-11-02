// Copyright 2012 Intel Corporation
// All Rights Reserved
//


#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

#ifndef H_COMMON
#define H_COMMON

//////////////////////////////////////////////
// Defines
//////////////////////////////////////////////

#define AVSM_FILTERING_ENABLED
#define LT_BILINEAR_FILTERING

//////////////////////////////////////////////
// Full screen pass
//////////////////////////////////////////////

struct FullScreenTriangleVSOut
{
//    float4 positionViewport : SV_Position;
    float4 positionClip     /*: positionClip*/;
    float2 texCoord         /*: texCoord*/;
};

//////////////////////////////////////////////
// Particle renderer
//////////////////////////////////////////////

struct DynamicParticlePSIn
{
//    float4 Position  : SV_POSITION;
    float3 UVS		 /*: TEXCOORD0*/;
    float  Opacity	 /*: TEXCOORD1*/;
    float3 ViewPos	 /*: TEXCOORD2*/;
    float3 ObjPos    /*: TEXCOORD3*/;
    float3 ViewCenter/*: TEXCOORD4*/;
    float4 color      /*: COLOR*/;
    float2 ShadowInfo /*: TEXCOORD5*/;
};

struct ParticlePSOut
{
    float4 color      /*: COLOR*/;
};

/*struct HS_CONSTANT_DATA_OUTPUT
{
    float    Edges[3]         : SV_TessFactor;
    float    Inside           : SV_InsideTessFactor;
    float3   DebugColor       : TEXCOORD0;
};*/

struct VS_OUTPUT_HS_INPUT
{
    float4 vWorldPos /*: WORLDPOS*/;
    float4 vScreenPos /*: SCREENPOS*/;
    float3 texCoord  /*: TEXCOORD0*/;
    float  inOpacity /*: TEXCOORD1*/;
};



struct HS_CONTROL_POINT_OUTPUT
{
    float4 vWorldPos /*: WORLDPOS*/;
    float4 vScreenPos /*: SCREENPOS*/;
    float3 texCoord  /*: TEXCOORD0*/;
    float  inOpacity /*: TEXCOORD1*/;
};

//////////////////////////////////////////////
// Helper Functions
//////////////////////////////////////////////

float linstep(float min, float max, float v)
{
    return saturate((v - min) / (max - min));
}

#endif // H_COMMON