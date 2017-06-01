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

#include "Base.frag"

#define FLT_MAX 3.402823466e+38f

#define RGB_TO_LUMINANCE float3(0.212671, 0.715160, 0.072169)

// Using static definitions instead of constant buffer variables is 
// more efficient because the compiler is able to optimize the code 
// more aggressively

#ifndef NUM_EPIPOLAR_SLICES
#   define NUM_EPIPOLAR_SLICES 1024
#endif

#ifndef MAX_SAMPLES_IN_SLICE
#   define MAX_SAMPLES_IN_SLICE 512
#endif

#ifndef SCREEN_RESLOUTION
#   define SCREEN_RESLOUTION float2(1280,720)
#endif

#define MIN_MAX_DATA_FORMAT float2

#ifndef CASCADE_PROCESSING_MODE
#   define CASCADE_PROCESSING_MODE CASCADE_PROCESSING_MODE_SINGLE_PASS
#endif

#ifndef USE_COMBINED_MIN_MAX_TEXTURE
#   define USE_COMBINED_MIN_MAX_TEXTURE 1
#endif

#ifndef EXTINCTION_EVAL_MODE
#   define EXTINCTION_EVAL_MODE EXTINCTION_EVAL_MODE_EPIPOLAR
#endif

#ifndef AUTO_EXPOSURE
#   define AUTO_EXPOSURE 0  //disable auto exposure
#endif

#ifndef OPTIMIZE_SAMPLE_LOCATIONS
#   define OPTIMIZE_SAMPLE_LOCATIONS 1
#endif

#ifndef CORRECT_INSCATTERING_AT_DEPTH_BREAKS
#   define CORRECT_INSCATTERING_AT_DEPTH_BREAKS 0
#endif

//#define SHADOW_MAP_DEPTH_BIAS 1e-4

#ifndef TRAPEZOIDAL_INTEGRATION
#   define TRAPEZOIDAL_INTEGRATION 1
#endif

#ifndef ENABLE_LIGHT_SHAFTS
#   define ENABLE_LIGHT_SHAFTS 0
#endif

#ifndef IS_32BIT_MIN_MAX_MAP
#   define IS_32BIT_MIN_MAX_MAP 0
#endif

#ifndef SINGLE_SCATTERING_MODE
#   define SINGLE_SCATTERING_MODE SINGLE_SCTR_MODE_LUT
#endif

#ifndef MULTIPLE_SCATTERING_MODE
#   define MULTIPLE_SCATTERING_MODE MULTIPLE_SCTR_MODE_UNOCCLUDED
#endif

#ifndef PRECOMPUTED_SCTR_LUT_DIM
#   define PRECOMPUTED_SCTR_LUT_DIM float4(32,128,64,16)
#endif

#ifndef NUM_RANDOM_SPHERE_SAMPLES
#   define NUM_RANDOM_SPHERE_SAMPLES 128
#endif

#ifndef PERFORM_TONE_MAPPING
#   define PERFORM_TONE_MAPPING 1
#endif

#ifndef LOW_RES_LUMINANCE_MIPS
#   define LOW_RES_LUMINANCE_MIPS 7
#endif

#ifndef TONE_MAPPING_MODE
#   define TONE_MAPPING_MODE TONE_MAPPING_MODE_UNCHARTED2
#endif

#ifndef LIGHT_ADAPTATION
#   define LIGHT_ADAPTATION 1
#endif


#define NON_LINEAR_PARAMETERIZATION 1

#define INVALID_EPIPOLAR_LINE float4(-1000,-1000, -100, -100)

const float HeightPower = 0.5f;
const float ViewZenithPower = 0.2;
const float SunViewPower = 1.5f;

/*
//--------------------------------------------------------------------------------------
// Texture samplers
//--------------------------------------------------------------------------------------

SamplerState samLinearBorder0 : register( s1 )
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Border;
    AddressV = Border;
    BorderColor = float4(0.0, 0.0, 0.0, 0.0);
};

SamplerComparisonState samComparison : register( s2 )
{
    Filter = COMPARISON_MIN_MAG_LINEAR_MIP_POINT;
    AddressU = Border;
    AddressV = Border;
    ComparisonFunc = GREATER;
    BorderColor = float4(0.0, 0.0, 0.0, 0.0);
};

SamplerState samPointClamp : register( s3 );

//--------------------------------------------------------------------------------------
// Depth stencil states
//--------------------------------------------------------------------------------------

// Depth stencil state disabling depth test
DepthStencilState DSS_NoDepthTest
{
    DepthEnable = false;
    DepthWriteMask = ZERO;
};

DepthStencilState DSS_NoDepthTestIncrStencil
{
    DepthEnable = false;
    DepthWriteMask = ZERO;
    STENCILENABLE = true;
    FRONTFACESTENCILFUNC = ALWAYS;
    BACKFACESTENCILFUNC = ALWAYS;
    FRONTFACESTENCILPASS = INCR;
    BACKFACESTENCILPASS = INCR;
};

DepthStencilState DSS_NoDepth_StEqual_IncrStencil
{
    DepthEnable = false;
    DepthWriteMask = ZERO;
    STENCILENABLE = true;
    FRONTFACESTENCILFUNC = EQUAL;
    BACKFACESTENCILFUNC = EQUAL;
    FRONTFACESTENCILPASS = INCR;
    BACKFACESTENCILPASS = INCR;
    FRONTFACESTENCILFAIL = KEEP;
    BACKFACESTENCILFAIL = KEEP;
};

DepthStencilState DSS_NoDepth_StEqual_KeepStencil
{
    DepthEnable = false;
    DepthWriteMask = ZERO;
    STENCILENABLE = true;
    FRONTFACESTENCILFUNC = EQUAL;
    BACKFACESTENCILFUNC = EQUAL;
    FRONTFACESTENCILPASS = KEEP;
    BACKFACESTENCILPASS = KEEP;
    FRONTFACESTENCILFAIL = KEEP;
    BACKFACESTENCILFAIL = KEEP;
};

//--------------------------------------------------------------------------------------
// Rasterizer states
//--------------------------------------------------------------------------------------

// Rasterizer state for solid fill mode with no culling
RasterizerState RS_SolidFill_NoCull
{
    FILLMODE = Solid;
    CullMode = NONE;
};


// Blend state disabling blending
BlendState NoBlending
{
    BlendEnable[0] = FALSE;
    BlendEnable[1] = FALSE;
    BlendEnable[2] = FALSE;
};
*/

/*
SamplerState samLinearClamp : register( s0 )
{
    Filter = MIN_MAG_MIP_LINEAR;
    AddressU = Clamp;
    AddressV = Clamp;
};

Texture2D<float>  g_tex2DDepthBuffer            : register( t0 );
Texture2D<float>  g_tex2DCamSpaceZ              : register( t0 );
Texture2D<float4> g_tex2DSliceEndPoints         : register( t4 );
Texture2D<float2> g_tex2DCoordinates            : register( t1 );
Texture2D<float>  g_tex2DEpipolarCamSpaceZ      : register( t2 );
Texture2D<uint2>  g_tex2DInterpolationSource    : register( t6 );
Texture2DArray<float> g_tex2DLightSpaceDepthMap : register( t3 );
Texture2D<float4> g_tex2DSliceUVDirAndOrigin    : register( t6 );
Texture2D<MIN_MAX_DATA_FORMAT> g_tex2DMinMaxLightSpaceDepth  : register( t4 );
Texture2D<float3> g_tex2DInitialInsctrIrradiance: register( t5 );
Texture2D<float4> g_tex2DColorBuffer            : register( t1 );
Texture2D<float3> g_tex2DScatteredColor         : register( t3 );
Texture2D<float2> g_tex2DOccludedNetDensityToAtmTop : register( t5 );
Texture2D<float3> g_tex2DEpipolarExtinction     : register( t6 );
Texture3D<float3> g_tex3DSingleSctrLUT          : register( t7 );
Texture3D<float3> g_tex3DHighOrderSctrLUT       : register( t8 );
Texture3D<float3> g_tex3DMultipleSctrLUT        : register( t9 );
Texture2D<float3> g_tex2DSphereRandomSampling   : register( t1 );
Texture3D<float3> g_tex3DPreviousSctrOrder      : register( t0 );
Texture3D<float3> g_tex3DPointwiseSctrRadiance  : register( t0 );
Texture2D<float>  g_tex2DAverageLuminance       : register( t10 );
Texture2D<float>  g_tex2DLowResLuminance        : register( t0 );
*/

#define TEX2D_DEPTH   					0
#define TEX2D_CAM_SPACE  				1
#define TEX2D_SLICE_END_POINTS  		2
#define TEX2D_COORDINATES  				3
#define TEX2D_EPIPOLAR_CAM_SPACE  		4
#define TEX2D_INTERPOLATION_SOURCE 		5
#define TEX2D_LIGHT_SPACE_DEPTH    		6
#define TEX2D_SLICE_UV_ORIGIN      		7
#define TEX2D_MIN_MAX_LIGHT_DEPTH  		8
#define TEX2D_INITIAL_IRRANDIANCE 		9
#define TEX2D_COLOR  					10
#define TEX2D_SCATTERED_COLOR 			11
#define TEX2D_OCCLUDED_NET_DENSITY 		12
#define TEX2D_EPIPOLAR_EXTINCTION 		13
#define TEX3D_SINGLE_LUT 				14
#define TEX3D_HIGH_ORDER_LUT 			15
#define TEX3D_MULTIPLE_LUT  			16
#define TEX2D_SPHERE_RANDOM      		17
#define TEX3D_PREVIOUS_RADIANCE 		18
#define TEX3D_POINT_TWISE_RANDIANCE 	19
#define TEX2D_AVERAGE_LUMINACE  		20
#define TEX2D_LOW_RES_LUMINACE  		21
#define TEX2D_SHADOW_MAP                22

layout(binding = TEX2D_DEPTH) uniform sampler2D  g_tex2DDepthBuffer            ;// register( t0 );
layout(binding = TEX2D_CAM_SPACE) uniform sampler2D  g_tex2DCamSpaceZ              ;// register( t0 );
layout(binding = TEX2D_SLICE_END_POINTS) uniform sampler2D  g_tex2DSliceEndPoints         ;// register( t4 );
layout(binding = TEX2D_COORDINATES) uniform sampler2D  g_tex2DCoordinates            ;// register( t1 );
layout(binding = TEX2D_EPIPOLAR_CAM_SPACE) uniform sampler2D  g_tex2DEpipolarCamSpaceZ      ;// register( t2 );
layout(binding = TEX2D_INTERPOLATION_SOURCE) uniform usampler2D  g_tex2DInterpolationSource    ;// register( t6 );
layout(binding = TEX2D_LIGHT_SPACE_DEPTH) uniform sampler2DArray g_tex2DLightSpaceDepthMap ;// register( t3 );
layout(binding = TEX2D_SLICE_UV_ORIGIN) uniform sampler2D  g_tex2DSliceUVDirAndOrigin    ;// register( t6 );
layout(binding = TEX2D_MIN_MAX_LIGHT_DEPTH) uniform sampler2D  g_tex2DMinMaxLightSpaceDepth  ;// register( t4 );
layout(binding = TEX2D_INITIAL_IRRANDIANCE) uniform sampler2D  g_tex2DInitialInsctrIrradiance;// register( t5 );
layout(binding = TEX2D_COLOR) uniform sampler2D  g_tex2DColorBuffer            ;// register( t1 );
layout(binding = TEX2D_SCATTERED_COLOR) uniform sampler2D  g_tex2DScatteredColor         ;// register( t3 );
layout(binding = TEX2D_OCCLUDED_NET_DENSITY) uniform sampler2D g_tex2DOccludedNetDensityToAtmTop ;// register( t5 );
layout(binding = TEX2D_EPIPOLAR_EXTINCTION) uniform sampler2D g_tex2DEpipolarExtinction     ;// register( t6 );
layout(binding = TEX3D_SINGLE_LUT) uniform sampler3D g_tex3DSingleSctrLUT          ;// register( t7 );
layout(binding = TEX3D_HIGH_ORDER_LUT) uniform sampler3D g_tex3DHighOrderSctrLUT       ;// register( t8 );
layout(binding = TEX3D_MULTIPLE_LUT) uniform sampler3D g_tex3DMultipleSctrLUT        ;// register( t9 );
layout(binding = TEX2D_SPHERE_RANDOM) uniform sampler2D g_tex2DSphereRandomSampling   ;// register( t1 );
layout(binding = TEX3D_PREVIOUS_RADIANCE) uniform sampler3D g_tex3DPreviousSctrOrder      ;// register( t0 );
layout(binding = TEX3D_POINT_TWISE_RANDIANCE) uniform sampler3D g_tex3DPointwiseSctrRadiance  ;// register( t0 );
layout(binding = TEX2D_AVERAGE_LUMINACE) uniform sampler2D g_tex2DAverageLuminance       ;// register( t10 );
layout(binding = TEX2D_LOW_RES_LUMINACE) uniform sampler2D  g_tex2DLowResLuminance        ;// register( t0 );
layout(binding = TEX2D_SHADOW_MAP) uniform sampler2DArrayShadow  g_tex2DShadowMapArray       ;// register( t0 );

uniform float2 g_f4ShadowAttribs_Cascades_StartEndZ[4];
uniform vec4  g_f4AngularRayleighSctrCoeff;
uniform vec4  g_f4CS_g;
uniform vec4  g_f4AngularMieSctrCoeff;
uniform mat4  g_ViewInv;
uniform mat4  g_Proj;
uniform mat4  g_ViewProjInv;
uniform vec2  g_f2WQ;
uniform int   g_uiDepthSlice;

uniform float g_fFarPlaneZ;
uniform float g_fNearPlaneZ;
uniform vec4  g_f4CameraPos;

uniform float g_fMiddleGray = 0.18;
uniform float g_fWhitePoint = 3.0;
uniform float g_fLuminanceSaturation = 1.0;
uniform bool  g_bShowDepthBreaks = false;
uniform bool  g_bShowLightingOnly = false;
uniform float g_fCascadeInd = 0.0;
uniform int   g_uiInstrIntegralSteps = 30;

uniform bool  g_bIsLightOnScreen;
uniform float4 g_f4LightScreenPos;
uniform int   g_iNumCascades;

uniform int g_iFirstCascade;
uniform int g_uiNumEpipolarSlices;
uniform float2 g_f2ShadowMapTexelSize;
uniform float g_fMaxStepsAlongRay;
uniform int g_uiMinMaxShadowMapResolution;
uniform float g_fMaxShadowMapStep;
uniform float g_fRefinementThreshold;
uniform int g_uiEpipoleSamplingDensityFactor;
uniform mat4 g_WorldToShadowMapUVDepth[MAX_CASCADES];
uniform int4 g_ui4SrcDstMinMaxLevelOffset;

#if TEST_STATIC_SCENE == 1

float2 ProjToUV(in float2 f2ProjSpaceXY)
{
    return float2(0.5, 0.5) + float2(0.5, -0.5) * f2ProjSpaceXY;
}

float2 UVToProj(in float2 f2UV)
{
    return float2(-1.0, 1.0) + float2(2.0, -2.0) * f2UV;
}

#else

float2 ProjToUV(in float2 f2ProjSpaceXY)
{
    return float2(0.5, 0.5) + float2(0.5, 0.5) * f2ProjSpaceXY;
}

float2 UVToProj(in float2 f2UV)
{
    return float2(-1.0, -1.0) + float2(2.0, 2.0) * f2UV;
}

#endif

float GetCosHorizonAnlge(float fHeight)
{
    // Due to numeric precision issues, fHeight might sometimes be slightly negative
    fHeight = max(fHeight, 0.0);
    return -sqrt(fHeight * (2.0*g_fEarthRadius + fHeight) ) / (g_fEarthRadius + fHeight);
}

void GetRaySphereIntersection(in float3 f3RayOrigin,
                              in float3 f3RayDirection,
                              in float3 f3SphereCenter,
                              in float fSphereRadius,
                              out float2 f2Intersections)
{
    // http://wiki.cgsociety.org/index.php/Ray_Sphere_Intersection
    f3RayOrigin -= f3SphereCenter;
    float A = dot(f3RayDirection, f3RayDirection);
    float B = 2.0 * dot(f3RayOrigin, f3RayDirection);
    float C = dot(f3RayOrigin,f3RayOrigin) - fSphereRadius*fSphereRadius;
    float D = B*B - 4.0*A*C;
    // If discriminant is negative, there are no real roots hence the ray misses the
    // sphere
    if( D<0 )
    {
        f2Intersections = float2(-1.0);
    }
    else
    {
        D = sqrt(D);
        f2Intersections = float2(-B - D, -B + D) / (2.0*A); // A must be positive here!!
    }
}

void ApplyPhaseFunctions(inout float3 f3RayleighInscattering,
                         inout float3 f3MieInscattering,
                         in float cosTheta)
{
    f3RayleighInscattering *= g_f4AngularRayleighSctrCoeff.rgb * (1.0 + cosTheta*cosTheta);
    
    // Apply Cornette-Shanks phase function (see Nishita et al. 93):
    // F(theta) = 1/(4*PI) * 3*(1-g^2) / (2*(2+g^2)) * (1+cos^2(theta)) / (1 + g^2 - 2g*cos(theta))^(3/2)
    // f4CS_g = ( 3*(1-g^2) / (2*(2+g^2)), 1+g^2, -2g, 1 )
    float fDenom = rsqrt( dot(g_f4CS_g.yz, float2(1.f, cosTheta)) ); // 1 / (1 + g^2 - 2g*cos(theta))^(1/2)
    float fCornettePhaseFunc = g_f4CS_g.x * (fDenom*fDenom*fDenom) * (1.0 + cosTheta*cosTheta);
    f3MieInscattering *= g_f4AngularMieSctrCoeff.rgb * fCornettePhaseFunc;
}

float3 ComputeViewDir(in float fCosViewZenithAngle)
{
    return float3(sqrt(saturate(1.0 - fCosViewZenithAngle*fCosViewZenithAngle)), fCosViewZenithAngle, 0);
}

float3 ComputeLightDir(in float3 f3ViewDir, in float fCosSunZenithAngle, in float fCosSunViewAngle)
{
    float3 f3DirOnLight;
    f3DirOnLight.x = (f3ViewDir.x > 0.0) ? (fCosSunViewAngle - fCosSunZenithAngle * f3ViewDir.y) / f3ViewDir.x : 0.0;
    f3DirOnLight.y = fCosSunZenithAngle;
    f3DirOnLight.z = sqrt( saturate(1.0 - dot(f3DirOnLight.xy, f3DirOnLight.xy)) );
    // Do not normalize f3DirOnLight! Even if its length is not exactly 1 (which can 
    // happen because of fp precision issues), all the dot products will still be as 
    // specified, which is essentially important. If we normalize the vector, all the 
    // dot products will deviate, resulting in wrong pre-computation.
    // Since fCosSunViewAngle is clamped to allowable range, f3DirOnLight should always
    // be normalized. However, due to some issues on NVidia hardware sometimes
    // it may not be as that (see IMPORTANT NOTE regarding NVIDIA hardware)
    //f3DirOnLight = normalize(f3DirOnLight);
    return f3DirOnLight;
}

// When checking if a point is inside the screen, we must test against 
// the biased screen boundaries 
bool IsValidScreenLocation(in float2 f2XY)
{
    const float SAFETY_EPSILON = 0.2f;
//    return all( abs(f2XY) <= 1.f - (1.f - SAFETY_EPSILON) / SCREEN_RESLOUTION.xy );
	bvec2 result = lessThanEqual(abs(f2XY), 1.f - (1.f - SAFETY_EPSILON) / SCREEN_RESLOUTION.xy);
	return all(result);
}

float GetCamSpaceZ(in float2 ScreenSpaceUV)
{
//	  Filter = MIN_MAG_MIP_LINEAR;
//    AddressU = Clamp;
//    AddressV = Clamp;
//    return g_tex2DCamSpaceZ.SampleLevel(samLinearClamp, ScreenSpaceUV, 0);
	return textureLod(g_tex2DCamSpaceZ, ScreenSpaceUV, 0.0).x;
}

float4 GetOutermostScreenPixelCoords()
{
    // The outermost visible screen pixels centers do not lie exactly on the boundary (+1 or -1), but are biased by
    // 0.5 screen pixel size inwards
    //
    //                                        2.0
    //    |<---------------------------------------------------------------------->|
    //
    //       2.0/Res
    //    |<--------->|
    //    |     X     |      X     |     X     |    ...    |     X     |     X     |
    //   -1     |                                                            |    +1
    //          |                                                            |
    //          |                                                            |
    //      -1 + 1.0/Res                                                  +1 - 1.0/Res
    //
    // Using shader macro is much more efficient than using constant buffer variable
    // because the compiler is able to optimize the code more aggressively
    // return float4(-1,-1,1,1) + float4(1, 1, -1, -1)/g_PPAttribs.m_f2ScreenResolution.xyxy;
    return float4(-1,-1,1,1) + float4(1, 1, -1, -1) / SCREEN_RESLOUTION.xyxy;
}

// 0 dx, 1, origin, 2, projInfo

#if TEST_STATIC_SCENE == 1
#define UV_TO_VIEW 0
#else
#define UV_TO_VIEW 1
#endif

#if UV_TO_VIEW == 0
float3 ProjSpaceXYZToWorldSpace(in float3 f3PosPS)
{
    // We need to compute depth before applying view-proj inverse matrix
    float fDepth = g_Proj[2][2] + g_Proj[3][2] / f3PosPS.z;
    float4 ReconstructedPosWS = mul( float4(f3PosPS.xy,fDepth,1), g_ViewProjInv );
    ReconstructedPosWS /= ReconstructedPosWS.w;
    return ReconstructedPosWS.xyz;
}

#elif UV_TO_VIEW == 1

float3 ProjSpaceXYZToWorldSpace(in float3 f3PosPS)
{
    // We need to compute depth before applying view-proj inverse matrix
    /*
    float fDepth = g_Proj[2][2] + g_Proj[3][2] / f3PosPS.z;
    float4 ReconstructedPosWS = mul( float4(f3PosPS.xy,fDepth,1), g_ViewProjInv );
    ReconstructedPosWS /= ReconstructedPosWS.w;
    return ReconstructedPosWS.xyz;
    */
    float fDepth = -g_Proj[2][2]  + g_Proj[3][2]/ f3PosPS.z;  // f3PosPS.z = -z
    float4 projPosition = float4(f3PosPS.xy, fDepth, 1) * f3PosPS.z;
    float4 ReconstructedPosWS = g_ViewProjInv * projPosition;
    ReconstructedPosWS /= ReconstructedPosWS.w;
    return ReconstructedPosWS.xyz; 
}
	
#elif UV_TO_VIEW == 2

float3 ProjSpaceXYZToWorldSpace(in float3 f3PosPS)
{
	vec2 uv = f3PosPS.xy;
	float eye_z = f3PosPS.z;
	eye_z  = abs(eye_z);
  vec3 viewPos =  vec3((uv * g_CameraAttribs.f4ProjInfo.xy + g_CameraAttribs.f4ProjInfo.zw) * eye_z, eye_z);
//  return (g_ViewProjInv * vec4(viewPos, 1)).xyz;
  
	float4 ReconstructedPosWS = g_ViewProjInv * vec4(viewPos, 1);
    ReconstructedPosWS /= ReconstructedPosWS.w;
    return ReconstructedPosWS.xyz; 
}

#endif

float3 WorldSpaceToShadowMapUV(in float3 f3PosWS, in mat4 mWorldToShadowMapUVDepth)
{
    float4 f4ShadowMapUVDepth = mul( float4(f3PosWS, 1), mWorldToShadowMapUVDepth );
    // Shadow map projection matrix is orthographic, so we do not need to divide by w
    //f4ShadowMapUVDepth.xyz /= f4ShadowMapUVDepth.w;
    
    // Applying depth bias results in light leaking through the opaque objects when looking directly
    // at the light source
    return f4ShadowMapUVDepth.xyz;
}

float TexCoord2ZenithAngle(float fTexCoord, float fHeight, in float fTexDim, float power)
{
    float fCosZenithAngle;

    float fCosHorzAngle = GetCosHorizonAnlge(fHeight);
    if( fTexCoord > 0.5 )
    {
        // Remap to [0,1] from the upper half of the texture [0.5 + 0.5/fTexDim, 1 - 0.5/fTexDim]
        fTexCoord = saturate( (fTexCoord - (0.5f + 0.5f / fTexDim)) * fTexDim / (fTexDim/2.0 - 1.0) );
        fTexCoord = pow(fTexCoord, 1.0/power);
        // Assure that the ray does NOT hit Earth
        fCosZenithAngle = max( (fCosHorzAngle + fTexCoord * (1.0 - fCosHorzAngle)), fCosHorzAngle + 1e-4);
    }
    else
    {
        // Remap to [0,1] from the lower half of the texture [0.5, 0.5 - 0.5/fTexDim]
        fTexCoord = saturate((fTexCoord - 0.5f / fTexDim) * fTexDim / (fTexDim/2.0 - 1.0));
        fTexCoord = pow(fTexCoord, 1.0/power);
        // Assure that the ray DOES hit Earth
        fCosZenithAngle = min( (fCosHorzAngle - fTexCoord * (fCosHorzAngle - (-1))), fCosHorzAngle - 1e-4);
    }
    return fCosZenithAngle;
}