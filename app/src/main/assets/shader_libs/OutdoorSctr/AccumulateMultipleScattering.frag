#include "Preprocessing.glsl"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float Out_fColor;

void main()
{
    float3 f3LUTCoords = float3(/*ProjToUV(In.m_f2PosPS)*/m_f4UVAndScreenPos.xy, g_GlobalCloudAttribs.f4Parameter.x);
    Out_fColor = textureLod(g_tex3DPrevSctrOrder, f3LUTCoords, 0).x;   //  samPointWrap
}