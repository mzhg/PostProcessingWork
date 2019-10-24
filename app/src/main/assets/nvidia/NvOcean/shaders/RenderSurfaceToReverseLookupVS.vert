#include "ocean_surface_heights.glsl"

out VS_OUT
{
    float m_uv;
}O;

void main()
{
    int vID = gl_VertexID;
    int quadID = vID/4;
    float2 corner = kQuadCornerUVs[vID%4];

    int quadX = quadID%int(g_numQuadsW);
    int quadY = quadID/int(g_numQuadsW);

    O.m_uv = g_quadUVDims.xy * (corner + float2(quadX,quadY));
}