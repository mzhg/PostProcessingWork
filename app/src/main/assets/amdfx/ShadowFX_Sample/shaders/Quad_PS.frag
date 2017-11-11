layout(binding = 0) uniform sampler2D g_Sampler;

layout(location = 0) out vec4 Out_f4Color;

in vec4 m_f4UVAndScreenPos;

void main()
{
	Out_f4Color = texelFetch(g_Sampler, ivec2(gl_FragCoord.xy), 0);
}