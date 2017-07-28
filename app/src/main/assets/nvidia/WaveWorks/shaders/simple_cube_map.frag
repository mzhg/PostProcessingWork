#version 110
varying vec3 TexCoord;
uniform samplerCube envMap;

void main()
{
	gl_FragColor = textureCube(envMap, TexCoord);
	//gl_FragColor.a = gl_FragCoord.z; // TODO ?
}