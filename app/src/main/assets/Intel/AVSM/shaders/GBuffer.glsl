// Copyright 2012 Intel Corporation
// All Rights Reserved

// GBuffer and related common utilities and structures

#include "Common.glsl"
#include "AVSM.glsl"
#include "AVSM_Gen.glsl"

#ifndef H_GBUFFER
#define H_GBUFFER

struct NONCPUT_UIConstants
{
    uint  faceNormals;
    uint  enableStats;
    uint  volumeShadowMethod;
    uint  enableVolumeShadowLookup;
    uint  pauseParticleAnimaton;
    uint  particleOpacity;
    uint  vertexShaderShadowLookup;
    uint  tessellate;
    uint  wireframe;
    uint  lightingOnly;
    float particleSize;
    float TessellatioDensity;               //  1/desired triangle size
};

layout(binding = 0) uniform PerFrameConstants
{
    /*row_major*/   float4x4    mCameraWorldViewProj;
    /*row_major*/   float4x4    mCameraWorldView;
    /*row_major*/   float4x4    mCameraViewProj;
    /*row_major*/   float4x4    mCameraProj;
                    float4      mCameraPos;
    /*row_major*/   float4x4    mLightWorldViewProj;
    /*row_major*/   float4x4    mAvsmLightWorldViewProj;
    /*row_major*/   float4x4    mCameraViewToLightProj;
    /*row_major*/   float4x4    mCameraViewToLightView;
    /*row_major*/   float4x4    mCameraViewToAvsmLightProj;
    /*row_major*/   float4x4    mCameraViewToAvsmLightView;
                    float4      mLightDir;
                    float4		mScreenResolution;
                    float4      mScreenToViewConsts;

    NONCPUT_UIConstants mUI;
};


// data that we can read or derived from the surface shader outputs
struct SurfaceData
{
    float3 positionView;         // View space position
    float3 positionViewDX;       // Screen space derivatives
    float3 positionViewDY;       // of view space position
    float3 normal;               // View space normal
    float4 albedo;
    float2 lightTexCoord;        // Texture coordinates in light space, [0, 1]
    float2 lightTexCoordDX;      // Screen space partial derivatives
    float2 lightTexCoordDY;      // of light space texture coordinates.
    float  lightSpaceZ;          // Z coordinate (depth) of surface in light space
};

float2 ProjectIntoLightTexCoord(float3 positionView)
{
    float4 positionLight = mul(float4(positionView, 1.0f), mCameraViewToLightProj);
#ifdef DEBUG_DX
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, -0.5f) + float2(0.5f, 0.5f);
#else
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, +0.5f) + float2(0.5f, 0.5f);
#endif
    return texCoord;
}

float2 ProjectIntoAvsmLightTexCoord(float3 positionView)
{
    float4 positionLight = mul(float4(positionView, 1.0f), mCameraViewToAvsmLightProj);
#ifdef DEBUG_DX
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, -0.5f) + float2(0.5f, 0.5f);
#else
    float2 texCoord = (positionLight.xy / positionLight.w) * float2(0.5f, +0.5f) + float2(0.5f, 0.5f);
#endif
    return texCoord;
}

//Texture2D                     gDepthBuffer                          : register(t30);
layout(binding = 3) uniform sampler2D  gDepthBuffer;
////////////////////////////////////////////////////////////////////////////////////////////
// getting the viewspace depth for smooth particle - solid geometry intersection
float ScreenToViewDepth( float screenDepth )
{
   float depthHackMul = mScreenToViewConsts.x;
   float depthHackAdd = mScreenToViewConsts.y;

   // Optimised version of "-cameraClipNear / (cameraClipFar - projDepth * (cameraClipFar - cameraClipNear)) * cameraClipFar"

   // Set your depthHackMul and depthHackAdd to:
   // depthHackMul = ( cameraClipFar * cameraClipNear) / ( cameraClipFar - cameraClipNear );
   // depthHackAdd = cameraClipFar / ( cameraClipFar - cameraClipNear );

 	return depthHackMul / (depthHackAdd - screenDepth);
}
//
float LoadScreenDepthViewspace( int2 pos )
{
   return ScreenToViewDepth( /*gDepthBuffer.Load( int3( pos.xy, 0 ) ).x*/ texelFetch(gDepthBuffer, pos, 0).x );
}
////////////////////////////////////////////////////////////////////////////////////////////

// Generalized volume sampling function
float VolumeSample(in uint method, in float2 textureCoords, in float receiverDepth)
{
    switch (method) {
        case(VOL_SHADOW_AVSM):
#ifdef AVSM_BILINEARF
            return AVSMBilinearSample(textureCoords, receiverDepth);
#else
            return AVSMPointSample(textureCoords, receiverDepth);
#endif
        case(VOL_SHADOW_AVSM_GEN):
#ifdef AVSM_GEN_SOFT
    #ifdef AVSM_GEN_BILINEARF
			    return AVSMGenBilinearSampleSoft(textureCoords, receiverDepth);
    #else
                return AVSMGenPointSampleSoft(textureCoords, receiverDepth);
    #endif
#else
    #ifdef AVSM_GEN_BILINEARF
			    return AVSMGenBilinearSample(textureCoords, receiverDepth);
    #else
                return AVSMGenPointSample(textureCoords, receiverDepth);
    #endif
#endif
        default:
            return 1.0f;
    }
}

float ShadowContrib(SurfaceData LitSurface, DynamicParticlePSIn Input)
{
    float2 lightTexCoord = ProjectIntoAvsmLightTexCoord(LitSurface.positionView.xyz);
    float receiverDepth = mul(float4(LitSurface.positionView.xyz, 1.0f), mCameraViewToAvsmLightView).z;

    return VolumeSample(mUI.volumeShadowMethod, lightTexCoord, receiverDepth);
}

#endif // H_GBUFFER
