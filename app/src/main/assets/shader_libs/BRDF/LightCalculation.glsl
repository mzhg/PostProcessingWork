#ifndef RF_NUM_DIRECTION_LIGHT
#define RF_NUM_DIRECTION_LIGHT 0
#endif

#ifndef RF_NUM_SPOT_LIGHT
#define RF_NUM_SPOT_LIGHT 0
#endif

#ifndef RF_NUM_POINT_LIGHT
#define RF_NUM_POINT_LIGHT 0
#endif

#ifndef RF_ENABLE_NORMAL_MAP
#define RF_ENABLE_NORMAL_MAP 0
#endif

struct DirectionLightDesc
{
    vec4 Diffuse;     // rgb: diffuse color; a: unused
    vec4 Specular;    // rgb: specular color; a: unused
    vec4 Direction;   // xyz: light direction; w: unused
};

struct PointLightDesc
{
    vec4 Diffuse;    // rgb: diffuse color; a: unused
    vec4 Specular;   // rgb: specular color; a: unused
    vec4 Position;   // xyz: light position; w: unused
    vec4 Attenuation;  // xyz: d0, d1, d2; w: LightRange, if w < 0, meas infinity.
};

struct SpotLihtDesc
{
    vec4 Diffuse;     // rgb: diffuse color; a: cos(LightAngle)
    vec4 Specular;    // rgb: specular color; a: unused
    vec4 Position;    // xyz: light position; w: unused
    vec4 Direction;   // xyz: light direction; w: unused
    vec4 Attenuation; // xyz: d0, d1, d2; w: LightRange, if w < 0, meas infinity.
};

#if RF_NUM_DIRECTION_LIGHT > 0
uniform DirectionLightDesc g_Direction_Lights[RF_NUM_DIRECTION_LIGHT];
#endif

#if RF_NUM_POINT_LIGHT > 0
uniform PointLightDesc g_Point_Lights[RF_NUM_POINT_LIGHT];
#endif

#if RF_NUM_SPOT_LIGHT > 0
uniform SpotLihtDesc g_Spot_Lights[RF_NUM_SPOT_LIGHT];
#endif

uniform sampler2D g_Normal_Map;

vec3 get_normal()
{
#if RF_ENABLE_NORMAL_MAP
    vec3 normal = texture(g_Normal_Map, m_Texcoord).xyz * 2.0 - 1.0;

    // Build orthonormal basis.
    vec3 N = normalize(m_NormalWS);
    vec3 T = normalize(m_TangentWS - dot(m_NormalWS, N) * N);
    vec3 B = cross(N,T);

    mat3 TBN = mat3(T,B,N);
    return TBN * normal;
#else
    return normalize(m_NormalWS);
#endif
}

void compute_direction_light(inout vec3 diffuse_accum, inout vec3 specular_accum, float shiness)
{
#if RF_NUM_DIRECTION_LIGHT > 0
    vec3 N = get_normal();
    vec3 V = normalize(g_Eye_Pos - m_PositionWS.xyz);

    for(int i = 0; i < RF_NUM_DIRECTION_LIGHT; i++)
    {
        vec3 L = normalize(-g_Direction_Lights[i].Direction.xyz);

        diffuse_accum += g_Direction_Lights[i].Diffuse.xyz * max(dot(N, L), 0.);

        vec3 H = normalize(L+V);
        specular_accum += g_Direction_Lights[i].Specular.xyz * pow(max(dot(N, H), 0.), shiness);
    }

#endif
}

void compute_spot_light(inout vec3 diffuse_accum, inout vec3 specular_accum, float shiness)
{
#if RF_NUM_SPOT_LIGHT > 0
    vec3 N = get_normal();
    vec3 V = normalize(g_Eye_Pos - m_PositionWS.xyz);

    for(int i = 0; i < RF_NUM_SPOT_LIGHT; i++)
    {
        float light_to_vertex_length;
        vec3 L = g_Spot_Lights[i].Position.xyz - m_PositionWS.xyz;
        light_to_vertex_length = length(L);
        L /= max(light_to_vertex_length, 0.00001);

        if(g_Spot_Lights[i].Attenuation.w > 0.0 && light_to_vertex_length > g_Spot_Lights[i].Attenuation.w)
        {
            // Out of the light range.
            continue;
        }

        float spot_cos = dot(-L, g_Spot_Lights[i].Direction.xyz);
        if(spot_cos < g_Spot_Lights[i].Diffuse.w)
        {
            continue;
        }

        float d0 = g_Spot_Lights[i].Attenuation.x;
        float d1 = g_Spot_Lights[i].Attenuation.y;
        float d2 = g_Spot_Lights[i].Attenuation.z;

        float attenuation = pow(spot_cos, g_Spot_Lights[i].Direction.w) / (d0 + d1 * light_to_vertex_length + d2 * light_to_vertex_length);

        diffuse_accum += g_Spot_Lights[i].Diffuse.xyz * max(dot(N, L), 0.0) * attenuation;

        vec3 H = normalize(L + V);
        specular_accum += g_Spot_Lights[i].Specular.xyz * pow(max(dot(H, N), 0.), shiness) * attenuation;
    }
#endif
}

void compute_point_light(inout vec3 diffuse_accum, inout vec3 specular_accum, float shiness)
{
#if RF_NUM_POINT_LIGHT > 0
    vec3 N = get_normal();
    vec3 V = normalize(g_Eye_Pos - m_PositionWS.xyz);

    for(int i = 0; i < RF_NUM_POINT_LIGHT; i++)
    {
        float light_to_vertex_length;
        vec3 L = g_Point_Lights[i].Position.xyz - m_PositionWS.xyz;
        light_to_vertex_length = length(L);
        L /= max(light_to_vertex_length, 0.00001);

        if(g_Point_Lights[i].Attenuation.w > 0.0 && light_to_vertex_length > g_Point_Lights[i].Attenuation.w)
        {
            continue;
        }

        float d0 = g_Point_Lights[i].Attenuation.x;
        float d1 = g_Point_Lights[i].Attenuation.y;
        float d2 = g_Point_Lights[i].Attenuation.z;

        float attenuation = 1. / (d0 + d1 * light_to_vertex_length + d2 * light_to_vertex_length);

        diffuse_accum += g_Point_Lights[i].Diffuse.xyz * max(dot(N, L), 0.0) * attenuation;

        vec3 H = normalize(L + V);
        specular_accum += g_Point_Lights[i].Specular.xyz * pow(max(dot(H, N), 0.), shiness) * attenuation;
    }
#endif
}

void compute_light(inout vec3 diffuse, inout vec3 specular, float shiness)
{
    diffuse = vec3(0);
    specular = vec3(0);

    compute_direction_light(diffuse, specular, shiness);
    compute_spot_light(diffuse, specular, shiness);
    compute_point_light(diffuse, specular, shiness);
}