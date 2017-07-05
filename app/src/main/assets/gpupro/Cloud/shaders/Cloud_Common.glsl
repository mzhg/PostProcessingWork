//------------------------------------------------
// Copyright (C) Kaori Kubota. All rights reserved.
//------------------------------------------------
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

uniform float4x4 mC2W;  // inverse matrix of world to projection matrix
uniform float2   vPix;  // 0.5f/texture size
uniform float4   vOff;  // parameter to compute blur direction

#define FILTER_WIDTH 16

layout(binding = 0)uniform sampler2D sDensity;  // density map

uniform float3  vParam;   //
uniform float4  vFallOff; // fall off parameters
uniform float2  invMax;   // inverse of the maximum length of the blur vector
uniform float3  vEye;     // view position

uniform float4x4 mW2C;   // world to projection matrix
uniform float4 vXZParam; // scale and offset for x and z position
uniform float2 vHeight;  // height parameter
uniform float4 vUVParam; // uv scale and offset

layout(binding = 0)uniform sampler2D sCloud;   // cloud texture
uniform float fCloudCover;               // cloud cover

uniform float3   litDir;  // ligtht direction

layout(binding = 1)uniform sampler2D sLit;      // blurred densitymap

uniform float4 scat[5];   // scattering parameters
uniform float2 vDistance; // parameter for computing distance to the cloud
uniform float3 cLit;      // light color
uniform float3 cAmb;      // ambient light

uniform float4x4 mL2C;  // local to projection matrix
uniform float4x4 mL2W;  // local to world transform
uniform float4x4 mL2S;  // local to shadowmap texture

layout(binding = 0)uniform sampler2D sGroundBlend;  // blend texture
layout(binding = 1)uniform sampler2D sGround0;      // ground textures
layout(binding = 2)uniform sampler2D sGround1;
layout(binding = 3)uniform sampler2D sGround2;
layout(binding = 4)uniform sampler2D sGround3;
layout(binding = 5)uniform sampler2D sGround4;
layout(binding = 6)uniform sampler2D sShadow;       // cloud shadowmap

uniform float3 litCol;  // light color
uniform float3 litAmb;  // ambient light
uniform float4 mDif;    // diffuse reflection
uniform float4 mSpc;    // specular reflection