in vec3 m_WorldPos;
in vec3 m_WorldNormal;

layout(binding = 0) uniform FrameCB
{
    mat4 g_ViewProj;
    mat4 g_Model;
    mat4 g_LightViewProj;
    vec4 g_LightPos;

    float g_LightZNear;
    float g_LightZFar;
//    float g_NormScale;
};

layout(binding = 0) uniform sampler2DArrayShadow tShadowmapArray;

uniform vec4 g_Color = vec4(1);

out vec4 OutColor;

vec3 ParaboloidProject(vec3 P, float zNear, float zFar)
{
	vec3 outP;
	float lenP = length(P.xyz);
	outP.xyz = P.xyz/lenP;
	outP.x = outP.x / (outP.z + 1);
	outP.y = outP.y / (outP.z + 1);
	outP.z = (lenP - zNear) / (zFar - zNear);
	outP.z = 2 * outP.z - 1;
	return outP;
}

vec3 tonemap(vec3 C)
{
	// Filmic -- model film properties
	C = max(vec3(0), C - 0.004);
	return (C*(6.2*C+0.5))/(C*(6.2*C+1.7)+0.06);
}

void main()
{
    vec3 P = m_WorldPos;
    vec3 N = normalize(m_WorldNormal);

    //return float4(0.5*(N+1), 1);

    const float SHADOW_BIAS = -0.001f;
    vec4 shadow_clip = g_LightViewProj *vec4(P,1);
    shadow_clip = shadow_clip / shadow_clip.w;
    int hemisphereID = (shadow_clip.z > 0.) ? 0 : 1;
    shadow_clip.z = abs(shadow_clip.z);
    shadow_clip.xyz = ParaboloidProject(shadow_clip.xyz, g_LightZNear, g_LightZFar);
    vec2 shadow_tc = 0.5 * shadow_clip.xy + 0.5f;
    float receiver_depth = 0.5 * shadow_clip.z + 0.5 +SHADOW_BIAS;

    float total_light = 0;
    const int SHADOW_KERNEL = 2;
//    [unroll]
    for (int ox=-SHADOW_KERNEL; ox<=SHADOW_KERNEL; ++ox)
    {
//        [unroll]
        for (int oy=-SHADOW_KERNEL; oy<=SHADOW_KERNEL; ++oy)
        {
            total_light += textureOffset(tShadowmapArray, vec4(shadow_tc, hemisphereID, receiver_depth), ivec2(ox, oy));
        }
    }
    float shadow_term = total_light / ((2.0*SHADOW_KERNEL+1.0) * (2.0*SHADOW_KERNEL+1.0));
    float light_to_world = length(P - g_LightPos.xyz);
    vec3 L = (g_LightPos.xyz - P) / light_to_world;
    float lambertTerm = max(dot(N,L), 0.0);

    OutColor = vec4(g_Color.rgb * shadow_term * lambertTerm, 1);

    /*vec3 final_color = vec3(0);

    if (lambertTerm > 0.0)
    {
        final_color += light_diffuse * material_diffuse * lambertTerm * tex01_color;

        vec4 E = normalize(Vertex_EyeVec);
        vec4 R = reflect(-L, N);
        float specular = pow( max(dot(R, E), 0.0), material_shininess);
        final_color += light_specular * material_specular * specular;
    }

    const vec4 c_vLightAttenuationFactors = vec4(1.0, 2.0, 1.0, 0.0);
    const vec3 c_vLightColor = vec3(25000.0, 23750.0, 22500.0);
    const vec3 c_vSigmaExtinction = vec3(0.002096, 0.0028239999, 0.00481);

    vec3 output_ = vec3(0,0,0);

    vec3 W = (g_LightPos.xyz - P)/max(light_to_world, 0.00001);
    float distance_attenuation = 1.0f/(c_vLightAttenuationFactors.x + c_vLightAttenuationFactors.y*light_to_world + c_vLightAttenuationFactors.z*light_to_world*light_to_world) + c_vLightAttenuationFactors.w;

    vec3 attenuation = vec3(distance_attenuation*shadow_term*dot(N, W));
    vec3 ambient = vec3(0.00001f*clamp(0.5f*(dot(N, W)+1.0f), 0., 1.));
    output_ += c_vLightColor*max(attenuation, ambient)*exp(-c_vSigmaExtinction*light_to_world);
    output_ = max(vec3(0), output_);

    OutColor = vec4(output_ * g_Color.rgb, 1);
    OutColor.rgb = tonemap(OutColor.rgb);*/
}