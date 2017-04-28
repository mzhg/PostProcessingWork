#include "PostProcessingCommonPS.frag"

uniform vec2 g_TexelSize;
uniform sampler2D g_Texture;

#ifndef METHOD
#define METHOD 1
#endif

// METHOD: 0, fastest; 1, normal, 2; combined_depth.
void main()
{
#if METHOD == 0
	Out_f4Color = texture(g_Texture, m_f4UVAndScreenPos.xy);
#elif METHOD == 1
	vec2 texelLocation = m_f4UVAndScreenPos.xy;
	vec2 texelSampleLoc1 = texelLocation + vec2(-1, -1) * g_TexelSize;
	vec2 texelSampleLoc2 = texelLocation + vec2(+1, -1) * g_TexelSize;
	vec2 texelSampleLoc3 = texelLocation + vec2(-1, +1) * g_TexelSize;
	vec2 texelSampleLoc4 = texelLocation + vec2(+1, +1) * g_TexelSize;

	vec4 color1 = texture(g_Texture, texelSampleLoc1);
	vec4 color2 = texture(g_Texture, texelSampleLoc2);
	vec4 color3 = texture(g_Texture, texelSampleLoc3);
	vec4 color4 = texture(g_Texture, texelSampleLoc4);

	Out_f4Color = (color1 + color2 + color3 + color4) * 0.25;
#elif METHOD == 2
	vec2 texelLocation = m_f4UVAndScreenPos.xy;
	vec2 texelSampleLoc1 = texelLocation + vec2(-1, -1) * g_TexelSize;
	vec2 texelSampleLoc2 = texelLocation + vec2(+1, -1) * g_TexelSize;
	vec2 texelSampleLoc3 = texelLocation + vec2(-1, +1) * g_TexelSize;
	vec2 texelSampleLoc4 = texelLocation + vec2(+1, +1) * g_TexelSize;

	vec4 color1 = texture(g_Texture, texelSampleLoc1);
	vec4 color2 = texture(g_Texture, texelSampleLoc2);
	vec4 color3 = texture(g_Texture, texelSampleLoc3);
	vec4 color4 = texture(g_Texture, texelSampleLoc4);

	Out_f4Color.rgb = (color1.xyz + color2.xyz + color3.xyz + color4.xyz) * 0.25;
	Out_f4Color.a = texture(g_Texture, texture).a;
#endif
}