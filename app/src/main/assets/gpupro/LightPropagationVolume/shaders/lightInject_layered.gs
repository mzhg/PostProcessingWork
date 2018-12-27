#version 430

layout(points )in;
layout(points ,max_vertices = 1 )out;

flat in ivec3 v_volumeCellIndex[];
in vec3 v_posFromRSM[];
in vec3 v_normalFromRSM[];
in vec4 v_fluxFromRSM[];

out vec3 v_GSposFromRSM;
out vec3 v_GSnormalFromRSM;
out vec4 v_GSfluxFromRSM;


void main(){
	/*for (int i = 0; i < gl_in.length(); i++)
	{
		gl_Position=gl_in[i].gl_Position;
		EmitVertex();
		EndPrimitive();
	}*/

		gl_Position=gl_in[0].gl_Position;
		gl_Layer = v_volumeCellIndex[0].z;

		v_GSposFromRSM = v_posFromRSM[0];
		v_GSnormalFromRSM = v_normalFromRSM[0];
		v_GSfluxFromRSM = v_fluxFromRSM[0];

		EmitVertex();
		EndPrimitive();

}