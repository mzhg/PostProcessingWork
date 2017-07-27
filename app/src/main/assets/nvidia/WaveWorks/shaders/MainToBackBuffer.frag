#version 130

in vec2 v_texcoords;
uniform sampler2D u_texture;
uniform float g_MainBufferSizeMultiplier = 1.1;

// layout (location = 0) out vec4 color;

void main()
{
	vec2 texcoord = vec2((v_texcoords.x-0.5)/g_MainBufferSizeMultiplier+0.5,(v_texcoords.y-0.5)/g_MainBufferSizeMultiplier+0.5);
	gl_FragColor = texture(u_texture, texcoord);
}