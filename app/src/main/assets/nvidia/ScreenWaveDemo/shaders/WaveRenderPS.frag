
layout(binding = 2) uniform sampler2D g_TexMap;

layout(location = 0) out vec4 Out_Color;

in vec2 m_Texcoord;
in vec3 m_Normal;

void main()
{
    vec3 L = normalize(vec3(1, 9, 1));  // Light Direction
    vec3 V = vec3(0,1, 0);
    vec3 N = normalize(m_Normal);

    vec3 R = reflect(-L, N);

    vec3 specular = pow(max(0.0, dot(R, N)), 10.0) * vec3(1);
    vec3 T = refract(-V, N, 1.33);

    vec3 refractColor = texture(g_TexMap, m_Texcoord + 0.002 * T.xz).rgb;

    Out_Color = vec4(mix(specular, refractColor, 0.9-clamp(dot(L, N), 0.0, 1.0)), 1);
//    Out_Color = vec4(specular, 1);
}