// This code contains NVIDIA Confidential Information and is disclosed 
// under the Mutual Non-Disclosure Agreement. 
// 
// Notice 
// ALL NVIDIA DESIGN SPECIFICATIONS AND CODE ("MATERIALS") ARE PROVIDED "AS IS" NVIDIA MAKES 
// NO REPRESENTATIONS, WARRANTIES, EXPRESSED, IMPLIED, STATUTORY, OR OTHERWISE WITH RESPECT TO 
// THE MATERIALS, AND EXPRESSLY DISCLAIMS ANY IMPLIED WARRANTIES OF NONINFRINGEMENT, 
// MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE. 
// 
// NVIDIA Corporation assumes no responsibility for the consequences of use of such 
// information or for any infringement of patents or other rights of third parties that may 
// result from its use. No license is granted by implication or otherwise under any patent 
// or patent rights of NVIDIA Corporation. No third party distribution is allowed unless 
// expressly authorized by NVIDIA.  Details are subject to change without notice. 
// This code supersedes and replaces all information previously supplied. 
// NVIDIA Corporation products are not authorized for use as critical 
// components in life support devices or systems without express written approval of 
// NVIDIA Corporation. 
// 
// Copyright (c) 2003 - 2016 NVIDIA Corporation. All rights reserved.
//
// NVIDIA Corporation and its licensors retain all intellectual property and proprietary
// rights in and to this software and related documentation and any modifications thereto.
// Any use, reproduction, disclosure or distribution of this software and related
// documentation without an express license agreement from NVIDIA Corporation is
// strictly prohibited.
//

/*
%% MUX_BEGIN %%
# Define the shader permutations for code generation

# Are we operating on single sample or MSAA buffer
- SAMPLEMODE:
    - SAMPLEMODE_SINGLE
    - SAMPLEMODE_MSAA

# What type of light are we rendering
- LIGHTMODE:
    - LIGHTMODE_DIRECTIONAL
    - LIGHTMODE_SPOTLIGHT
    - LIGHTMODE_OMNI

# What sort of pass are we rendering
- PASSMODE:
    - PASSMODE_GEOMETRY
    - PASSMODE_SKY
    - PASSMODE_FINAL

# What is our distance attenuation function
- ATTENUATIONMODE:
    - ATTENUATIONMODE_NONE
    - ATTENUATIONMODE_POLYNOMIAL
    - ATTENUATIONMODE_INV_POLYNOMIAL

# What is our spotlight angular falloff mode
- FALLOFFMODE:
    - FALLOFFMODE_NONE
    - FALLOFFMODE_FIXED
    - FALLOFFMODE_CUSTOM

%% MUX_END %%
*/

#include "ShaderCommon.frag"

#if (PASSMODE == PASSMODE_FINAL)
#   if (SAMPLEMODE == SAMPLEMODE_SINGLE)

//        Texture2D<float> tSceneDepth : register(t2);
		uniform sampler2D tSceneDepth;
        float LoadSceneDepth(int2 pos, int s)
        {
 //           return tSceneDepth.Load(int3(pos.xy, 0)).x;
 			return texelFetch(tSceneDepth, pos, 0).x;
        }

#   elif (SAMPLEMODE == SAMPLEMODE_MSAA)

//        Texture2DMS<float> tSceneDepth : register(t2);
		uniform sampler2DMS tSceneDepth;
        float LoadSceneDepth(int2 pos, int s)
        {
//            return tSceneDepth.Load(int2(pos.xy), s).x;
			return texelFetch(tSceneDepth, pos, s).x;
        }

#   endif
#else

    float LoadSceneDepth(int2 pos, int s)
    {
        return 1.0;
    }

#endif

//Texture2D<float4> tPhaseLUT : register(t4);
//Texture2D<float4> tLightLUT_P : register(t5);
//Texture2D<float4> tLightLUT_S1 : register(t6);
//Texture2D<float4> tLightLUT_S2 : register(t7);
uniform sampler2D tPhaseLUT;
uniform sampler2D tLightLUT_P;
uniform sampler2D tLightLUT_S1;
uniform sampler2D tLightLUT_S2;

float GetLutCoord_X(float t, float light_dist)
{
    float t0 = max(0.0, light_dist-g_fLightZFar);
    float t_range = g_fLightZFar + light_dist - t0;
    return (t-t0) / t_range;
}

float GetLutCoord_Y(float cos_theta)
{
    return acos(-cos_theta) / PI;
}

vec3 SampleLut(sampler2D tex, vec2 tc)
{
//    float4 s = tex.SampleLevel(sBilinear, tc, 0);
	vec4 s = textureLod(tex, tc, 0.0);
    return s.rgb*s.a;
}

////////////////////////////////////////////////////////////////////////////////
// Integration code

#define INTEGRATE(result, fn, data, step_count, t0, t1) \
{                                                       \
    float t_step = (t1-t0)/float(step_count);           \
    float3 sum = float3(0,0,0);                         \
    sum += fn(data, t0);                                \
    float t = t0+t_step;                                \
    for (uint istep=1; istep<step_count-1; istep += 2)  \
    {                                                   \
        sum += 4*fn(data, t);                           \
        t += t_step;                                    \
        sum += 2*fn(data, t);                           \
        t += t_step;                                    \
    }                                                   \
    sum += 4*fn(data, t);                               \
    sum += fn(data, t1);                                \
    result = (t_step/3.0f) * sum;                       \
}

////////////////////////////////////////////////////////////////////////////////
// Directional Light 

struct LightEvaluatorData_Directional {
    float VdotL;
    float3 sigma;
};

float3 LightEvaluator_Directional(LightEvaluatorData_Directional data, float t)
{
    float light_to_world_depth = g_fLightToEyeDepth + t*data.VdotL;
    return exp(-data.sigma*(t+light_to_world_depth));
}

float3 Integrate_Directional(float eye_dist, float3 vV, float3 vL)
{
    float VdotL = dot(vV, vL);
    // Manually integrate over interval
    LightEvaluatorData_Directional evaluator;
    float3 sigma = g_vSigmaExtinction;
    evaluator.VdotL = VdotL;
    const uint STEP_COUNT = 6;
    float3 integral = float3(0,0,0);
    INTEGRATE(integral, LightEvaluator_Directional, evaluator, STEP_COUNT, 0, eye_dist);
    return GetPhaseFactor(tPhaseLUT, -VdotL)*integral*exp(g_fLightToEyeDepth*(evaluator.sigma.r+evaluator.sigma.g+evaluator.sigma.b)/3.0);
}

float3 Integrate_SimpleDirectional(float eye_dist, float3 vV, float3 vL)
{
    // Do basic directional light
    float VdotL = dot(vV, vL);
    float3 sigma = g_vSigmaExtinction;
    return GetPhaseFactor(tPhaseLUT, -VdotL) * (1.0 - exp(-sigma*eye_dist)) / (sigma);
}

////////////////////////////////////////////////////////////////////////////////
// Spotlight

bool IntersectCone(out float t0, out float t1, float t_max, float cos_theta, float3 vW, float3 vV, float3 vL, float WdotL, float VdotL)
{
    float cos_sqr = cos_theta * cos_theta;
    float sin_sqr = 1 - cos_sqr;
    float3 v_proj = vV - VdotL*vL;
    float3 w_proj = vW - WdotL*vL;

    float A = cos_sqr*dot(v_proj, v_proj) - sin_sqr*VdotL*VdotL;
    float B = 2 * cos_sqr*dot(v_proj, w_proj) - 2 * sin_sqr*VdotL*WdotL;
    float C = cos_sqr*dot(w_proj, w_proj) - sin_sqr*WdotL*WdotL;

    float det = B*B - 4 * A*C;
    float denom = 2 * A;
    if (det < 0.0f || denom == 0.0f)
    {
        t0 = 0;
        t1 = 0;
        return false;
    }
    else
    {
        bool hit = true;
        float root = sqrt(det);
        t0 = (-B - root) / denom;
        t1 = (-B + root) / denom;

        float vW_len = length(vW);
        float WdotL_norm = (vW_len > 0.0f) ? WdotL / vW_len : 1.0f;
        if (WdotL_norm >= cos_theta)
        {
            if (VdotL >= cos_theta)
                t1 = t_max;
            t0 = 0;
        }
        else if (WdotL_norm <= -cos_theta)
        {
            if (t0 < 0 && t1>0)
                hit = false;
            t0 = t0;
            t1 = t_max;
        }
        else
        {
            if (t0 < 0 && t1 < 0)
                hit = false;
            else if (dot(vL, vW + t0*vV) < 0)
                hit = false;
            else if (t1<0)
                t1 = t_max;
        }

        if (t0 > t_max)
        {
            t0 = 0;
            t1 = 0;
            hit = false;
        }

        return hit;
    }
}

float GetPrecomputedPtLghtSrcTexU(in float3 f3Pos, in float3 f3EyeDir, in float3 f3ClosestPointToLight)
{
    return (dot(f3Pos - f3ClosestPointToLight, f3EyeDir) + g_fMaxTracingDistance) / (2.0*g_fMaxTracingDistance);
}

struct LightEvaluatorData_Spotlight
{
    float3 sigma;
    float light_theta;
    float light_falloff_power;
    float Wsqr;
    float WdotV;
    float WdotL;
    float VdotL;
};

float3 LightEvaluator_Spotlight(LightEvaluatorData_Spotlight data, float t)
{
    float Dsqr = max(data.Wsqr+2*data.WdotV*t+t*t, 0.0f);
    float D = sqrt(Dsqr);
    float cos_phi = (t>0 && D>0) ? (t*t + Dsqr - data.Wsqr) / (2 * t*D) : 0;
    float3 phase_factor = GetPhaseFactor(tPhaseLUT, -cos_phi);
    float distance_attenuation = AttenuationFunc(D);
    float Dproj = data.WdotL + t*data.VdotL;
    float cos_alpha = (D>0.0f) ? Dproj/D : 1.0f;
    float angle_factor = saturate(cos_alpha-data.light_theta)/(1-data.light_theta);
    const float ANGLE_EPSILON = 0.000001f;
    float spot_attenuation = (angle_factor > ANGLE_EPSILON) ? pow(abs(angle_factor), data.light_falloff_power) : 0.0f;
    float3 media_attenuation = exp(-data.sigma*(t+D));
    return phase_factor*distance_attenuation*spot_attenuation*media_attenuation;
}

float3 GetInsctrIntegral_SRNN05( in float3 f3A1, in float3 f3Tsv, in float fCosGamma, in float fSinGamma, in float fDistFromCamera)
{
    // f3A1 depends only on the location of the camera and the light source
    // f3Tsv = fDistToLight * g_MediaParams.f4SummTotalBeta.rgb
    float3 f3Tvp = fDistFromCamera * g_vScatterPower.rgb;
    float3 f3Ksi = PI/4.f + 0.5f * atan( (f3Tvp - f3Tsv * fCosGamma) / (f3Tsv * fSinGamma) );
    float2 f2SRNN05LUTParamLimits = GetSRNN05LUTParamLimits();
    // float fGamma = acos(fCosGamma);
    // F(A1, Gamma/2) defines constant offset and thus is not required
    return float3(
//              g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(f3A1.x, f3Ksi.x)/f2SRNN05LUTParamLimits, 0).x ,
    			textureLod(tLightLUT_P, float2(f3A1.x, f3Ksi.x)/f2SRNN05LUTParamLimits, 0.0).x,
//              g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(f3A1.y, f3Ksi.y)/f2SRNN05LUTParamLimits, 0).x,
    			textureLod(tLightLUT_P, float2(f3A1.y, f3Ksi.y)/f2SRNN05LUTParamLimits, 0.0).x,
//              g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(f3A1.z, f3Ksi.z)/f2SRNN05LUTParamLimits, 0).x
    			textureLod(tLightLUT_P, float2(f3A1.z, f3Ksi.z)/f2SRNN05LUTParamLimits, 0.0).x
                );
}

float3 Integrate_Spotlight(float eye_dist, float3 vW, float3 vV, float3 vL)
{
    float3 integral = float3(0, 0, 0);
    float WdotL = dot(vW, vL);
    float VdotL = dot(vV, vL);
    float t0=0, t1=1;
    if (IntersectCone(t0, t1, eye_dist, g_fLightFalloffAngle, vW, vV, vL, WdotL, VdotL))
    {
        t1 = min(t1, eye_dist);

#       if (FALLOFFMODE == FALLOFFMODE_NONE)
        {
            float light_dist = length(vW);
            float3 vW_norm = vW / light_dist;
            float2 tc;
            tc.x = GetLutCoord_X(t1, light_dist);
            tc.y = GetLutCoord_Y(dot(vW_norm, vV));
            integral = SampleLut(tLightLUT_P, tc);
            if (t0 > 0)
            {
                tc.x = GetLutCoord_X(t0, light_dist);
                integral -= SampleLut(tLightLUT_P, tc);
            }
            integral *= g_vScatterPower;
        }
#       elif (FALLOFFMODE == FALLOFFMODE_FIXED)
        {
            float light_dist = length(vW);
            float3 vW_norm = vW / light_dist;
            float2 tc;            
            tc.x = GetLutCoord_X(t1, light_dist);
            tc.y = GetLutCoord_Y(dot(vW_norm, vV));
            integral = WdotL*SampleLut(tLightLUT_S1, tc) + VdotL*SampleLut(tLightLUT_S2, tc) - g_fLightFalloffAngle*SampleLut(tLightLUT_P, tc);
            if (t0 > 0)
            {
                tc.x = GetLutCoord_X(t0, light_dist);
                integral -= WdotL*SampleLut(tLightLUT_S1, tc) + VdotL*SampleLut(tLightLUT_S2, tc) - g_fLightFalloffAngle*SampleLut(tLightLUT_P, tc);
            }
            integral *= g_vScatterPower / (1-g_fLightFalloffAngle);
        }
#       elif (FALLOFFMODE == FALLOFFMODE_CUSTOM)
        {
            LightEvaluatorData_Spotlight evaluator;
            evaluator.sigma = g_vSigmaExtinction;
            evaluator.light_theta = g_fLightFalloffAngle;
            evaluator.light_falloff_power = g_fLightFalloffPower;
            evaluator.Wsqr = dot(vW, vW);
            evaluator.WdotV = dot(vW, vV);
            evaluator.WdotL = WdotL;
            evaluator.VdotL = VdotL;
            const uint STEP_COUNT = 8;
            INTEGRATE(integral, LightEvaluator_Spotlight, evaluator, STEP_COUNT, t0, t1);
            integral *= 6;
        }
#       elif(FALLOFFMODE == FALLOFFMODE_INTEL)
        {
             //                       Light
             //                        *                   -
             //                     .' |\                  |
             //                   .'   | \                 | fClosestDistToLight
             //                 .'     |  \                |
             //               .'       |   \               |
             //          Cam *--------------*--------->    -
             //              |<--------|     \
             //                  \
             //                  fStartDistFromProjection
            float3 f3StartPos = g_vEyePosition.xyz + max(t0, 0.0) * vV;
            float3 f3ReconstructedPos = g_vEyePosition.xyz + t1 * vV;
            float fDistToLight = length(vW);
            float3 vW_norm = vW / fDistToLight;
            float fCosLV = dot(vV, -vW_norm);
            float fDistToClosestToLightPoint = fDistToLight * fCosLV;
            float fClosestDistToLight = fDistToLight * sqrt(1.0 - fCosLV*fCosLV);
            float fV = fClosestDistToLight / g_fMaxTracingDistance;

            float3 f3ClosestPointToLight = g_vEyePosition.xyz + vV * fDistToClosestToLightPoint;

            float fCameraU = GetPrecomputedPtLghtSrcTexU(f3StartPos, vV, f3ClosestPointToLight);
            float fReconstrPointU = GetPrecomputedPtLghtSrcTexU(f3ReconstructedPos, vV, f3ClosestPointToLight);

            float3 f3CameraInsctrIntegral = // g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fCameraU, fV), 0);
            		 					textureLod(tLightLUT_P, float2(fCameraU, fV), 0.0).xyz;
            float3 f3RayTerminationInsctrIntegral = exp(-(t1 - max(t0, 0.0))*g_vScatterPower.rgb) * // g_tex2DPrecomputedPointLightInsctr.SampleLevel(samLinearClamp, float2(fReconstrPointU, fV), 0);
            		 					textureLod(tLightLUT_P, float2(fReconstrPointU, fV), 0.0).xyz;

            integral = f3CameraInsctrIntegral - f3RayTerminationInsctrIntegral;
            integral *= g_vScatterPower;
        }
#       elif(FALLOFFMODE == FALLOFFMODE_SRNN05)
        {
            float light_dist = length(vW);
            float3 vW_norm = vW / light_dist;
            float fCosLV = dot(vV, -vW_norm);
            float3 f3Tsv = light_dist * g_vScatterPower.rgb;
            float fSinGamma = max(sqrt( 1 - fCosLV*fCosLV ), 1e-6);
            float3 f3A0 = g_vScatterPower.rgb * g_vScatterPower.rgb *
                       //g_LightAttribs.f4LightColorAndIntensity.rgb * g_LightAttribs.f4LightColorAndIntensity.w *
                       exp(-f3Tsv * fCosLV) /
                       (2.0*PI * f3Tsv * fSinGamma);
            float3 f3A1 = f3Tsv * fSinGamma;

            float3 f3CameraInsctrIntegral = f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, max(0.0, t0));
            float3 f3RayTerminationInsctrIntegral = f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, t1);

            integral = f3RayTerminationInsctrIntegral - f3CameraInsctrIntegral;

            /*if(t0 > 0)
            {
                integral -= f3A0 * GetInsctrIntegral_SRNN05( f3A1, f3Tsv, fCosLV, fSinGamma, t0);
            }*/
//            integral *= g_vScatterPower;
        }
#endif
    }
    return integral;
}

////////////////////////////////////////////////////////////////////////////////
// Omni

float3 Integrate_Omni(float eye_dist, float3 vW, float3 vV)
{
    float light_dist = length(vW);
    vW = vW / light_dist;
    float2 tc;
    tc.x = GetLutCoord_X(eye_dist, light_dist);
    tc.y = GetLutCoord_Y(dot(vW, vV));
    return g_vScatterPower*SampleLut(tLightLUT_P, tc);
}

////////////////////////////////////////////////////////////////////////////////
// Shader Entrypoint

/*
float4 main(
#if (PASSMODE == PASSMODE_FINAL)
    VS_QUAD_OUTPUT pi
    , uint sampleID : SV_SAMPLEINDEX
#else
    PS_POLYGONAL_INPUT pi
#endif  
    , bool bIsFrontFace : SV_ISFRONTFACE
        ) : SV_TARGET
*/
#if (PASSMODE == PASSMODE_FINAL)
in float2 vTex;
in float4 vWorldPos;
#else
in float4 vWorldPos;
#endif

layout(location = 0) out float4 OutColor;
void main()
{
	float2 vPos = gl_FragCoord.xy;
#if (PASSMODE != PASSMODE_FINAL)
    int sampleID = 0;
#else
	int sampleID = gl_SampleID;
#endif
    float fSign = 0;
    float4 pWorldPos = float4(0, 0, 0, 1);
    float eye_dist = 0;
    float3 vV = float3(0, 0, 0);
    if (PASSMODE == PASSMODE_GEOMETRY)
    {
        fSign = gl_FrontFacing ? -1.0f : 1.0f;
        pWorldPos = vWorldPos;
        eye_dist = length(pWorldPos.xyz - g_vEyePosition.xyz);
        vV = (pWorldPos.xyz - g_vEyePosition.xyz) / eye_dist;
    }
    else if (PASSMODE == PASSMODE_SKY)
    {
        fSign = 1.0f;
        eye_dist = g_fZFar;
        vV = normalize(vWorldPos.xyz - g_vEyePosition.xyz);
        pWorldPos.xyz = g_vEyePosition.xyz + vV * eye_dist;
        pWorldPos.w = 1;
    }
    else if (PASSMODE == PASSMODE_FINAL)
    {
        fSign = 1.0f;
        float fSceneDepth = LoadSceneDepth(int2(vPos), sampleID);
        float4 vClipPos;
        vClipPos.xy = 2.0 *g_vViewportSize_Inv*vPos.xy - 1.0;
        vClipPos.z = 2.0 * fSceneDepth - 1.0;
        vClipPos.w = 1.0;
        pWorldPos = mul(g_mViewProjInv, vClipPos);
        pWorldPos *= 1.0 / pWorldPos.w;
        eye_dist = length(pWorldPos.xyz - g_vEyePosition.xyz);
        vV = (pWorldPos.xyz - g_vEyePosition.xyz) / eye_dist;
    }

    float3 vL = g_vLightDir.xyz;

    float3 integral = float3(0,0,0);
    if (LIGHTMODE == LIGHTMODE_DIRECTIONAL)
    {
        integral = Integrate_SimpleDirectional(eye_dist, vV, vL);
    }
    else if (LIGHTMODE == LIGHTMODE_SPOTLIGHT)
    {
        float3 vW = g_vEyePosition.xyz - g_vLightPos.xyz;
        integral = Integrate_Spotlight(eye_dist, vW, vV, vL);
    }
    else if (LIGHTMODE == LIGHTMODE_OMNI)
    {
        float3 vW = g_vEyePosition.xyz - g_vLightPos.xyz;
        integral = Integrate_Omni(eye_dist, vW, vV);
    }
    
    
//    OutColor = float4(max(fSign*integral*g_vLightIntensity.rgb, float3(-10)), 0);
    OutColor = float4(fSign*integral*g_vLightIntensity.rgb, fSign);
    
    /*
    if (PASSMODE == PASSMODE_FINAL)
    {
    	OutColor = float4(1);
    }else
    {
    	OutColor = float4(fSign*integral*g_vLightIntensity.rgb, 0);
    }
    */
}
