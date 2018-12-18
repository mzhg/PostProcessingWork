layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Normal;

layout(binding = 0) uniform FrameCB
{
    mat4 g_ViewProj;
    mat4 g_Model;
    mat4 g_LightViewProj;
    vec4 g_LightPos;

    float g_LightZNear;
    float g_LightZFar;
//    int   g_GenShadow;
};

out vec3 m_WorldPos;

void main()
{
    vec4 worldPos = g_Model * vec4(In_Position,1);
    m_WorldPos = worldPos.xyz;
}
