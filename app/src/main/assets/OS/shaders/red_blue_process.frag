#version 310 es
precision mediump float;

in vec4 m_f4UVAndScreenPos;

out vec4 OutColor;
layout(binding = 0) uniform sampler2D g_SrcTexture0;
layout(binding = 1) uniform sampler2D g_SrcTexture1;

void main()
{
	vec3 left_rgb =  textureLod(g_SrcTexture0, m_f4UVAndScreenPos.xy, 0.0).rgb;
	vec3 right_rgb = textureLod(g_SrcTexture1, m_f4UVAndScreenPos.xy, 0.0).rgb;
	
	OutColor = vec4(left_rgb.r,right_rgb.gb, 1.0);
}