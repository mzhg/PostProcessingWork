#version 330

layout (location = 0) out vec4 fragColor;
uniform sampler2D sprite_texture;

void main()
{
	fragColor = texture(sprite_texture, gl_PointCoord);
}