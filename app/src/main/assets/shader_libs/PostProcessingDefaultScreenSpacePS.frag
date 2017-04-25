#include "PostProcessingCommon.glsl"

#if GL_ES
precision highp float;
#endif

#if ENABLE_IN_OUT_FEATURE
    in vec4 m_f4UVAndScreenPos;
#else
    varying vec4 m_f4UVAndScreenPos;
#endif

LAYOUT_LOC(0) out vec4 Out_f4Color;
uniform sampler2D g_InputTex;

void main()
{
    Out_f4Color = texture(g_InputTex, m_f4UVAndScreenPos.xy);
}

	

