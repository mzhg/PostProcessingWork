#include "skin.glsl"

in VertexThrough
{
//    float3		m_pos		/*: POSITION*/;
    float3		m_normal	/*: NORMAL*/;
    float2		m_uv		/*: UV*/;
    float3		m_tangent	/*: TANGENT*/;
    float		m_curvature /*: CURVATURE*/;
}_input;

//float4 main(in float2 i_uv : UV) : SV_Target
layout(location=0) out vec3 Out_Color;

in float3 o_vecCamera;
in float4 o_uvzwShadow;

void main()
{
    Vertex i_vtx;
//    i_vtx.m_pos = _input.m_pos;
    i_vtx.m_normal = _input.m_normal;
    i_vtx.m_uv = _input.m_uv;
    i_vtx.m_tangent = _input.m_tangent;
    i_vtx.m_curvature = _input.m_curvature;

//    float4 o_uvzwShadow = mul(float4(_input.m_pos, 1.0), g_matWorldToUvzwShadow);

    float4 i_uvzwShadow = o_uvzwShadow;
    i_uvzwShadow /= i_uvzwShadow.w;
    i_uvzwShadow.xyz = i_uvzwShadow.xyz * 0.5 + 0.5;
    SkinMegashader(i_vtx, o_vecCamera, i_uvzwShadow, Out_Color, %s, %s);
}