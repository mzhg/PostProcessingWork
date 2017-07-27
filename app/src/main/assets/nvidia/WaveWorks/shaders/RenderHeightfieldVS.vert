#version 400

layout (location = 0) in vec4 aPosition;        // position vector

out HSIn_Heightfield
{
   vec2 origin;
   vec2 size;
}_output;

void main()
{
	_output.origin = aPosition.xy;
	_output.size = aPosition.zw;
}