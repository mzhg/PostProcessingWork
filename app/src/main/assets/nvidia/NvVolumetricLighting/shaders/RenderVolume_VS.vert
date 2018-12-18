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
- MESHMODE:
    - MESHMODE_FRUSTUM_GRID
    - MESHMODE_FRUSTUM_BASE
    - MESHMODE_FRUSTUM_CAP
    - MESHMODE_OMNI_VOLUME
    - MESHMODE_GEOMETRY
%% MUX_END %%
*/

#include "ShaderCommon.frag"

#if (MESHMODE == MESHMODE_GEOMETRY)
    layout(location = 0) in float4 input_position;  
#endif

// Bypass vertex shader
//HS_POLYGONAL_INPUT main( 
//#if (MESHMODE == MESHMODE_GEOMETRY)
//    float4 input_position : POSITION,
//#endif
//    uint id : SV_VERTEXID )

float4 GenerateOMNIVertexCube()
{
    int id = gl_VertexID;
    int verts_per_face = 4*g_uMeshResolution*g_uMeshResolution;
    int face_idx = id / verts_per_face;
    int face_vert_idx = id % verts_per_face;

    const float patch_size = 2.0f / float(g_uMeshResolution);
    int patch_idx = face_vert_idx / 4;
    int patch_row = patch_idx / g_uMeshResolution;
    int patch_col = patch_idx % g_uMeshResolution;

    float3 P;
    P.x = patch_size*patch_col - 1.0f;
    P.y = patch_size*patch_row - 1.0f;

    int vtx_idx = id % 4;
    /*if((face_idx % 3) == 1)
    {
        if(vtx_idx != 0)
            vtx_idx = 4 - vtx_idx;
    }*/

    float2 vtx_offset;
    if (vtx_idx == 0)
    {
        vtx_offset = float2(0, 0);
    }
    else if (vtx_idx == 1)
    {
        vtx_offset = float2(1, 0);
    }
    else if (vtx_idx == 2)
    {
        vtx_offset = float2(1, 1);
    }
    else // if (vtx_idx == 3)
    {
        vtx_offset = float2(0, 1);
    }
    P.xy += patch_size * vtx_offset;
    P.z = ((face_idx / 3) == 0) ? 1 : -1;
    if ((face_idx % 3) == 0)  // Right(+) and Left(-) faces
        P.yzx = P.xyz * (((face_idx / 3) == 0) ? float3(1,1,1) : float3(-1,1,1));
    else if ((face_idx % 3) == 1)  // Top(+) and Bottom(-) faces
        P.xzy = P.xyz * (((face_idx / 3) == 1) ? float3(1,1,1) : float3(-1,1,1));
     else //if ((face_idx % 3) == 2)  // Front(+) and Back(-) faces
        P.xyz = P.xyz * (((face_idx / 3) == 0) ? float3(1,1,1) : float3(-1,1,1));
    return float4(normalize(P.xyz), 1);
}

float4 GenerateOMNIVertexSphere()
{
    int id = gl_VertexID;
    const float patch_size = 2.0 / float(g_uMeshResolution);
    int patch_idx = id / 4;
    int patch_row = patch_idx / g_uMeshResolution;
    int patch_col = patch_idx % g_uMeshResolution;

    float2 vClipPos;

    vClipPos.x = patch_size*patch_col - 1.0f;
    vClipPos.y = patch_size*patch_row - 1.0f;

    uint vtx_idx = id % 4;
//    if(vtx_idx != 0)
//    {
//        vtx_idx = 4u - vtx_idx;
//    }
    float2 vtx_offset;
    if (vtx_idx == 0)
    {
        vtx_offset = float2(0, 0);
    }
    else if (vtx_idx == 1)
    {
        vtx_offset = float2(1, 0);
    }
    else if (vtx_idx == 2)
    {
        vtx_offset = float2(1, 1);
    }
    else // if (vtx_idx == 3)
    {
        vtx_offset = float2(0, 1);
    }
    vClipPos.xy += patch_size * vtx_offset;

    vClipPos = vClipPos * 0.5 + 0.5;

    float theta = PI * 2. * vClipPos.y;
    float fei = PI * vClipPos.x;

    float3 pos;
    pos.x = sin(fei) * cos(theta);
    pos.y = sin(fei) * sin(theta);
    pos.z = cos(fei);

    return float4(pos,1);
}

out float4 vClipPos;
out float4 vWorldPos;
// out float4 vPos;

void main()
{
	int id = gl_VertexID;
#if (MESHMODE != MESHMODE_GEOMETRY)
    float4 input_position = float4(0,0,0,1);
#endif
//	HS_POLYGONAL_INPUT output;
    //
    // Generate the mesh dynamically from the vertex ID
    //
    if (MESHMODE == MESHMODE_FRUSTUM_GRID)
    {
        const float patch_size = 2.0f / float(g_uMeshResolution);
        int patch_idx = id / 4;
        int patch_row = patch_idx / g_uMeshResolution;
        int patch_col = patch_idx % g_uMeshResolution;
        vClipPos.x = patch_size*patch_col - 1.0f;
        vClipPos.y = patch_size*patch_row - 1.0f;

        uint vtx_idx = id % 4;
        if(vtx_idx != 0)
        {
        	vtx_idx = 4u - vtx_idx;
        }
        float2 vtx_offset;
        if (vtx_idx == 0)
        {
            vtx_offset = float2(0, 0);
        }
        else if (vtx_idx == 1)
        {
            vtx_offset = float2(1, 0);
        }
        else if (vtx_idx == 2)
        {
            vtx_offset = float2(1, 1);
        }
        else // if (vtx_idx == 3)
        {
            vtx_offset = float2(0, 1);
        }
        vClipPos.xy += patch_size * vtx_offset;

        vClipPos.z = 1.0f;
        vClipPos.w = 1.0f;
    }
    else if (MESHMODE == MESHMODE_FRUSTUM_BASE)
    {
        int vtx_idx = id % 3;
        if(vtx_idx != 0)
        {
        	vtx_idx = 3 - vtx_idx;
        }
        
        vClipPos.x = (vtx_idx == 0) ? 1.0 : -1.0;
        vClipPos.y = (vtx_idx == 2) ? -1.0 : 1.0;
        vClipPos.xy *= (id/3 == 0) ? 1.0 : -1.0;
        vClipPos.z = 1.0f;
        vClipPos.w = 1.0f;
    }
    else if (MESHMODE == MESHMODE_FRUSTUM_CAP)
    {
        int tris_per_face = g_uMeshResolution+1;
        int verts_per_face = 3*tris_per_face;
        int face_idx = id / verts_per_face;
        int vtx_idx = id % 3;
        if(vtx_idx != 0)
        {
        	vtx_idx = 3 - vtx_idx;
        }
        
        if (face_idx < 4)
        {
            // Cap Side
            const float patch_size = 2.0f / float(g_uMeshResolution);
            const int split_point = (g_uMeshResolution+1)/2;
            float3 v;
            int tri_idx = (id%verts_per_face)/3;
            if (tri_idx < g_uMeshResolution)
            {
                if (vtx_idx == 0)
                    v.x = (tri_idx >= split_point) ? 1 : -1;
                else if (vtx_idx == 1)
                    v.x = patch_size * tri_idx - 1;
                else // if (vtx_idx == 2)
                    v.x = patch_size * (tri_idx+1) - 1;
                v.y = (vtx_idx == 0) ? 0 : 1;
            }
            else
            {
                if (vtx_idx == 1)
                    v.x = patch_size*split_point-1;
                else
                    v.x = (vtx_idx == 0) ? -1 : 1;
                v.y = (vtx_idx == 1) ? 1 : 0;
            }
            v.z = 1;
            v.xz *= (face_idx/2 == 0) ? 1 : -1;
            vClipPos.xyz = (face_idx%2 == 0) ? v.zxy : v.xzy*float3(-1,1,1);
        }
        else
        {
            // Z=0
            int tri_idx = (id-4*verts_per_face)/3;
            vClipPos.x = (vtx_idx == 1) ? 1 : -1;
            vClipPos.y = (vtx_idx == 2) ? 1 : -1;
            vClipPos.xy *= (tri_idx == 0) ? 1 : -1;
            vClipPos.z = 0.0f;
        }
        
        vClipPos.z =2.0 * vClipPos.z - 1.0;  // remap dx depth to gl depth.
        vClipPos.w = 1.0f;
    }
    else if (MESHMODE == MESHMODE_OMNI_VOLUME)
    {
        // TODO Using a sphere to instead the cube box may be avoid the face wind problems.
//        vClipPos = GenerateOMNIVertexCube();
        vClipPos = GenerateOMNIVertexSphere();
    }
    else
    {
        vClipPos = input_position;

    }

    if (MESHMODE == MESHMODE_OMNI_VOLUME)
    {
        vWorldPos = mul(g_mLightToWorld, float4(g_fLightZFar*vClipPos.xyz, 1));
    }
    else
    {
	    vWorldPos = mul(g_mLightToWorld, vClipPos);
    }
    vWorldPos = vWorldPos / vWorldPos.w;
//	vPos = mul(g_mViewProj, vWorldPos);
    
    gl_Position = mul(g_mViewProj, vWorldPos);
#   if (MESHMODE == MESHMODE_FRUSTUM_CAP)
	
#   endif
}
