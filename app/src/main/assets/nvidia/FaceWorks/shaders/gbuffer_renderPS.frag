#define USE_UNIFORM_VAR 1

#include "skin.glsl"


//float4 main(in float2 i_uv : UV) : SV_Target
layout(location=0) out vec3 Out_Color;

layout(binding = 8) uniform sampler2D g_NormalTex;
layout(binding = 9) uniform sampler2D g_UVTex;
layout(binding = 10) uniform sampler2D g_Tex;
layout(binding = 11) uniform sampler2D g_TangentCur;
layout(binding = 12) uniform sampler2D g_Position;

in vec4 m_f4UVAndScreenPos;

//uniform float3 o_vecCamera;
//uniform float4 o_uvzwShadow;

void main()
{
    float2 uv = float2(m_f4UVAndScreenPos.x, 1.-m_f4UVAndScreenPos.y);
    float4 posVtx = textureLod(g_Position, uv, 0.);
    if(posVtx.w == 0.0)
        discard;

    Vertex i_vtx;
//    i_vtx.m_pos = _input.m_pos;
    i_vtx.m_normal = textureLod(g_NormalTex, uv, 0.).xyz;
    i_vtx.m_uv = textureLod(g_UVTex, uv, 0.).xy;
    float4 tangentCur = textureLod(g_TangentCur, uv, 0.);
    i_vtx.m_tangent = tangentCur.xyz;
    i_vtx.m_curvature = tangentCur.w;

    float3 o_vecCamera = g_posCamera - posVtx.xyz;
    float4 o_uvzwShadow = mul(float4(posVtx.xyz, 1.0), g_matWorldToUvzwShadow);

    SkinMegashader(i_vtx, o_vecCamera, o_uvzwShadow, Out_Color, false, false);
//    Out_Color.xyz = float3(i_vtx.m_curvature);
}