#include "PostProcessingCommonPS.frag"

uniform vec4 g_MixCoeff;

uniform sampler2D g_Texture1;
uniform sampler2D g_Texture2;
uniform sampler2D g_Texture3;
uniform sampler2D g_Texture4;


void main()
{
	Out_f4Color =  texture(g_Texture1, m_f4UVAndScreenPos.xy)*g_MixCoeff.x
    			 + texture(g_Texture2, m_f4UVAndScreenPos.xy)*g_MixCoeff.y
    			 + texture(g_Texture3, m_f4UVAndScreenPos.xy)*g_MixCoeff.z
    			 + texture(g_Texture4, m_f4UVAndScreenPos.xy)*g_MixCoeff.w;

    Out_f4Color = min(vec4(256.0 * 256.0), Out_f4Color);
}