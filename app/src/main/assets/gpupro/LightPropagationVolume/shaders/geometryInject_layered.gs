#version 430

layout(points )in;
layout(points ,max_vertices = 1 )out;

flat in ivec3 v_volumeCellIndex[];
in vec3 v_posFromRSM[];
in vec3 v_normalFromRSM[];
in float surfelArea[];

out vec3 v_GSposFromRSM;
out vec3 v_GSnormalFromRSM;
out float GSsurfelArea;

void main(){
	gl_Position=gl_in[0].gl_Position;
	gl_Layer = v_volumeCellIndex[0].z;

	v_GSposFromRSM = v_posFromRSM[0];
	v_GSnormalFromRSM = v_normalFromRSM[0];
	GSsurfelArea = surfelArea[0];

	EmitVertex();
	EndPrimitive();
}