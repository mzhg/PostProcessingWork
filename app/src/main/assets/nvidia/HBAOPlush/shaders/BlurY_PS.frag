/*
#permutation ENABLE_SHARPNESS_PROFILE 0 1
#permutation KERNEL_RADIUS 2 4
*/

/* 
* Copyright (c) 2008-2017, NVIDIA CORPORATION. All rights reserved. 
* 
* NVIDIA CORPORATION and its licensors retain all intellectual property 
* and proprietary rights in and to this software, related documentation 
* and any modifications thereto. Any use, reproduction, disclosure or 
* distribution of this software and related documentation without an express 
* license agreement from NVIDIA CORPORATION is strictly prohibited. 
*/

#include "ConstantBuffers.glsl"
#include "Blur_Common.glsl"

layout(location = 0) out float OutColor;

float2 SubtractViewportOrigin()
{
//    IN.pos.xy -= g_f2InputViewportTopLeft;
	float2 relativeFragCoord = gl_FragCoord.xy - g_f2InputViewportTopLeft;
    return relativeFragCoord * g_f2InvFullResolution;
}

//-------------------------------------------------------------------------
//float4 BlurY_PS( PostProc_VSOut IN ) : SV_TARGET
void main()
{
    float2 uv = SubtractViewportOrigin(/*IN*/);

    float CenterDepth;
    float AO = ComputeBlur(uv, float2(0,g_f2InvFullResolution.y), CenterDepth);

    OutColor = pow(saturate(AO), g_fPowExponent);
}