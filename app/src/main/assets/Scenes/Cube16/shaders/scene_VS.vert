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
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "Uniforms.frag"

out VS_OUTPUT
{
	float4 P;  // TEXCOORD0
	float3 N;  // NORMAL0;
	float ScreenZ;
}Output;

void main()
{
	uint id = uint(gl_VertexID);
	
	// Faces
    // +X, +Y, +Z, -X, -Y, -Z
    // Vertices
    // 0 ---15
    // |   / |
    // |  /  |
    // | /   |
    // 24--- 3
    
    uint face_idx = id / 6;
    uint vtx_idx = id % 6;
    float3 P;
    P.x = ((vtx_idx % 3) == 2) ? -1.0 : 1.0;
    P.y = ((vtx_idx % 3) == 1) ? -1.0 : 1.0;
    P.z = 0;
    if ((face_idx % 3) == 0)
        P.yzx = P.xyz;
    else if ((face_idx % 3) == 1)
        P.xzy = P.xyz;
    // else if ((face_idx % 3) == 2)
    //    P.xyz = P.xyz;
    P *= ((vtx_idx / 3) == 0) ? 1.0 : -1.0;
    
    float3 N;
    N.x = ((face_idx % 3) == 0) ? 1.0 : 0.0;
    N.y = ((face_idx % 3) == 1) ? 1.0 : 0.0;
    N.z = ((face_idx % 3) == 2) ? 1.0 : 0.0;
    N *= ((face_idx / 3) == 0) ? 1.0 : -1.0;
    P += N;

    Output.P = mul(c_mObject, float4(P, 1));
    gl_Position = mul(c_mViewProj, Output.P);
    Output.N = mul(c_mObject, float4(N, 0)).xyz;
    Output.ScreenZ = gl_Position.z;
}