

layout(location = 0) out vec4 FragColor;

in vec3 m_WorldPos;

uniform vec4 g_CameraPos;   // w for the light range.
void main()
{
    FragColor = vec4(0);
    gl_FragDepth = length(m_WorldPos - g_CameraPos.xyz)/g_CameraPos.w;
}