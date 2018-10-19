//----------------------------------------------------------------------------------
// File:        FaceWorks/samples/sample_d3d11/shaders/hair_ps.hlsl
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
#include "lighting.glsl"

//cbuffer cbShader : CB_SHADER
layout(binding=CB_SHADER) uniform  cbShader
{
	float		g_specReflectance;
	float		g_gloss;
};

//Texture2D<float4> g_texDiffuse			: TEX_DIFFUSE0;
layout(binding = TEX_DIFFUSE0) uniform sampler2D g_texDiffuse;

//in Vertex i_vtx;
in float3 o_vecCamera/* : CAMERA*/;
in float4 o_uvzwShadow/* : UVZW_SHADOW*/;

in VertexThrough
{
//	float3		m_pos		/*: POSITION*/;
	float3		m_normal	/*: NORMAL*/;
	float2		m_uv		/*: UV*/;
	float3		m_tangent	/*: TANGENT*/;
	float		m_curvature /*: CURVATURE*/;
}_input;

//float4 main(in float2 i_uv : UV) : SV_Target
layout(location=0) out vec4 Out_Color;

void main(
	/*in Vertex i_vtx,
	in float3 i_vecCamera : CAMERA,
	in float4 i_uvzwShadow : UVZW_SHADOW,
	in bool front : SV_IsFrontFace,
	out float4 o_rgbaLit : SV_Target*/)
{
    Vertex i_vtx;
//    i_vtx.m_pos = _input.m_pos;
    i_vtx.m_normal = _input.m_normal;
    i_vtx.m_uv = _input.m_uv;
    i_vtx.m_tangent = _input.m_tangent;
    i_vtx.m_curvature = _input.m_curvature;

    bool front = gl_FrontFacing;
	float2 uv = i_vtx.m_uv;

	// Sample textures
	float4 rgbaDiffuse = texture(g_texDiffuse, uv);   // g_ssTrilinearRepeatAniso

	// Perform lighting
	if (!front)
		i_vtx.m_normal = -i_vtx.m_normal;

    GFSDK_FaceWorks_CBData dummy;
    dummy.data[0] = float4(0);
    dummy.data[1] = float4(0);
    dummy.data[2] = float4(0);

    float4 i_uvzwShadow = o_uvzwShadow;
    i_uvzwShadow /= i_uvzwShadow.w;
    i_uvzwShadow.xyz = i_uvzwShadow.xyz * 0.5 + 0.5;
    vec3 outColor;
	LightingMegashader(
		i_vtx,
		o_vecCamera,
		i_uvzwShadow,
		rgbaDiffuse.rgb,
		float3(0.0),
		float3(0.0),
		g_specReflectance,
		g_gloss,
		float3(0.0),
		dummy,
		outColor, //Out_Color.rgb,
		false,	// useNormalMap
		false,	// useSSS
		false);	// useDeepScatter

	// Write texture alpha for transparency
	Out_Color = vec4(outColor, rgbaDiffuse.a);
}
