in vec2 TexCoord1;
in vec2 TexCoord2;
in vec2 TexCoord3;
in vec2 TexCoord4;

uniform sampler2D sampler;
out vec4 FragColor;
void main()
{
	   FragColor = (texture(sampler, TexCoord1) +
                    texture(sampler, TexCoord2) +
                    texture(sampler, TexCoord3) +
                    texture(sampler, TexCoord4))*0.25;
}