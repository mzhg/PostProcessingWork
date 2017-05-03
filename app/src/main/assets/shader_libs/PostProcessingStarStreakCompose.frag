#include "PostProcessingCommonPS.frag"

uniform sampler2D g_Texture1;
uniform sampler2D g_Texture2;
uniform sampler2D g_Texture3;
uniform sampler2D g_Texture4;

void main()
{
    vec2 f2TexCoord = m_f4UVAndScreenPos.xy;
  vec4 color1 = max(texture(g_Texture1, f2TexCoord), texture(g_Texture2, f2TexCoord));
  vec4 color2 = max(texture(g_Texture3, f2TexCoord), texture(g_Texture4, f2TexCoord));

  Out_f4Color = max(color1, color2);
//  gl_FragColor = min(vec4(256.0 * 256.0), gl_FragColor);
}