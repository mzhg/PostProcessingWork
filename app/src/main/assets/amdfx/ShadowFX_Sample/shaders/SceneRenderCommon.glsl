//
// Copyright (c) 2016 Advanced Micro Devices, Inc. All rights reserved.
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

#include "<std-class>hlsl_compatiable.glsl"
//--------------------------------------------------------------------------------------
// Constant buffer
//--------------------------------------------------------------------------------------

//=================================================================================================================================
// Pixel shader input structure
//=================================================================================================================================

struct S_MODEL_DATA
{
  float4x4    m_World;
  float4x4    m_WorldViewProjection;
  float4x4    m_WorldViewProjectionLight;
  float4      m_Diffuse;
  float4      m_Ambient;
  float4      m_Parameter0;
};

struct S_CAMERA_DATA
{
  float4x4    m_View;
  float4x4    m_Projection;
  float4x4    m_ViewInv;
  float4x4    m_ProjectionInv;
  float4x4    m_ViewProjection;
  float4x4    m_ViewProjectionInv;

  float4      m_BackBufferDim;
  float4      m_Color;

  float4      m_Eye;
  float4      m_Direction;
  float4      m_Up;
  float       m_Fov;
  float       m_Aspect;
  float       m_zNear;
  float       m_zFar;

  float4      m_Parameter0;
  float4      m_Parameter1;
  float4      m_Parameter2;
  float4      m_Parameter3;
};

#if 0
cbuffer CB_MODEL_DATA  : register( b0 )
{
  S_MODEL_DATA  g_Model;
}

cbuffer CB_VIEWER_DATA : register( b1 )
{
  S_CAMERA_DATA g_Viewer;
}

cbuffer CB_LIGHT_ARRAY_DATA  : register( b2 )
{
  S_CAMERA_DATA g_Light[6];
}

//--------------------------------------------------------------------------------------
// Buffers, Textures and Samplers
//--------------------------------------------------------------------------------------

// Textures
Texture2D               g_t2dDiffuse         : register( t0 );
Texture2D<float4>       g_t2dShadowMask      : register( t1 );
Texture2D<float>        g_t2dDepth           : register( t2 );

// Samplers
SamplerState            g_SampleLinear      : register( s0 );
#else
layout(binding=0) uniform CB_MODEL_DATA {
  S_MODEL_DATA   g_Model;
};

layout(binding=1) uniform CB_VIEWER_DATA {
  S_CAMERA_DATA g_Viewer;
};

layout(binding=2) uniform CB_LIGHT_ARRAY_DATA {
  S_CAMERA_DATA g_Light[6];
};

layout(binding =0) uniform sampler2D g_t2dDiffuse;
layout(binding =1) uniform sampler2D g_t2dShadowMask;
layout(binding =2) uniform sampler2D g_t2dDepth;
//layout(binding =0) uniform sampler2D g_t2dDiffuse;

#endif