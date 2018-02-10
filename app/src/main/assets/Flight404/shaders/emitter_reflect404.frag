#version 330

layout (location = 0) out vec4 fragColor;
uniform sampler2D sprite_texture;

in vec2 gs_texCoord;
flat in vec4 gs_color;

void main()
{
	fragColor = texture(sprite_texture, gs_texCoord) * gs_color;
}