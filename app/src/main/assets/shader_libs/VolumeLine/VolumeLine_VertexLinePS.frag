#version 100

precision highp float;

varying vec2 Texcoord;

uniform mat4 MVP;
uniform float radius;
uniform sampler2D lineTexture;

void main()
{
	//just sample the texture
	gl_FragColor = texture2D(lineTexture,Texcoord);	
}

