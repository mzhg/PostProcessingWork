layout(location = 0) in vec4 In_Position;
layout(location = 1) in vec2 In_Texcoord;

out vec2 m_Texcoord;
out vec3 m_Normal;
layout(binding = 0) uniform sampler2D g_HeightMap;
layout(binding = 1) uniform sampler2D g_GradientMap;
uniform vec2 g_GridSize = vec2(1);
uniform mat4 g_MVP;

void main()
{
    m_Texcoord = In_Texcoord;

    float height= textureLod(g_HeightMap, m_Texcoord, 0.0).y;
    vec3 worldPos = vec3(In_Position.x, In_Position.z, height);

    vec2 gradient = textureLod(g_GradientMap, m_Texcoord, 0.0).rg;
    m_Normal = normalize(vec3(gradient.x, /*4.0/(1280.0/4.0)*/1, gradient.y));

    gl_Position = g_MVP * vec4(worldPos, 1);
}