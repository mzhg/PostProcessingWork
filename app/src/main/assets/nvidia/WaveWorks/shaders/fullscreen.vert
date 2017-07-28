#version 130

const vec2 QuadVertices[4] = vec2[4]
(
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0)
);

const vec2 QuadTexCoordinates[4] = vec2[4]
(
    vec2(0.0, 1.0),
    vec2(1.0, 1.0),
    vec2(0.0, 0.0),
    vec2(1.0, 0.0)
);

out vec2 v_texcoords;
    
void main()
{
	gl_Position = vec4(QuadVertices[gl_VertexID], 0.0, 1.0);
	v_texcoords = QuadTexCoordinates[gl_VertexID];
	v_texcoords.y = 1.0 - v_texcoords.y;
}