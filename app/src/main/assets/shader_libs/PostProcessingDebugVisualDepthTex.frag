//----------------------------------------------------------------------------------
// File:        SoftShadows\assets\shaders/vistex.frag
// SDK Version: v1.2 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014, NVIDIA CORPORATION. All rights reserved.
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
#include "PostProcessingCommonPS.frag"

#if SHADOW_MAP_ARRAY
uniform sampler2DArray g_shadowMap;
#else
uniform sampler2D g_shadowMap;
#endif

uniform vec4 g_Uniforms;
#define g_lightZNear    g_Uniforms.x
#define g_lightZFar     g_Uniforms.y
#define g_slice         g_Uniforms.z
#define g_scalerFactor  g_Uniforms.w

/*
uniform float g_lightZNear;
uniform float g_lightZFar;
uniform float g_slice;
uniform float g_scalerFactor = 1.0f;
*/

float zClipToEye(float z)
{
    return g_lightZFar * g_lightZNear / (g_lightZFar - z * (g_lightZFar - g_lightZNear));   
}

void main()
{
#if SHADOW_MAP_ARRAY
    float z = texture(g_shadowMap, vec3(m_f4UVAndScreenPos.xy, g_slice)).x;
#else
	float z = texture(g_shadowMap, m_f4UVAndScreenPos.xy).x;
#endif

    float color = (zClipToEye(z) - g_lightZNear) / (g_lightZFar - g_lightZNear) * g_scalerFactor;
//    color = texture(g_shadowMap, texCoord).r;
    Out_f4Color = vec4(color, color, color, 1.0f);
}
