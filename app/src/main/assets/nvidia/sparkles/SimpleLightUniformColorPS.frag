#version 110

#ifdef GL_ES
precision highp float;
#endif

varying vec3 m_PositionWS;
varying vec3 m_NormalWS;
varying vec2 m_Texcoord;

uniform sampler2D g_InputTex;

uniform vec4 g_LightPos;   // w==0, means light direction, must be normalized

uniform vec3 g_LightAmbient;   // Ia
uniform vec3 g_LightDiffuse;   // Il
uniform vec3 g_LightSpecular;  // rgb = Cs * Il,

uniform vec3 g_MaterialAmbient;   // ka
uniform vec3 g_MaterialDiffuse;   // kb
uniform vec4 g_MaterialSpecular;   // ks, w for power
uniform vec3 g_EyePos;
uniform vec4 g_Color;
uniform bool g_EnableLighting /*= true*/;

vec4 lit(float n_l, float r_v, vec4 C)
{
    vec3 color = g_LightAmbient * g_MaterialAmbient  // ambient term
                +g_LightDiffuse * C.rgb * g_MaterialDiffuse * max(n_l, 0.0)
                +g_MaterialSpecular.rgb * g_LightSpecular * pow(max(r_v, 0.0), g_MaterialSpecular.a);

    return vec4(color, C.a);
}

void main()
{
    vec4 C;
    vec4 texColor = texture2D(g_InputTex, m_Texcoord);
    C.rgb = (g_Color.rgb + texColor.rgb);
    C.a = max(g_Color.a, texColor.a);

    if(!g_EnableLighting)
    {
        gl_FragColor = C;
    }
    else
    {
        vec3 L;  // light direction
        if(g_LightPos.w == 0.0)
        {
            L = g_LightPos.xyz;
        }
        else
        {
            L = normalize(g_LightPos.xyz-m_PositionWS);
        }

        vec3 N = normalize(m_NormalWS);
        vec3 R = reflect(-L, N);
        vec3 V = normalize(g_EyePos - m_PositionWS);

        float n_l = dot(N,  L);
        float r_v = dot(R,  V);

        gl_FragColor = lit(n_l, r_v, C);
    }
}