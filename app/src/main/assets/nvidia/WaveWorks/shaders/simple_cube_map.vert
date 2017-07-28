#version 110
attribute vec3 PosAttribute;
uniform mat4 uMVP;
varying vec3 TexCoord;

void main()
{
    TexCoord  = PosAttribute;
	gl_Position = uMVP * vec4(PosAttribute, 1.0);
}