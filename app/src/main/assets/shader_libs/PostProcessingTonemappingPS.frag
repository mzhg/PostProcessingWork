#include "PostProcessingCommonPS.frag"

uniform sampler2D   g_SceneTex;
uniform sampler2D   g_BlurTex;
uniform sampler2D   g_LumTex;

uniform vec4 g_Uniforms;
#define blurAmount g_Uniforms.x
#define exposure   g_Uniforms.y
#define gamma      g_Uniforms.z

#ifndef EYE_ADAPATION
#define EYE_ADAPATION 0
#endif

float GetAverageSceneLuminance()
{
#if EYE_ADAPATION
//    float fAveLogLum = g_tex2DAverageLuminance.Load( int3(0,0,0) );
	float fAveLogLum = texture(g_LumTex, vec2(0)).r;
#else
    float fAveLogLum =  0.1;
#endif
    fAveLogLum = max(0.05, fAveLogLum); // Average luminance is an approximation to the key of the scene
    return fAveLogLum;
}

/*
uniform float blurAmount;
uniform float exposure;
uniform float gamma;
*/

const float A = 0.15;
const float B = 0.50;
const float C = 0.10;
const float D = 0.20;
const float E = 0.02;
const float F = 0.30;
const float W = 11.2;

vec3 filmicTonemapping(vec3 x)
{
  return ((x*(A*x+C*B)+D*E)/(x*(A*x+B)+D*F))-E/F;
}

float vignette(vec2 pos, float inner, float outer)
{
  float r = length(pos);
  r = 1.0 - smoothstep(inner, outer, r);
  return r;
}
void main()
{
    vec2 f2UV = m_f4UVAndScreenPos.xy;
    vec4 scene = texture(g_SceneTex, f2UV);
    vec4 blurred = texture(g_BlurTex, f2UV);
	float lum = GetAverageSceneLuminance();
    vec3 c = mix(scene.rgb, blurred.rgb, blurAmount);
    c = c * exposure/lum;
	c = c * vignette(m_f4UVAndScreenPos.zw, 0.55, 1.5);
	float ExposureBias = 1.0;
	c = filmicTonemapping(ExposureBias*c);
	vec3 whiteScale = 1.0/filmicTonemapping(vec3(W,W,W));
	c = c*whiteScale;
    c.r = pow(c.r, gamma);
    c.g = pow(c.g, gamma);
    c.b = pow(c.b, gamma);
	Out_f4Color = vec4(c, 1.0);
	Out_f4Color = min(vec4(256.0 * 256.0), gl_FragColor);
}