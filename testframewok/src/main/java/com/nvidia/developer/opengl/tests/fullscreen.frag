#version 130

in vec2 v_texcoords;

uniform sampler2D u_texture;
    
void main()
{
//	gl_Position = vec4(QuadVertices[gl_VertexID], 0.0, 1.0);
//	v_texcoords = QuadTexCoordinates[gl_VertexID];
	gl_FragColor = texture(u_texture, v_texcoords);
}