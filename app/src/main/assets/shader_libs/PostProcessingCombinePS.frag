#include "PostProcessingCommonPS.frag"

uniform sampler2D g_Texture0;
uniform sampler2D g_Texture1;

uniform vec2 g_f2Intensity;

void main()
{
    vec2 f2UV = m_f4UVAndScreenPos.xy;

	vec4 f4Color0 = texture2D(g_Texture0, f2UV);
	vec4 f4Color1 = texture2D(g_Texture1, f2UV);

	Out_f4Color = f4Color0 * g_f2Intensity.x + f4Color1 * g_f2Intensity.y;
}