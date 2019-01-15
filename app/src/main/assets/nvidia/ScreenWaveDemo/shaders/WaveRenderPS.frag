layout(binding = 1) uniform sampler2D g_GradientMap;
layout(binding = 2) uniform sampler2D g_TexMap;

layout(location = 0) out vec4 Out_Color;

in vec2 m_Texcoord;
in vec3 m_Normal;

void main()
{
    vec3 L = normalize(vec3(1, 9, 1));  // Light Direction
    vec3 V = vec3(0,1, 0);
    vec3 N = normalize(m_Normal);
//    vec2 gradient = textureLod(g_GradientMap, m_Texcoord, 0.0).rg;
//    vec3 N = normalize(vec3(gradient.x, /*4.0/(1280.0/4.0)*/1, gradient.y));

    vec3 R = reflect(-L, N);

    vec3 specular = pow(max(0.0, dot(R, V)), 128.0) * vec3(1);
    vec3 diffuse = max(0.0, dot(N, L)) * vec3(1);
//    float maxValue = max(specular.r, max(specular.g, specular.b));
//    specular /= max(maxValue, 0.00001);
    float F = 1.33;
    vec3 T = refract(-V, N, F);

    vec3 refractColor = texture(g_TexMap, m_Texcoord + 0.2 * T.xz).rgb;

    float F0 = (F-1)/(F+1);
    F0 = F0 * F0;
    float fresel =clamp(F0 + (1-F0)*(1-max(dot(L, N), 0.0)), 0.0, 1.0);

//    Out_Color = vec4(mix(diffuse + specular * 0.5, refractColor, 1-fresel), 1);
    Out_Color = vec4(refractColor * max(diffuse + specular * 0.5, vec3(0.0)), 1);
    return;
//    Out_Color = vec4(specular, 1);
    Out_Color = vec4(vec3(refractColor + clamp(specular - 0.5, 0.0, 1.0)), 1);
    Out_Color = vec4(refractColor, 1);
}