#include "PostProcessingCommonPS.frag"

uniform vec4 g_ColorCoeff[4];

#if ENABLE_IN_OUT_FEATURE
	in vec2 m_TexCoord1;
	in vec2 m_TexCoord2;
	in vec2 m_TexCoord3;
	in vec2 m_TexCoord4;
#else
	varying vec2 m_TexCoord1;
    varying vec2 m_TexCoord2;
    varying vec2 m_TexCoord3;
    varying vec2 m_TexCoord4;
#endif

uniform sampler2D g_Texture;

void main()
{
	Out_f4Color  = texture(g_Texture, m_TexCoord1)*g_ColorCoeff[0]
				 + texture(g_Texture, m_TexCoord2)*g_ColorCoeff[1]
				 + texture(g_Texture, m_TexCoord3)*g_ColorCoeff[2]
				 + texture(g_Texture, m_TexCoord4)*g_ColorCoeff[3];

//    gl_FragColor = min(vec4(256.0 * 256.0), gl_FragColor);
}