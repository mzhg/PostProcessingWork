#version 430

layout(points )in;
layout(points ,max_vertices = 1 )out;

flat in ivec3 cellIndex[];

flat out ivec3 GScellIndex;


void main(){
	/*for (int i = 0; i < gl_in.length(); i++)
	{
		gl_Position=gl_in[i].gl_Position;
		EmitVertex();
		EndPrimitive();
	}*/

		gl_Position=gl_in[0].gl_Position;
		gl_Layer = cellIndex[0].z;

		GScellIndex = cellIndex[0];

		EmitVertex();
		EndPrimitive();

}