#include "../PostProcessingCommon.glsl"

#if GL_ES
precision highp float;
#endif

#if ENABLE_IN_OUT_FEATURE
    in vec4 m_f4UVAndScreenPos;
    layout(location = 0, index = 0) out float Out_f4Color[8];  // TODO
#else
    varying vec4 m_f4UVAndScreenPos;
   #define Out_f4Color gl_FragColor
#endif

uniform vec4  g_Uniforms;
vec2 uvOffset = g_Uniforms.xy;
vec2 invResolution = g_Uniforms.zw;

uniform sampler2D g_LinearDepthTex;

//----------------------------------------------------------------------------------

void main() {
  vec2 uv = floor(gl_FragCoord.xy) * 4.0 + uvOffset + 0.5;
  uv *= invResolution;  
  
  vec4 S0 = textureGather(g_LinearDepthTex, uv, 0);
  vec4 S1 = textureGatherOffset(g_LinearDepthTex, uv, ivec2(2,0), 0);
 
  Out_f4Color[0] = S0.w;
  Out_f4Color[1] = S0.z;
  Out_f4Color[2] = S1.w;
  Out_f4Color[3] = S1.z;
  Out_f4Color[4] = S0.x;
  Out_f4Color[5] = S0.y;
  Out_f4Color[6] = S1.x;
  Out_f4Color[7] = S1.y;
}
