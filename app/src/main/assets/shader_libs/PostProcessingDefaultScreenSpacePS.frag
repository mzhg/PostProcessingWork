#include "PostProcessingCommonPS.frag"

uniform sampler2D g_InputTex;

void main()
{
    Out_f4Color = texture(g_InputTex, m_f4UVAndScreenPos.xy);
}