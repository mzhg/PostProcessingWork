#version 130

in vec2 v_texcoords;
uniform sampler2D u_texture;

// layout (location = 0) out vec4 color;

void main()
{
	gl_FragColor = texture(u_texture, v_texcoords);
	gl_FragColor.a = 0.0;
}