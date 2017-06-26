//
// Copyright (c) 2017 Advanced Micro Devices, Inc. All rights reserved.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

struct S_MODEL_DESC
{
    float4x4 m_World;
    float4x4 m_World_Inv;
    float4x4 m_WorldView;
    float4x4 m_WorldView_Inv;
    float4x4 m_WorldViewProjection;
    float4x4 m_WorldViewProjection_Inv;
    float4   m_Position;
    float4   m_Orientation;
    float4   m_Scale;
    float4   m_Ambient;
    float4   m_Diffuse;
    float4   m_Specular;
};

struct S_CAMERA_DESC
{
    float4x4 m_View;
    float4x4 m_Projection;
    float4x4 m_ViewProjection;
    float4x4 m_View_Inv;
    float4x4 m_Projection_Inv;
    float4x4 m_ViewProjection_Inv;
    float3   m_Position;
    float    m_Fov;
    float3   m_Direction;
    float    m_FarPlane;
    float3   m_Right;
    float    m_NearPlane;
    float3   m_Up;
    float    m_Aspect;
    float4   m_Color;
};


struct DirectionalLightInfo
{
    float3 direction;
    float3 color;
    float  specPower;
};

/*
static DirectionalLightInfo directionalLights[] = {
    { { -0.7943764, -0.32935333, 0.5103845 }, { 1.0, 0.7, 0.6 }, 50.0 },
};*/

#if 0

cbuffer CB_MODEL_DATA : register(b0) { S_MODEL_DESC g_Model; }

cbuffer CB_VIEWER_DATA : register(b1) { S_CAMERA_DESC g_Viewer; }

//--------------------------------------------------------------------------------------
// Buffers, Textures and Samplers
//--------------------------------------------------------------------------------------
Texture2D g_t2dDiffuse : register(t0);

SamplerState g_ssLinear : register(s0);
#else
layout (binding = 0) buffer BufferObject{
    S_MODEL_DESC g_Model;
};

layout (binding = 1) buffer BufferCamera{
    S_CAMERA_DESC g_Viewer;
};

layout(binding = 0) uniform sampler2D g_t2dDiffuse;
#endif
//--------------------------------------------------------------------------------------
// Shader structures
//--------------------------------------------------------------------------------------