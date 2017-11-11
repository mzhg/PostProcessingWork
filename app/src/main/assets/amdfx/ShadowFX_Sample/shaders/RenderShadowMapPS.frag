#include "SceneRenderCommon.glsl"

in float3 m_f3PositionWS; //   : WS_POSITION;
in float3 m_f3Normal; //       : NORMAL;
in float2 m_f2TexCoord; //     : TEXCOORD0;

layout(location =0) out float4 Out_f4Color;

void main()
{
	float4 color = texture(g_t2dDiffuse, m_f2TexCoord);  // g_SampleLinear
	if (color.a <= 0.50) discard;
	
	Out_f4Color = color * g_Viewer.m_Color;
}