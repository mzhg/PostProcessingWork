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

layout (triangles) in;
layout (triangle_strip, max_vertices = 6) out;

#include "Uniforms.frag"

in VS_OUTPUT
{
	float4 P;  // TEXCOORD0
	float3 N;  // NORMAL0;
	float ScreenZ;
}Inputs[];

in gl_PerVertex
{
	float4 gl_Position;
}gl_in[];

out gl_PerVertex
{
	float4 gl_Position;
};

out GS_OUTPUT
{
	float4 P;
	float3 N;
//	uint Target;
//	float 
}outValue;

struct VS_Out
{
	float4 P;  // TEXCOORD0
	float3 N;  // NORMAL0;
	float ScreenZ;
	float3 ScreenP;
};

////////////////////////////////////////////////////////////////////////////////
// Geometry Shader

float3 ParaboloidProject(float3 P, float zNear, float zFar)
{
	float3 outP;
	float lenP = length(P.xyz);
	outP.xyz = P.xyz/lenP;
	outP.x = outP.x / (outP.z + 1);
	outP.y = outP.y / (outP.z + 1);			
	outP.z = (lenP - zNear) / (zFar - zNear);
	outP.z = 2 * outP.z - 1;
	return outP;
}

void GenerateOmniTriangle(int target, VS_Out vA, VS_Out vB, VS_Out vC/*, inout TriangleStream<GS_OUTPUT> output*/)
{
//    GS_OUTPUT outValue;
    gl_Layer = target;
    gl_Position = float4(ParaboloidProject(vA.ScreenP.xyz, c_fLightZNear, c_fLightZNFar), 1);
    outValue.P = vA.P;
    outValue.N = vA.N;
    EmitVertex();
    
    gl_Position = float4(ParaboloidProject(vB.ScreenP.xyz, c_fLightZNear, c_fLightZNFar), 1);
    outValue.P = vB.P;
    outValue.N = vB.N;
	EmitVertex();
	
    gl_Position = float4(ParaboloidProject(vC.ScreenP.xyz, c_fLightZNear, c_fLightZNFar), 1);
    outValue.P = vC.P;
    outValue.N = vC.N;
	EmitVertex();
	
    EndPrimitive();
}

/*
VS_Out convert(VS_OUTPUT vs_out, float3 ScreenP)
{
	VS_Out result;
	result.P = vs_out.P;
	result.N = vs_out.N;
	result.ScreenZ = vs_out.ScreenZ;
	result.ScreenP = ScreenP;
	
	return result;
}
*/

#define CONVERT(result, vs_out, screenP)  \
	result.P = vs_out.P;  \
	result.N = vs_out.N;  \
	result.ScreenZ = vs_out.ScreenZ; \
	result.ScreenP = screenP; 

void main()
{
	float minZ = min(Inputs[0].ScreenZ, min(Inputs[1].ScreenZ, Inputs[2].ScreenZ));
    float maxZ = max(Inputs[0].ScreenZ, max(Inputs[1].ScreenZ, Inputs[2].ScreenZ));

    VS_Out input0, input1, input2;
    CONVERT(input0, Inputs[0], gl_in[0].gl_Position.xyz)
    CONVERT(input1, Inputs[1], gl_in[1].gl_Position.xyz)
    CONVERT(input2, Inputs[2], gl_in[2].gl_Position.xyz)

    if (maxZ >= 0)
    {
        GenerateOmniTriangle(0, input0, input1, input2);
    }

    if (minZ <= 0)
    {
        input0.ScreenZ *= -1.0;
        input1.ScreenZ *= -1.0;
        input2.ScreenZ *= -1.0;
        GenerateOmniTriangle(1, input2, input1, input0);
    }
}