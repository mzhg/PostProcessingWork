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
Define the shader permutations for code generation
%% MUX_BEGIN %%

%% MUX_END %%
*/

#include "ShaderCommon.frag"

out float2 vTex;
out float4 vWorldPos;

void main()
{
	int id = gl_VertexID;
	
	vTex = float2((id << 1) & 2, id & 2);
//	gl_Position = float4(vTex * float2(2,-2) + float2(-1,1), 0, 1);  // DX_FORM
	gl_Position = float4(vTex * float2(2,2) + float2(-1,-1), 1, 1);  // GL_FORM
	
	vWorldPos = g_mViewProjInv * gl_Position;
	vWorldPos/= vWorldPos.w;
//	0: [0.0, 0.0], [-1.0, 1.0]
//	1: [2.0, 0.0], [3.0, 1.0]
//	2: [0.0, 2.0], [-1.0, -3.0]
//	3: [2.0, 2.0], [3.0, -3.0]
}