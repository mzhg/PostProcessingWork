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

layout(location = 0) out vec2 OutColor;
in vec2 m_Texcoord;
void main()
{
	float CenterDepth;
    float AO = ComputeBlur(IN, float2(g_f2InvFullResolution.x,0), CenterDepth);

    OutColor = float2(AO, CenterDepth);
}