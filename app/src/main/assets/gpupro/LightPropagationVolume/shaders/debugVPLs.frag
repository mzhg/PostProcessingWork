#version 430

out vec4 pointColor;
in vec3 pos;

void main()
{
	//Temporary solution - must figure it out
	if(pos.y == 0.0 && pos.x == 0.0 && pos.z == 0.0)
		discard;
		
	pointColor = vec4(1.0,0.0,0.0,1.0);
}