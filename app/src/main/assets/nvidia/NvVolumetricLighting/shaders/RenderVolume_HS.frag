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

- SHADOWMAPTYPE:
    - SHADOWMAPTYPE_ATLAS
    - SHADOWMAPTYPE_ARRAY

- CASCADECOUNT:
    - CASCADECOUNT_1: 1
    - CASCADECOUNT_2: 2
    - CASCADECOUNT_3: 3
    - CASCADECOUNT_4: 4

- VOLUMETYPE:
	- VOLUMETYPE_FRUSTUM
	- VOLUMETYPE_PARABOLOID

- MAXTESSFACTOR:
    - MAXTESSFACTOR_LOW: 16.0f
    - MAXTESSFACTOR_MEDIUM: 32.0f
    - MAXTESSFACTOR_HIGH: 64.0f
%% MUX_END %%
*/

#define COARSE_CASCADE (CASCADECOUNT-1)

#include "ShaderCommon.frag"
layout (vertices = 4) out;

float3 NearestPos(float3 vStartPos, float3 vEndPos)
{
    float3 vPos = (g_vEyePosition - vStartPos);
    float3 vLine = (vEndPos - vStartPos);
    float lineLength = length(vLine);
    float t = max(0.0, min(lineLength, dot(vPos, vLine)/lineLength));
    return vStartPos + (t/lineLength)*vLine;
}

float CalcTessFactor(float3 vStartPos, float3 vEndPos)
{
    float section_size = length(vEndPos - vStartPos);
	float3 vWorldPos = 0.5*(vStartPos+vEndPos);
	float3 vEyeVec = (vWorldPos.xyz - g_vEyePosition);
	float4 clip_pos = mul( g_mProj, float4(0, 0, -length(vEyeVec), 1) );
	float projected_size = abs(section_size * g_mProj[1][1] / (clip_pos.w * 0.5));
	float desired_splits = (projected_size*g_vOutputViewportSize.y)/(g_fTargetRaySize);
	return min(MAXTESSFACTOR, max(3.0, desired_splits));
}

bool IntersectsFrustum(float4 vPos1, float4 vPos2)
{
	return !(vPos1.x > 1.0 && vPos2.x > 1.0 || vPos1.x < -1.0 && vPos2.x < -1.0)
		|| !(vPos1.y > 1.0 && vPos2.y > 1.0 || vPos1.y < -1.0 && vPos2.y < -1.0)
		|| !(vPos1.z < -1.0 && vPos2.z < -1.0);
//		|| !(vPos1.z < 0.0 && vPos2.z < 0.0);
}

//HS_POLYGONAL_CONSTANT_DATA_OUTPUT HS_POLYGONAL_CONSTANT_FUNC( /*uint PatchID : SV_PRIMITIVEID,*/ const OutputPatch<HS_POLYGONAL_CONTROL_POINT_OUTPUT, 4> opPatch)

in float4 vClipPos[];
in float4 vWorldPos[];

out float4 hClipPos[];
out float4 hWorldPos[];

void main()
{
//	HS_POLYGONAL_CONSTANT_DATA_OUTPUT output  = (HS_POLYGONAL_CONSTANT_DATA_OUTPUT)0;
	hWorldPos[gl_InvocationID] = vWorldPos[gl_InvocationID];
	hClipPos[gl_InvocationID] = vClipPos[gl_InvocationID];
	
	bool bIsVisible = false;
#if 1
	//Frustum cull
//	[unroll]
	for (int j=0; j<4; ++j)
	{
		float4 vScreenClip = mul(g_mViewProj, vWorldPos[j]);
		vScreenClip *= 1.0f / vScreenClip.w;
		float4 vOriginPos = float4(0,0,0,1);
		if (VOLUMETYPE == VOLUMETYPE_FRUSTUM)
		{
			vOriginPos = mul(g_mLightToWorld, float4(vClipPos[j].xy, -1, 1)); 
		}
		else if (VOLUMETYPE == VOLUMETYPE_PARABOLOID)
		{
			vOriginPos = float4(g_vLightPos, 1); 
		}
		float4 vScreenClipOrigin = mul(g_mViewProj, vOriginPos);
		vScreenClipOrigin *= 1.0f / vScreenClipOrigin.w; 
		bIsVisible = bIsVisible || IntersectsFrustum(vScreenClip, vScreenClipOrigin);
		
		if(bIsVisible)
		{
			break;
		}
	}
#else
	bIsVisible = true;
#endif

	if (bIsVisible)
	{
        float3 nearest_pos[4];
        for (int j=0; j < 4; ++j)
        {
            float3 start_pos;
            if (VOLUMETYPE == VOLUMETYPE_FRUSTUM)
            {
                float4 p = mul(g_mLightToWorld, float4(vClipPos[j].xy, -1, 1));
                start_pos = p.xyz / p.w;
            }
            else if (VOLUMETYPE == VOLUMETYPE_PARABOLOID)
                start_pos = g_vLightPos;
            else
                start_pos = float3(0, 0, 0);
            nearest_pos[j] = NearestPos(start_pos, vWorldPos[j].xyz);
        }

		float tess_factor[4];
//		[unroll]
		for (int k=0; k<4; ++k)
		{
            float tess_near = CalcTessFactor(nearest_pos[(k+3)%4], nearest_pos[k]);
            float tess_far = CalcTessFactor(vWorldPos[(k+3)%4].xyz,vWorldPos[k].xyz);
            tess_factor[k] = max(tess_near, tess_far);
            if (VOLUMETYPE == VOLUMETYPE_FRUSTUM)
            {
                bool bIsEdge = !(all(lessThan(abs(vClipPos[(k + 3) % 4].xy),  float2(EDGE_FACTOR)) || lessThan(abs(vClipPos[k].xy), float2(EDGE_FACTOR))));
                gl_TessLevelOuter[k] = (bIsEdge) ? 1.0 : tess_factor[k];
            }
            else if (VOLUMETYPE == VOLUMETYPE_PARABOLOID)
            {
                gl_TessLevelOuter[k] = tess_factor[k];
            }
            else
            {
                gl_TessLevelOuter[k] = 1.0;
            }
            
		}
		gl_TessLevelInner[0] = max(tess_factor[1], tess_factor[3]);
        gl_TessLevelInner[1] = max(tess_factor[0], tess_factor[2]);
	}
	else
	{
		gl_TessLevelOuter[0] = -1;
		gl_TessLevelOuter[1] = -1;
		gl_TessLevelOuter[2] = -1;
		gl_TessLevelOuter[3] = -1;
		gl_TessLevelInner[0] = -1;
		gl_TessLevelInner[1] = -1;
	}

//	return output;
}

/*                          
[domain("quad")]
[partitioning("integer")]
[outputtopology("triangle_ccw")]
[outputcontrolpoints(4)]
[patchconstantfunc("HS_POLYGONAL_CONSTANT_FUNC")]
[maxtessfactor(MAXTESSFACTOR)]
HS_POLYGONAL_CONTROL_POINT_OUTPUT main( InputPatch<HS_POLYGONAL_INPUT, 4> ipPatch, uint uCPID : SV_OUTPUTCONTROLPOINTID )
{
	HS_POLYGONAL_CONTROL_POINT_OUTPUT output = (HS_POLYGONAL_CONTROL_POINT_OUTPUT)0;
	output.vWorldPos = ipPatch[uCPID].vWorldPos;
	output.vClipPos = ipPatch[uCPID].vClipPos;
    return output;
}	
*/