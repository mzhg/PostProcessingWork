#include "PostProcessingCommonPS.frag"

uniform vec4 g_ColorCoeff[4];

#if ENABLE_IN_OUT_FEATURE
	out vec2 m_TexCoord1;
	out vec2 m_TexCoord2;
	out vec2 m_TexCoord3;
	out vec2 m_TexCoord4;
#else
	varying vec2 m_TexCoord1;
    varying vec2 m_TexCoord2;
    varying vec2 m_TexCoord3;
    varying vec2 m_TexCoord4;
#endif

uniform sampler2D g_Texture1;
uniform sampler2D g_Texture2;
uniform sampler2D g_Texture3;
uniform sampler2D g_Texture4;


void main()
{
	Out_f4Color  = texture(g_Texture1, m_TexCoord1)*texture(g_Texture4, m_TexCoord1).g*g_ColorCoeff[0]
	             + texture(g_Texture1, m_TexCoord2)*texture(g_Texture4, m_TexCoord2).g*g_ColorCoeff[1]
	             + texture(g_Texture2, m_TexCoord3)*texture(g_Texture4, m_TexCoord3).g*g_ColorCoeff[2]
	             + texture(g_Texture3, m_TexCoord4)*texture(g_Texture4, m_TexCoord4).g*g_ColorCoeff[3];

//	gl_FragColor = min(vec4(256.0 * 256.0), gl_FragColor);
}