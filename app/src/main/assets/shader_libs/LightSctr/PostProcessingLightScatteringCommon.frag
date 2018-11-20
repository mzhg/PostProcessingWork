//--------------------------------------------------------------------------------------
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
//--------------------------------------------------------------------------------------

#include "../PostProcessingHLSLCompatiable.glsl"

/*
#ifndef TEST_STATIC_SCENE
#   define TEST_STATIC_SCENE  1  
#endif
*/

#define PI 3.1415928

#define ACCEL_STRUCT_NONE 0
#define ACCEL_STRUCT_MIN_MAX_TREE 1
#define ACCEL_STRUCT_BV_TREE 2

#define LIGHT_TYPE_DIRECTIONAL 0
#define LIGHT_TYPE_SPOT 1
#define LIGHT_TYPE_POINT 2


#define INSCTR_INTGL_EVAL_METHOD_MY_LUT 0
#define INSCTR_INTGL_EVAL_METHOD_SRNN05 1
#define INSCTR_INTGL_EVAL_METHOD_ANALYTIC 2

#define LIGHT_SCTR_TECHNIQUE_EPIPOLAR_SAMPLING 0
#define LIGHT_SCTR_TECHNIQUE_BRUTE_FORCE 1

#define FLT_MAX 3.402823466e+38f

//Using static definitions instead of constant buffer variables is 
//more efficient because the compiler is able to optimize the code 
//more aggressively

#ifndef NUM_EPIPOLAR_SLICES
#   define NUM_EPIPOLAR_SLICES 1024
#endif

#ifndef MAX_SAMPLES_IN_SLICE
#   define MAX_SAMPLES_IN_SLICE 512
#endif

#ifndef SCREEN_RESLOUTION
#   define SCREEN_RESLOUTION float2(1024,768)
#endif

#ifndef ACCEL_STRUCT
#   define ACCEL_STRUCT ACCEL_STRUCT_BV_TREE
#endif

#if ACCEL_STRUCT == ACCEL_STRUCT_BV_TREE
#   define MIN_MAX_DATA_FORMAT float4
#elif ACCEL_STRUCT == ACCEL_STRUCT_MIN_MAX_TREE
#   define MIN_MAX_DATA_FORMAT float2
#else
#   define MIN_MAX_DATA_FORMAT float2
#endif

#ifndef INSCTR_INTGL_EVAL_METHOD
#   define INSCTR_INTGL_EVAL_METHOD INSCTR_INTGL_EVAL_METHOD_MY_LUT
#endif

#if INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_MY_LUT
#   define INSCTR_LUT_FORMAT float3
#elif INSCTR_INTGL_EVAL_METHOD == INSCTR_INTGL_EVAL_METHOD_SRNN05
#   define INSCTR_LUT_FORMAT float
#endif
#ifndef INSCTR_LUT_FORMAT
#   define INSCTR_LUT_FORMAT float
#endif

#ifndef STAINED_GLASS
#   define STAINED_GLASS 1
#endif

#ifndef OPTIMIZE_SAMPLE_LOCATIONS
#   define OPTIMIZE_SAMPLE_LOCATIONS 1
#endif

#ifndef LIGHT_TYPE
#   define LIGHT_TYPE LIGHT_TYPE_POINT
#endif

#ifndef ANISOTROPIC_PHASE_FUNCTION
#   define ANISOTROPIC_PHASE_FUNCTION 1
#endif

#define SHADOW_MAP_DEPTH_BIAS 1e-4

const float4 g_f4IncorrectSliceUVDirAndStart = float4(-10000, -10000, 0, 0);

#define TEX2D_DEPTH_BUFFER  0
#define TEX2D_CAM_SPACEZ    1
#define TEX2D_SLICE_END_POINTS 2
#define TEX2D_COORDINATES   3
#define TEX2D_EPIPOLAR_CAM  4
#define TEX2D_INTERP_SOURCE 5
#define TEX2D_SHADOWM_MAP   6
#define TEX2D_SLICE_UV_ORIGIN 7
#define TEX2D_MIN_MAX_DEPTH 8
#define TEX2D_STAINED_DEPTH 9
#define TEX2D_INIT_INSCTR   10
#define TEX2D_COLOR_BUFFER  11
#define TEX2D_SCATTERED_COLOR 12
#define TEX2D_DOWNSCALED_INSCTR 13
#define TEX2D_PRECOMPUTED_INSCTR 14
#define TEX2D_SHADOWM_BUFFER 15

layout(binding = TEX2D_DEPTH_BUFFER) uniform sampler2D  g_tex2DDepthBuffer            /*: register( t0 )*/;
layout(binding = TEX2D_CAM_SPACEZ) uniform sampler2D  g_tex2DCamSpaceZ              /*: register( t0 )*/;
layout(binding = TEX2D_SLICE_END_POINTS) uniform sampler2D g_tex2DSliceEndPoints         /*: register( t4 )*/;
layout(binding = TEX2D_COORDINATES) uniform sampler2D g_tex2DCoordinates            /*: register( t1 )*/;
layout(binding = TEX2D_EPIPOLAR_CAM) uniform sampler2D  g_tex2DEpipolarCamSpaceZ      /*: register( t2 )*/;
layout(binding = TEX2D_INTERP_SOURCE) uniform usampler2D  g_tex2DInterpolationSource    /*: register( t7 )*/;
layout(binding = TEX2D_SHADOWM_MAP) uniform sampler2DShadow  g_tex2DLightSpaceDepthMap     /*: register( t3 )*/;
layout(binding = TEX2D_SLICE_UV_ORIGIN) uniform sampler2D g_tex2DSliceUVDirAndOrigin    /*: register( t2 )*/;
layout(binding = TEX2D_MIN_MAX_DEPTH) uniform sampler2D g_tex2DMinMaxLightSpaceDepth  /*: register( t4 )*/;
layout(binding = TEX2D_STAINED_DEPTH) uniform sampler2D g_tex2DStainedGlassColorDepth /*: register( t5 )*/;
layout(binding = TEX2D_INIT_INSCTR) uniform sampler2D g_tex2DInitialInsctrIrradiance /*: register( t6 )*/;
layout(binding = TEX2D_COLOR_BUFFER) uniform  sampler2D g_tex2DColorBuffer            /*: register( t1 )*/;
layout(binding = TEX2D_SCATTERED_COLOR) uniform sampler2D g_tex2DScatteredColor         /*: register( t3 )*/;
layout(binding = TEX2D_DOWNSCALED_INSCTR) uniform sampler2D g_tex2DDownscaledInsctrRadiance/*: register( t2 )*/;
layout(binding = TEX2D_PRECOMPUTED_INSCTR) uniform sampler2D g_tex2DPrecomputedPointLightInsctr/*: register( t6 )*/;
layout(binding = TEX2D_SHADOWM_BUFFER) uniform sampler2D  g_tex2DLightSpaceDepthBuffer;

uniform float4 g_f4AngularRayleighBeta;
uniform float4 g_f4AngularMieBeta;
uniform float4 g_f4HG_g;
uniform float4 g_f4TotalRayleighBeta;
uniform float4 g_f4TotalMieBeta;
uniform bool   g_bShowLightingOnly;

uniform float4 g_f4LightColorAndIntensity;
uniform mat4   g_WorldToLightProjSpace;
uniform bool   g_bIsLightOnScreen;
uniform float4 g_f4LightScreenPos;
uniform float4 g_f4DirOnLight;
uniform float4 g_f4LightWorldPos;
uniform float4 g_f4SpotLightAxisAndCosAngle;
uniform float4 g_f4CameraUVAndDepthInShadowMap;

uniform mat4 g_Proj;
uniform mat4 g_ViewProjInv;
uniform float g_fFarPlaneZ;
uniform float g_fNearPlaneZ;
uniform float4 g_f4CameraPos;

uniform float g_fExposure;
uniform float2 g_f2ShadowMapTexelSize;
uniform ivec4 g_ui4SrcDstMinMaxLevelOffset;
uniform float g_fRefinementThreshold;
uniform bool  g_bCorrectScatteringAtDepthBreaks;
uniform bool  g_bShowDepthBreaks = false;
uniform float g_fMaxTracingDistance;
uniform float4 g_f4SummTotalBeta;
uniform int m_uiEpipoleSamplingDensityFactor;
uniform float g_fMaxStepsAlongRay;
uniform int g_uiMinMaxShadowMapResolution;
uniform int g_uiMaxShadowMapStep;

float3 ApplyPhaseFunction(in float3 f3InsctrIntegral, in float cosTheta)
{
    //    sun
    //      \
    //       \
    //    ----\------eye
    //         \theta 
    //          \
    //    
    
    // Compute Rayleigh scattering Phase Function
    // According to formula for the Rayleigh Scattering phase function presented in the 
    // "Rendering Outdoor Light Scattering in Real Time" by Hoffman and Preetham (see p.36 and p.51), 
    // BethaR(Theta) is calculated as follows:
    // 3/(16PI) * BethaR * (1+cos^2(theta))
    // g_f4AngularRayleighBeta == (3*PI/16) * g_MediaParams.f4TotalRayleighBeta, hence:
    float3 RayleighScatteringPhaseFunc = g_f4AngularRayleighBeta.rgb * (1.0 + cosTheta*cosTheta);

    // Compute Henyey-Greenstein approximation of the Mie scattering Phase Function
    // According to formula for the Mie Scattering phase function presented in the 
    // "Rendering Outdoor Light Scattering in Real Time" by Hoffman and Preetham 
    // (see p.38 and p.51),  BethaR(Theta) is calculated as follows:
    // 1/(4PI) * BethaM * (1-g^2)/(1+g^2-2g*cos(theta))^(3/2)
    // const float4 g_MediaParams.f4HG_g = float4(1 - g*g, 1 + g*g, -2*g, 1);
    float HGTemp = rsqrt( dot(g_f4HG_g.yz, float2(1.f, cosTheta)) );//rsqrt( g_MediaParams.f4HG_g.y + g_MediaParams.f4HG_g.z*cosTheta);
    // g_MediaParams.f4AngularMieBeta is calculated according to formula presented in "A practical Analytic 
    // Model for Daylight" by Preetham & Hoffman (see p.23)
    float3 fMieScatteringPhaseFunc_HGApprox = g_f4AngularMieBeta.rgb * g_f4HG_g.x * (HGTemp*HGTemp*HGTemp);

    float3 f3InscatteredLight = f3InsctrIntegral * 
                               (RayleighScatteringPhaseFunc + fMieScatteringPhaseFunc_HGApprox);

    f3InscatteredLight.rgb *= g_f4LightColorAndIntensity.w;  
    
    return f3InscatteredLight;
}

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
    float4 projPosition = float4(f3PosPS.xy, fDepth, 1) /** f3PosPS.z*/;
    float4 ReconstructedPosWS = g_ViewProjInv * projPosition;
    ReconstructedPosWS /= ReconstructedPosWS.w;
    return ReconstructedPosWS.xyz;
}

float Sign(float v)
{
	if(v > 0.0) return 1.0;
	else if(v < 0.0) return -1.0;
	else return 0.0;
}

float2 RayConeIntersect(in float3 f3ConeApex, in float3 f3ConeAxis, in float fCosAngle, float3 f3RayStart, in float3 f3RayDir)
{
    f3RayStart -= f3ConeApex;
    float a = dot(f3RayDir, f3ConeAxis);
    float b = dot(f3RayDir, f3RayDir);
    float c = dot(f3RayStart, f3ConeAxis);
    float d = dot(f3RayStart, f3RayDir);
    float e = dot(f3RayStart, f3RayStart);
    fCosAngle *= fCosAngle;
    float A = a*a - b*fCosAngle;
    float B = 2 * ( c*a - d*fCosAngle );
    float C = c*c - e*fCosAngle;
    float D = B*B - 4*A*C;
    
//    return float2(D, 0);
    if( D > 0.0 )
    {
        D = sqrt(D);
        float2 t = (-B + Sign(A)*float2(-D,+D)) / (2*A);
//        bool2 b2IsCorrect = greaterThan(c + a * t, float2(0));
		float2 b2IsCorrect;
		b2IsCorrect.x = (c + a * t.x) > 0.0 ? 1.0 : 0.0;
		b2IsCorrect.y = (c + a * t.y) > 0.0 ? 1.0 : 0.0;
		
//		return b2IsCorrect;
//        t = t * b2IsCorrect + (1.0 - b2IsCorrect) * (-FLT_MAX);
		t.x = (b2IsCorrect.x > 0.0) ? t.x :(-FLT_MAX);
		t.y = (b2IsCorrect.y > 0.0) ? t.y :(-FLT_MAX);
        return t;
    }
    else
        return float2(-FLT_MAX);
}

bool PlanePlaneIntersect(float3 f3N1, float3 f3P1, float3 f3N2, float3 f3P2,
                         out float3 f3LineOrigin, out float3 f3LineDir)
{
    // http://paulbourke.net/geometry/planeplane/
    float fd1 = dot(f3N1, f3P1);
    float fd2 = dot(f3N2, f3P2);
    float fN1N1 = dot(f3N1, f3N1);
    float fN2N2 = dot(f3N2, f3N2);
    float fN1N2 = dot(f3N1, f3N2);

    float fDet = fN1N1 * fN2N2 - fN1N2*fN1N2;
    if( abs(fDet) < 1e-6 )
        return false;

    float fc1 = (fd1 * fN2N2 - fd2 * fN1N2) / fDet;
    float fc2 = (fd2 * fN1N1 - fd1 * fN1N2) / fDet;

    f3LineOrigin = fc1 * f3N1 + fc2 * f3N2;
    f3LineDir = normalize(cross(f3N1, f3N2));
    
    return true;
}

float2 ProjToUV(in float2 f2ProjSpaceXY)
{
    return 0.5 + 0.5 * f2ProjSpaceXY;
}

float2 UVToProj(in float2 f2UV)
{
    return -1.0 + 2.0 * f2UV;
}

float GetCamSpaceZ(in float2 ScreenSpaceUV)
{
//	  Filter = MIN_MAG_MIP_LINEAR;
//    AddressU = Clamp;
//    AddressV = Clamp;
//    return g_tex2DCamSpaceZ.SampleLevel(samLinearClamp, ScreenSpaceUV, 0);
	return textureLod(g_tex2DCamSpaceZ, ScreenSpaceUV, 0.0).x;
}

float3 ToneMap(in float3 f3Color)
{
    float fExposure = g_fExposure;
    return 1.0 - exp(-fExposure * f3Color);
}

float3 ProjSpaceXYToWorldSpace(in float2 f2PosPS)
{
    // We can sample camera space z texture using bilinear filtering
//    float fCamSpaceZ = g_tex2DCamSpaceZ.SampleLevel(samLinearClamp, ProjToUV(f2PosPS), 0);
	float fCamSpaceZ = textureLod(g_tex2DCamSpaceZ, ProjToUV(f2PosPS), 0.0).x;
    return ProjSpaceXYZToWorldSpace(float3(f2PosPS, fCamSpaceZ));
}

float4 WorldSpaceToShadowMapUV(in float3 f3PosWS)
{
    float4 f4LightProjSpacePos = mul( float4(f3PosWS, 1), g_WorldToLightProjSpace );
    f4LightProjSpacePos.xyz /= f4LightProjSpacePos.w;
    float4 f4UVAndDepthInLightSpace;
    f4UVAndDepthInLightSpace.xy = ProjToUV( f4LightProjSpacePos.xy );
    // Applying depth bias results in light leaking through the opaque objects when looking directly
    // at the light source
#if TEST_STATIC_SCENE == 1 
    f4UVAndDepthInLightSpace.z = f4LightProjSpacePos.z;
#else
	f4UVAndDepthInLightSpace.z = 0.5 * f4LightProjSpacePos.z + 0.5;// * g_DepthBiasMultiplier;
#endif
    f4UVAndDepthInLightSpace.w = 1/f4LightProjSpacePos.w;
    return f4UVAndDepthInLightSpace;
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

#if __FRAG_SHADER__ == 1
float3 PerformBilateralInterpolation(in float2 f2BilinearWeights,
        in float2 f2LeftBottomSrcTexelUV,
        in float4 f4SrcLocationsCamSpaceZ,
        in float  fFilteringLocationCamSpaceZ,
        sampler2D tex2DSrcTexture,
        in float2 f2SrcTexDim/*,
        in SamplerState Sampler*/)
{
	// Initialize bilateral weights with bilinear:
	float4 f4BilateralWeights = 
	//Offset:       (x=0,y=1)            (x=1,y=1)             (x=1,y=0)               (x=0,y=0)
	float4(1 - f2BilinearWeights.x, f2BilinearWeights.x,   f2BilinearWeights.x, 1 - f2BilinearWeights.x) * 
	float4(    f2BilinearWeights.y, f2BilinearWeights.y, 1-f2BilinearWeights.y, 1 - f2BilinearWeights.y);
	
	// Compute depth weights in a way that if the difference is less than the threshold, the weight is 1 and
	// the weights fade out to 0 as the difference becomes larger than the threshold:
	float4 f4DepthWeights = saturate( g_fRefinementThreshold / max( abs(fFilteringLocationCamSpaceZ-f4SrcLocationsCamSpaceZ), g_fRefinementThreshold ) );
	// Note that if the sample is located outside the [-1,1]x[-1,1] area, the sample is invalid and fCurrCamSpaceZ == fInvalidCoordinate
	// Depth weight computed for such sample will be zero
	f4DepthWeights = pow(f4DepthWeights, float4(4));
	// Multiply bilinear weights with the depth weights:
	f4BilateralWeights *= f4DepthWeights;
	// Compute summ weight
	float fTotalWeight = dot(f4BilateralWeights, float4(1,1,1,1));
	
	float3 f3ScatteredLight = float3(0);
//	[branch]
	if( g_bCorrectScatteringAtDepthBreaks && fTotalWeight < 1e-2 )
	{
		// Discarded pixels will keep 0 value in stencil and will be later
		// processed to correct scattering
		discard;
	}
	else
	{
		// Normalize weights
		f4BilateralWeights /= fTotalWeight;
		
		// We now need to compute the following weighted summ:
		//f3ScatteredLight = 
		//    f4BilateralWeights.x * tex2DSrcTexture.SampleLevel(samPoint, f2ScatteredColorIJ, 0, int2(0,1)) +
		//    f4BilateralWeights.y * tex2DSrcTexture.SampleLevel(samPoint, f2ScatteredColorIJ, 0, int2(1,1)) +
		//    f4BilateralWeights.z * tex2DSrcTexture.SampleLevel(samPoint, f2ScatteredColorIJ, 0, int2(1,0)) +
		//    f4BilateralWeights.w * tex2DSrcTexture.SampleLevel(samPoint, f2ScatteredColorIJ, 0, int2(0,0));
		
		// We will use hardware to perform bilinear filtering and get these values using just two bilinear fetches:
		
		// Offset:                  (x=1,y=0)                (x=1,y=0)               (x=0,y=0)
		float fRow0UOffset = f4BilateralWeights.z / max(f4BilateralWeights.z + f4BilateralWeights.w, 0.001);
		fRow0UOffset /= f2SrcTexDim.x;
		float3 f3Row0WeightedCol = 
		(f4BilateralWeights.z + f4BilateralWeights.w) * 
//		tex2DSrcTexture.SampleLevel(Sampler, f2LeftBottomSrcTexelUV + float2(fRow0UOffset, 0), 0, int2(0,0));
		textureLod(tex2DSrcTexture, f2LeftBottomSrcTexelUV + float2(fRow0UOffset, 0), 0.0).rgb;
		
		// Offset:                  (x=1,y=1)                 (x=0,y=1)              (x=1,y=1)
		float fRow1UOffset = f4BilateralWeights.y / max(f4BilateralWeights.x + f4BilateralWeights.y, 0.001);
		fRow1UOffset /= f2SrcTexDim.x;
		float3 f3Row1WeightedCol = 
			(f4BilateralWeights.x + f4BilateralWeights.y) * 
//		tex2DSrcTexture.SampleLevel(Sampler, f2LeftBottomSrcTexelUV + float2(fRow1UOffset, 0 ), 0, int2(0,1));
				textureLodOffset(tex2DSrcTexture, f2LeftBottomSrcTexelUV + float2(fRow1UOffset, 0 ), 0.0, int2(0, 1)).rgb;
		
		f3ScatteredLight = f3Row0WeightedCol + f3Row1WeightedCol;
	}
	
	return f3ScatteredLight;
}

float3 UnwarpEpipolarInsctrImage(float2 m_f2PosPS, in float fCamSpaceZ )
{
	// Compute direction of the ray going from the light through the pixel
	float2 f2RayDir = normalize( m_f2PosPS - g_f4LightScreenPos.xy );
	
	// Find, which boundary the ray intersects. For this, we will 
	// find which two of four half spaces the f2RayDir belongs to
	// Each of four half spaces is produced by the line connecting one of four
	// screen corners and the current pixel:
	//    ________________        _______'________           ________________           
	//   |'            . '|      |      '         |         |                |          
	//   | '       . '    |      |     '          |      .  |                |          
	//   |  '  . '        |      |    '           |        '|.        hs1    |          
	//   |   *.           |      |   *     hs0    |         |  '*.           |          
	//   |  '   ' .       |      |  '             |         |      ' .       |          
	//   | '        ' .   |      | '              |         |          ' .   |          
	//   |'____________ '_|      |'_______________|         | ____________ '_.          
	//                           '                                             '
	//                           ________________  .        '________________  
	//                           |             . '|         |'               | 
	//                           |   hs2   . '    |         | '              | 
	//                           |     . '        |         |  '             | 
	//                           | . *            |         |   *            | 
	//                         . '                |         |    '           | 
	//                           |                |         | hs3 '          | 
	//                           |________________|         |______'_________| 
	//                                                              '
	// The equations for the half spaces are the following:
	//bool hs0 = (In.m_f2PosPS.x - (-1)) * f2RayDir.y < f2RayDir.x * (In.m_f2PosPS.y - (-1));
	//bool hs1 = (In.m_f2PosPS.x -  (1)) * f2RayDir.y < f2RayDir.x * (In.m_f2PosPS.y - (-1));
	//bool hs2 = (In.m_f2PosPS.x -  (1)) * f2RayDir.y < f2RayDir.x * (In.m_f2PosPS.y -  (1));
	//bool hs3 = (In.m_f2PosPS.x - (-1)) * f2RayDir.y < f2RayDir.x * (In.m_f2PosPS.y -  (1));
	// Note that in fact the outermost visible screen pixels do not lie exactly on the boundary (+1 or -1), but are biased by
	// 0.5 screen pixel size inwards. Using these adjusted boundaries improves precision and results in
	// smaller number of pixels which require inscattering correction
	float4 f4Boundaries = GetOutermostScreenPixelCoords();//left, bottom, right, top
	float4 f4HalfSpaceEquationTerms = (m_f2PosPS.xxyy - f4Boundaries.xzyw/*float4(-1,1,-1,1)*/) * f2RayDir.yyxx;
	bool4 b4HalfSpaceFlags = lessThan(f4HalfSpaceEquationTerms.xyyx, f4HalfSpaceEquationTerms.zzww);
	
	// Now compute mask indicating which of four sectors the f2RayDir belongs to and consiquently
	// which border the ray intersects:
	//    ________________ 
	//   |'            . '|         0 : hs3 && !hs0
	//   | '   3   . '    |         1 : hs0 && !hs1
	//   |  '  . '        |         2 : hs1 && !hs2
	//   |0  *.       2   |         3 : hs2 && !hs3
	//   |  '   ' .       |
	//   | '   1    ' .   |
	//   |'____________ '_|
	//
	bool4 b4SectorFlags = b4HalfSpaceFlags.wxyz && !b4HalfSpaceFlags.xyzw;
	// Note that b4SectorFlags now contains true (1) for the exit boundary and false (0) for 3 other
	
	// Compute distances to boundaries according to following lines:
	//float fDistToLeftBoundary   = abs(f2RayDir.x) > 1e-5 ? ( -1 - g_LightAttribs.f4LightScreenPos.x) / f2RayDir.x : -FLT_MAX;
	//float fDistToBottomBoundary = abs(f2RayDir.y) > 1e-5 ? ( -1 - g_LightAttribs.f4LightScreenPos.y) / f2RayDir.y : -FLT_MAX;
	//float fDistToRightBoundary  = abs(f2RayDir.x) > 1e-5 ? (  1 - g_LightAttribs.f4LightScreenPos.x) / f2RayDir.x : -FLT_MAX;
	//float fDistToTopBoundary    = abs(f2RayDir.y) > 1e-5 ? (  1 - g_LightAttribs.f4LightScreenPos.y) / f2RayDir.y : -FLT_MAX;
	float4 f4DistToBoundaries = ( f4Boundaries - g_f4LightScreenPos.xyxy ) / (f2RayDir.xyxy + float4( lessThan(abs(f2RayDir.xyxy), float4(1e-6)) ) );
	// Select distance to the exit boundary:
	float fDistToExitBoundary = dot( float4(b4SectorFlags), f4DistToBoundaries );
	// Compute exit point on the boundary:
	float2 f2ExitPoint = g_f4LightScreenPos.xy + f2RayDir * fDistToExitBoundary;
	
	// Compute epipolar slice for each boundary:
	//if( LeftBoundary )
	//    fEpipolarSlice = 0.0  - (LeftBoudaryIntersecPoint.y   -   1 )/2 /4;
	//else if( BottomBoundary )
	//    fEpipolarSlice = 0.25 + (BottomBoudaryIntersecPoint.x - (-1))/2 /4;
	//else if( RightBoundary )
	//    fEpipolarSlice = 0.5  + (RightBoudaryIntersecPoint.y  - (-1))/2 /4;
	//else if( TopBoundary )
	//    fEpipolarSlice = 0.75 - (TopBoudaryIntersecPoint.x      - 1 )/2 /4;
	float4 f4EpipolarSlice = float4(0, 0.25, 0.5, 0.75) + 
	saturate( (f2ExitPoint.yxyx - f4Boundaries.wxyz)*float4(-1, +1, +1, -1) / (f4Boundaries.wzwz - f4Boundaries.yxyx) ) / 4.0;
	// Select the right value:
	float fEpipolarSlice = dot(float4(b4SectorFlags), f4EpipolarSlice);
	
	// Load epipolar endpoints. Note that slice 0 is stored in the first
	// texel which has U coordinate shifted by 0.5 texel size
	// (search for "fEpipolarSlice = saturate(f2UV.x - 0.5f / (float)NUM_EPIPOLAR_SLICES)"):
	fEpipolarSlice = saturate(fEpipolarSlice + 0.5f/float(NUM_EPIPOLAR_SLICES));
	// Note also that this offset dramatically reduces the number of samples, for which correction pass is
	// required (the correction pass becomes more than 2x times faster!!!)
	float4 f4SliceEndpoints = //g_tex2DSliceEndPoints.SampleLevel( samLinearClamp, float2(fEpipolarSlice, 0.5), 0 );
								textureLod(g_tex2DSliceEndPoints, float2(fEpipolarSlice, 0.5), 0.0);
	f2ExitPoint = f4SliceEndpoints.zw;
	float2 f2EntryPoint = f4SliceEndpoints.xy;
	
	
	float2 f2EpipolarSliceDir = f2ExitPoint - f2EntryPoint;
	float fEpipolarSliceLen = length(f2EpipolarSliceDir);
	f2EpipolarSliceDir /= max(fEpipolarSliceLen, 1e-6);
	
	// Project current pixel onto the epipolar slice
	float fSamplePosOnEpipolarLine = dot((m_f2PosPS - f2EntryPoint.xy), f2EpipolarSliceDir) / fEpipolarSliceLen;
	// Rescale the sample position
	// Note that the first sample on slice is exactly the f2EntryPoint.xy, while the last sample is exactly the f2ExitPoint
	// (search for "fSamplePosOnEpipolarLine *= (float)MAX_SAMPLES_IN_SLICE / ((float)MAX_SAMPLES_IN_SLICE-1.f)")
	// As usual, we also need to add offset by 0.5 texel size
	float fScatteredColorU = fSamplePosOnEpipolarLine * float(MAX_SAMPLES_IN_SLICE-1) / float(MAX_SAMPLES_IN_SLICE) + 0.5f/float(MAX_SAMPLES_IN_SLICE);
	
	// We need to manually perform bilateral filtering of the scattered radiance texture to
	// eliminate artifacts at depth discontinuities
	float2 f2ScatteredColorUV = float2(fScatteredColorU, fEpipolarSlice);
	float2 f2ScatteredColorTexDim;
//	g_tex2DScatteredColor.GetDimensions(f2ScatteredColorTexDim.x, f2ScatteredColorTexDim.y);
	f2ScatteredColorTexDim = float2(textureSize(g_tex2DScatteredColor, 0));
	// Offset by 0.5 is essential, because texel centers have UV coordinates that are offset by half the texel size
	float2 f2ScatteredColorUVScaled = f2ScatteredColorUV.xy * f2ScatteredColorTexDim.xy - float2(0.5, 0.5);
	float2 f2ScatteredColorIJ = floor(f2ScatteredColorUVScaled);
	// Get bilinear filtering weights
	float2 f2BilinearWeights = f2ScatteredColorUVScaled - f2ScatteredColorIJ;
	// Get texture coordinates of the left bottom source texel. Again, offset by 0.5 is essential
	// to align with texel center
	f2ScatteredColorIJ = (f2ScatteredColorIJ + float2(0.5, 0.5)) / f2ScatteredColorTexDim.xy;
	
	// Gather 4 camera space z values
	// Note that we need to bias f2ScatteredColorIJ by 0.5 texel size to get the required values
	//   _______ _______
	//  |       |       |
	//  |       |       |
	//  |_______X_______|  X gather location
	//  |       |       |
	//  |   *   |       |  * f2ScatteredColorIJ
	//  |_______|_______|
	//  |<----->|
	//     1/f2ScatteredColorTexDim.x
	float4 f4SrcLocationsCamSpaceZ = //g_tex2DEpipolarCamSpaceZ.Gather(samLinearClamp, f2ScatteredColorIJ + float2(0.5, 0.5) / f2ScatteredColorTexDim.xy);
					textureGather(g_tex2DEpipolarCamSpaceZ, f2ScatteredColorIJ + float2(0.5, 0.5) / f2ScatteredColorTexDim.xy);
	// The values in f4SrcLocationsCamSpaceZ are arranged as follows:
	// f4SrcLocationsCamSpaceZ.x == g_tex2DEpipolarCamSpaceZ.SampleLevel(samPointClamp, f2ScatteredColorIJ, 0, int2(0,1))
	// f4SrcLocationsCamSpaceZ.y == g_tex2DEpipolarCamSpaceZ.SampleLevel(samPointClamp, f2ScatteredColorIJ, 0, int2(1,1))
	// f4SrcLocationsCamSpaceZ.z == g_tex2DEpipolarCamSpaceZ.SampleLevel(samPointClamp, f2ScatteredColorIJ, 0, int2(1,0))
	// f4SrcLocationsCamSpaceZ.w == g_tex2DEpipolarCamSpaceZ.SampleLevel(samPointClamp, f2ScatteredColorIJ, 0, int2(0,0))
	
	return PerformBilateralInterpolation(f2BilinearWeights, f2ScatteredColorIJ, f4SrcLocationsCamSpaceZ, fCamSpaceZ, g_tex2DScatteredColor, f2ScatteredColorTexDim/*, samLinearClamp,  Do not use wrap mode for epipolar slice! */);
}

#endif

float3 GetExtinction(float in_Dist)
{
    float3 vExtinction;
    // Use analytical expression for extinction (see "Rendering Outdoor Light Scattering in Real Time" by 
    // Hoffman and Preetham, p.27 and p.51) 
    vExtinction = exp( -(g_f4TotalRayleighBeta.rgb +  g_f4TotalMieBeta.rgb) * in_Dist );
    return vExtinction;
}

// f2Pos == gl_FragCoord.xy
float3 GetAttenuatedBackgroundColor(in float2 f2Pos, in float fDistToCamera )
{
    float3 f3BackgroundColor = float3(0);
//    [branch]
    if( !g_bShowLightingOnly )
    {
        f3BackgroundColor = // g_tex2DColorBuffer.Load(int3(f2Pos,0)).rgb;
        					texelFetch(g_tex2DColorBuffer, int2(f2Pos), 0).rgb;
        float3 f3Extinction = GetExtinction(fDistToCamera);
        f3BackgroundColor *= f3Extinction.rgb;
    }
    return f3BackgroundColor;
}

// f2Pos == gl_Position.xy
float3 GetAttenuatedBackgroundColor(in float2 projPos, in float2 fragCoord)
{
    float3 f3WorldSpacePos = ProjSpaceXYToWorldSpace(projPos);
    float fDistToCamera = length(f3WorldSpacePos - g_f4CameraPos.xyz);
    return GetAttenuatedBackgroundColor(fragCoord, fDistToCamera);
}

float3 EvaluatePhaseFunction(float fCosTheta)
{
#if ANISOTROPIC_PHASE_FUNCTION
    float3 f3RlghInsctr =  g_f4AngularRayleighBeta.rgb * (1.0 + fCosTheta*fCosTheta);
    float HGTemp = rsqrt( dot(g_f4HG_g.yz, float2(1.f, fCosTheta)) );
    float3 f3MieInsctr = g_f4AngularMieBeta.rgb * g_f4HG_g.x * (HGTemp*HGTemp*HGTemp);
#else
    float3 f3RlghInsctr = g_f4TotalRayleighBeta.rgb / (4.0*PI);
    float3 f3MieInsctr = g_f4TotalMieBeta.rgb / (4.0*PI);
#endif

    return f3RlghInsctr + f3MieInsctr;
}

float2 GetSRNN05LUTParamLimits()
{
    // The first argument of the lookup table is the distance from the point light source to the view ray, multiplied by the scattering coefficient
    // The second argument is the weird angle which is in the range from 0 t Pi/2, as tan(Pi/2) = +inf
    return float2(
        g_fMaxTracingDistance * 2.0 * max(max(g_f4SummTotalBeta.r, g_f4SummTotalBeta.g), g_f4SummTotalBeta.b),
        PI * 0.5 );
}