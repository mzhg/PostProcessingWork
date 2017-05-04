#version 300 es
precision highp float;
in vec4 Position;
in vec3 Normal;
in vec3 IncidentVector;
in vec2 texcoord;
uniform vec3 emission;
uniform vec4 color;
uniform samplerCube envMap;
uniform samplerCube envMapIrrad;
uniform sampler2D diffuseMap;

out vec4 FragColor;

float my_fresnel(vec3 I, vec3 N, float power,  float scale,  float bias)
{
    return bias + (pow(clamp(1.0 - dot(I, N), 0.0, 1.0), power) * scale);
}

void main()
{
    vec3 I = normalize(IncidentVector);
    vec3 N = normalize(Normal);
    vec3 R = reflect(I, N);
    float fresnel = my_fresnel(-I, N, 5.0, 1.0, 0.1);
    vec3 Creflect = texture(envMap, R).rgb;
	vec3 irrad = texture(envMapIrrad, N).rgb;
	vec3 diffuse = texture(diffuseMap, texcoord).rgb * color.a + color.rgb;
//	diffuse = max(vec3(1), diffuse);
	FragColor = vec4(mix(diffuse*irrad, Creflect, fresnel*color.a)+emission, 1.0);
//	gl_FragColor.xyz = max(vec3(1), gl_FragColor.xyz);
}