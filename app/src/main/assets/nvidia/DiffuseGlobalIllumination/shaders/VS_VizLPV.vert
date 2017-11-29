#include "SimpleShading.glsl"
layout(location = 0) in float3 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out vec2 m_Tex;

out gl_PerVertex
{
    vec4 gl_Position;
};

out float3 LPVSpacePos;

void main()
{

    float3 Pos = In_Position;
    Pos *= g_sphereScale;
    Pos += (g_LPVSpacePos-float3(0.5,0.5,0.5));
    gl_Position = mul( float4( Pos, 1 ), g_WorldViewProjSimple );
    LPVSpacePos = g_LPVSpacePos;
}