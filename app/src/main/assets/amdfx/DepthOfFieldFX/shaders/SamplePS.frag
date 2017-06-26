#include "Sample_Common.glsl"

in float3 m_worldPos;
in float3 m_Normal;
in float2 m_TexCoord;

layout(location = 0) out float4 Out_f4Color;

void CalcDirectionalLight(in DirectionalLightInfo light, in float3 worldPos, in float3 normal, inout float3 lightColor)
{
    lightColor += saturate(dot(normal, -light.direction)) * light.color * g_Model.m_Diffuse.rgb;
    float3 lightReflect = normalize(reflect(light.direction, normal));
    float  spec         = saturate(dot(normalize(g_Viewer.m_Position - worldPos), lightReflect));
    float  specFactor   = pow(spec, light.specPower);
    lightColor += specFactor * light.color * g_Model.m_Specular.rgb;
}

void main()
{
    float4 texColor = float4(1, 1, 1, 1);  //
    texColor        = texture(g_t2dDiffuse, m_TexCoord);

    vec3 lightColor = vec3(0, 0, 0);

    DirectionalLightInfo directionalLight = DirectionalLightInfo( float3( -0.7943764, -0.32935333, 0.5103845 ), float3( 1.0, 0.7, 0.6 ), 50.0 );
    CalcDirectionalLight(directionalLight, m_worldPos, m_Normal, lightColor);

    vec3 ambient = vec3(0, 0, 0);
    ambient += lerp(float3(0.08, 0.08, 0.05), float3(0.09, 0.1, 0.33), m_Normal.y * 0.5 + 0.5);

    vec3 color = (lightColor + ambient);
    color *= texColor.xyz;

    Out_f4Color = float4(color, 1.0);
}