#include "SceneRenderCommon.glsl"

in float3 m_f3PositionWS; //   : WS_POSITION;
in float3 m_f3Normal; //       : NORMAL;
in float2 m_f2TexCoord; //     : TEXCOORD0;

layout(location =0) out float4 Out_f4Color;

void main()
{
  float4 f4TextureColor = texture(g_t2dDiffuse, m_f2TexCoord);  // g_SampleLinear

  if (f4TextureColor.a <= 0.50) discard;

  float4 shadow = texelFetch(g_t2dShadowMask, int2(gl_FragCoord.xy), 0 ) * 0.4 + 0.6;

  float3 normal = normalize( m_f3Normal );

  float3 LightDir;
  LightDir = normalize(g_Light[0].m_Eye.xyz - m_f3PositionWS);

  float lightness;
  lightness = max(0, dot(normal, LightDir) );

  Out_f4Color.xyz  = g_Model.m_Ambient.xyz;
  Out_f4Color.xyz += lightness * g_Light[0].m_Color.xyz * g_Model.m_Diffuse.xyz * shadow.x;

  Out_f4Color.w    = 1.0f;
  Out_f4Color     *= f4TextureColor;
}