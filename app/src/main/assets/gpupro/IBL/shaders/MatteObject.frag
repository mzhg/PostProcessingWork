layout(location = 0) out vec4 Out_Color;

layout(binding = 0) uniform samplerCube envMapIrrad;
in vec3 m_Normal;

void main()
{
    vec3 N = normalize(m_Normal);

    Out_Color =  textureLod(envMapIrrad, N, 0.0);
}