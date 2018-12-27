#version 430

layout(location = 0) in vec2 vPosition;

uniform sampler2D rsm_world_space_coords_tex;
uniform sampler2D rsm_normal_tex;
uniform mat4 mvp;
uniform int i_RSMsize;
uniform bool b_useNormalOffset;

out vec3 pos;

void main()
{
	ivec2 v_RSMCoords = ivec2(gl_VertexID % i_RSMsize, gl_VertexID / i_RSMsize);
	vec3 v_posFromRSM = texelFetch(rsm_world_space_coords_tex, v_RSMCoords,0).rgb;
	vec3 v_normFromRSM = texelFetch(rsm_normal_tex, v_RSMCoords,0).rgb;
	if(b_useNormalOffset)
		pos=v_posFromRSM + 0.5 * v_normFromRSM ;
	else
		pos=v_posFromRSM;
	gl_Position =  mvp * vec4(pos, 1.0);
}