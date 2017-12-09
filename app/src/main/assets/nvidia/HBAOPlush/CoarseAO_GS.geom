/* 
* Copyright (c) 2008-2017, NVIDIA CORPORATION. All rights reserved. 
* 
* NVIDIA CORPORATION and its licensors retain all intellectual property 
* and proprietary rights in and to this software, related documentation 
* and any modifications thereto. Any use, reproduction, disclosure or 
* distribution of this software and related documentation without an express 
* license agreement from NVIDIA CORPORATION is strictly prohibited. 
*/

#include "ConstantBuffers.glsl"

/*
struct GSOut
{
    float4 pos  : SV_Position;
    float2 uv   : TEXCOORD0;
    uint LayerIndex : SV_RenderTargetArrayIndex;
};

[maxvertexcount(3)]
void CoarseAO_GS(triangle PostProc_VSOut input[3], inout TriangleStream<GSOut> OUT)
{
    GSOut OutVertex;

    OutVertex.LayerIndex = g_PerPassConstants.uSliceIndex;

    [unroll]
    for (int VertexID = 0; VertexID < 3; VertexID++)
    {
        OutVertex.uv  = input[VertexID].uv;
        OutVertex.pos = input[VertexID].pos;
        OUT.Append(OutVertex);
    }
}
*/

layout(triangles) in;

#extension GL_NV_geometry_shader_passthrough : enable

#if GL_NV_geometry_shader_passthrough

  layout(passthrough) in gl_PerVertex {
    vec4 gl_Position;
  };

  layout(passthrough) in vec4 m_f4UVAndScreenPos[];

  void main()
  {
    gl_Layer = g_PerPassConstants.uSliceIndex;
//    gl_PrimitiveID = gl_PrimitiveIDIn;
  }

#else
  layout(triangle_strip,max_vertices=3) out;

  in Inputs {
    vec4 m_f4UVAndScreenPos;
  } IN[];
  
  
  in gl_PerVertex {
    vec4 gl_Position;
  }gl_in[];
  
  out gl_PerVertex{
  	vec4 gl_Position;
  };
  
  out vec2 m_f4UVAndScreenPos;

  void main()
  {
    for (int i = 0; i < 3; i++){
      m_f4UVAndScreenPos = IN[i].m_f4UVAndScreenPos;
      gl_Layer = g_PerPassConstants.uSliceIndex;
      gl_Position = gl_in[i].gl_Position;
      EmitVertex();
    }
  }

#endif