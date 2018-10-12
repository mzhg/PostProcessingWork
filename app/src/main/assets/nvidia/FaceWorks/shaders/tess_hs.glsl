//----------------------------------------------------------------------------------
// File:        FaceWorks/samples/sample_d3d11/shaders/tess_hs.hlsl
// SDK Version: v1.0
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2016, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------

#include "common.glsl"

const float s_tessFactorMax = 3.0;
layout (vertices = 3) out;

in VertexThrough
{
    float3		m_pos		/*: POSITION*/;
    float3		m_normal	/*: NORMAL*/;
    float2		m_uv		/*: UV*/;
    float3		m_tangent	/*: TANGENT*/;
    float		m_curvature /*: CURVATURE*/;
}i_cps[];

out TessHSOut
{
    float3		m_pos		/*: POSITION*/;
    float3		m_normal	/*: NORMAL*/;
    float2		m_uv		/*: UV*/;
    float3		m_tangent	/*: TANGENT*/;
    float		m_curvature /*: CURVATURE*/;
}_outputs[];

/*void calcHSConstants(
	in InputPatch<Vertex, 3> i_cps,
	out PatchConstData o_pcd)*/
	void main()
{
    _outputs[gl_InvocationID].m_pos = i_cps[gl_InvocationID].m_pos;
    _outputs[gl_InvocationID].m_normal = i_cps[gl_InvocationID].m_normal;
    _outputs[gl_InvocationID].m_uv = i_cps[gl_InvocationID].m_uv;
    _outputs[gl_InvocationID].m_tangent = i_cps[gl_InvocationID].m_tangent;
    _outputs[gl_InvocationID].m_curvature = i_cps[gl_InvocationID].m_curvature;

	// Backface culling: check if the camera is behind all three tangent planes
	float3 vecNdotV =
	{
		dot(g_posCamera - i_cps[0].m_pos, i_cps[0].m_normal),
		dot(g_posCamera - i_cps[1].m_pos, i_cps[1].m_normal),
		dot(g_posCamera - i_cps[2].m_pos, i_cps[2].m_normal),
	};

//	if (all(vecNdotV < 0.0))
    if (all(lessThan(vecNdotV, float3(0))))
	{
		gl_TessLevelOuter[0] = 0.0;
		gl_TessLevelOuter[1] = 0.0;
		gl_TessLevelOuter[2] = 0.0;
		gl_TessLevelInner[0] = 0.0;
		gl_TessLevelInner[1] = 0.0;
		return;
	}

	// Frustum culling: check if all three verts are out on the same side of the frustum
	// This isn't quite correct because the displacement could make a patch visible even if
	// it fails this test; but in practice this is nearly impossible to notice
	float4 posClip0 = mul(float4(i_cps[0].m_pos, 1.0), g_matWorldToClip);
	float4 posClip1 = mul(float4(i_cps[1].m_pos, 1.0), g_matWorldToClip);
	float4 posClip2 = mul(float4(i_cps[2].m_pos, 1.0), g_matWorldToClip);
	float3 xs = { posClip0.x, posClip1.x, posClip2.x };
	float3 ys = { posClip0.y, posClip1.y, posClip2.y };
	float3 ws = { posClip0.w, posClip1.w, posClip2.w };
	if (all(lessThan(xs , -ws)) || all(greaterThan(xs , ws)) || all(lessThan(ys , -ws)) || all(greaterThan(ys, ws)))
	{
		gl_TessLevelOuter[0] = 0.0;
		gl_TessLevelOuter[1] = 0.0;
		gl_TessLevelOuter[2] = 0.0;
		gl_TessLevelInner[0] = 0.0;
        gl_TessLevelInner[1] = 0.0;
		return;
	}

	// Adaptive tessellation based on a screen-space error estimate using curvature

	// Calculate approximate screen-space edge length, but including z length as well,
	// so we don't undertessellate edges that are foreshortened
	float edge0 = length(i_cps[2].m_pos - i_cps[1].m_pos) / (0.5 * (posClip2.w + posClip1.w));
	float edge1 = length(i_cps[0].m_pos - i_cps[2].m_pos) / (0.5 * (posClip0.w + posClip2.w));
	float edge2 = length(i_cps[1].m_pos - i_cps[0].m_pos) / (0.5 * (posClip1.w + posClip0.w));

	// Calculate dots of the two normals on each edge - used to give more tessellation
	// in areas with higher curvature
	float normalDot0 = dot(i_cps[2].m_normal, i_cps[1].m_normal);
	float normalDot1 = dot(i_cps[0].m_normal, i_cps[2].m_normal);
	float normalDot2 = dot(i_cps[1].m_normal, i_cps[0].m_normal);

	// Calculate target screen-space error
	const float errPxTarget = 0.5;
	const float tanHalfFov = tan(0.5 * 0.5);
	const float errTarget = errPxTarget * 2.0 * tanHalfFov / 1080.0;

	// Calculate tess factors using curve fitting approximation to screen-space error
	// derived from curvature and edge length
	const float tessScale = 0.41 / sqrt(errTarget);
	gl_TessLevelOuter[0] = g_tessScale * sqrt(edge0) * pow(1.0 - saturate(normalDot0), 0.27);
	gl_TessLevelOuter[1] = g_tessScale * sqrt(edge1) * pow(1.0 - saturate(normalDot1), 0.27);
	gl_TessLevelOuter[2] = g_tessScale * sqrt(edge2) * pow(1.0 - saturate(normalDot2), 0.27);

	// Clamp to supported range
	gl_TessLevelOuter[0] = clamp(gl_TessLevelOuter[0], 1.0, s_tessFactorMax);
	gl_TessLevelOuter[1] = clamp(gl_TessLevelOuter[1], 1.0, s_tessFactorMax);
	gl_TessLevelOuter[2] = clamp(gl_TessLevelOuter[2], 1.0, s_tessFactorMax);

	// Set interior tess factor to maximum of edge factors
	gl_TessLevelInner[0] = max(max(gl_TessLevelOuter[0],
									   gl_TessLevelOuter[1]),
									   gl_TessLevelOuter[2]);
    gl_TessLevelInner[1] = 0.0;
}

/*
[domain("tri")]
[maxtessfactor(s_tessFactorMax)]
[outputcontrolpoints(3)]
[outputtopology("triangle_cw")]
[partitioning("fractional_odd")]
[patchconstantfunc("calcHSConstants")]
Vertex main(
	in InputPatch<Vertex, 3> i_cps,
	in uint iCp : SV_OutputControlPointID)
{
	return i_cps[iCp];
}
*/