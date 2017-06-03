#version 400

out vec4 UVAndScreenPos;
out float m_fInstID;

out gl_PerVertex
{
	vec4 gl_Position;
};

void main()
{	
	int id = gl_VertexID;
	
	UVAndScreenPos.xy = vec2((id << 1) & 2, id & 2);
	gl_Position = vec4(UVAndScreenPos.xy * vec2(2,2) + vec2(-1,-1), 0, 1);
	UVAndScreenPos.zw = gl_Position.xy;
	
	m_fInstID = gl_InstanceID;
}