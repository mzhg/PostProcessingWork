#version 400

out vec4 m_f4UVAndScreenPos;
out int m_fInstID;

out gl_PerVertex
{
	vec4 gl_Position;
};

void main()
{	
	int id = gl_VertexID;
	
	m_f4UVAndScreenPos.xy = vec2((id << 1) & 2, id & 2);
	gl_Position = vec4(m_f4UVAndScreenPos.xy * vec2(2,2) + vec2(-1,-1), 0, 1);
	m_f4UVAndScreenPos.zw = gl_Position.xy;
	
	m_fInstID = gl_InstanceID;
}