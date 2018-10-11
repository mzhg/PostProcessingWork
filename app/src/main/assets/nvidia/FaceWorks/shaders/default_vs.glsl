#include "common.glsl"

layout(location =0) in float3		In_pos		/*: POSITION*/;
layout(location =1) in float3		In_normal	/*: NORMAL*/;
layout(location =2) in float2		In_uv		/*: UV*/;

out gl_PerVertex
{
    vec4 gl_Position;
};

out float2 m_uv;

void main(
	/*in Vertex i_vtx,
	out Vertex o_vtx,
	out float3 o_vecCamera : CAMERA,
	out float4 o_uvzwShadow : UVZW_SHADOW,
	out float4 o_posClip : SV_Position*/)
{
	m_uv = In_uv;
	gl_Position = mul(float4(In_pos, 1.0), g_matWorldToClip);
}