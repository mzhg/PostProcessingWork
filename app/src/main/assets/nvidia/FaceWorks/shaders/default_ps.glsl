#include "common.glsl"
#include "lighting.glsl"
#include "../../../shader_libs/FaceWork/GFSDK_FaceWorks.glsl"

layout(binding=TEX_DIFFUSE0) uniform sampler2D g_texDiffuse;

//in float2 m_uv;

in VertexThrough
{
//    float3		m_pos		/*: POSITION*/;
    float3		m_normal	/*: NORMAL*/;
    float2		m_uv		/*: UV*/;
    float3		m_tangent	/*: TANGENT*/;
    float		m_curvature /*: CURVATURE*/;
}i_vtx;

//float4 main(in float2 i_uv : UV) : SV_Target
layout(location=0) out vec4 Out_Color;


void main()
{
    Out_Color = texture(g_texDiffuse, i_vtx.m_uv.xy);
}