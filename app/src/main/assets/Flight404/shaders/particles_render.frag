#version 330

in vec4 color;
uniform sampler2D sprite_texture;
layout (location = 0) out vec4 fragColor;

void main( void)
{
	fragColor = texture(sprite_texture, gl_PointCoord) * color;
}