
uniform vec2 twoTexelSize;
out vec2 TexCoord1;
out vec2 TexCoord2;
out vec2 TexCoord3;
out vec2 TexCoord4;
void main()
{
  vec4 m_f4UVAndScreenPos;
  m_f4UVAndScreenPos.xy = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
  gl_Position = vec4(m_f4UVAndScreenPos.xy * 2.0 - 1.0, 0, 1);
  m_f4UVAndScreenPos.zw = gl_Position.xy;

  TexCoord1 = m_f4UVAndScreenPos.xy;
  TexCoord2 = m_f4UVAndScreenPos.xy + vec2(twoTexelSize.x, 0);
  TexCoord3 = m_f4UVAndScreenPos.xy + vec2(twoTexelSize.x, twoTexelSize.y);
  TexCoord4 = m_f4UVAndScreenPos.xy + vec2(0, twoTexelSize.y);
}