#include "../../../shader_libs/PostProcessingCommonPS.frag"

uniform sampler2D tex0;
//uniform sampler2D tex1;
uniform vec4 light_diffuse;
uniform vec4 material_diffuse;
uniform vec4 light_specular;
uniform vec4 material_specular;
uniform float material_shininess;

#if ENABLE_IN_OUT_FEATURE
in vec4 Vertex_UV;
in vec4 Vertex_Normal;
in vec4 Vertex_LightDir;
in vec4 Vertex_EyeVec;
#else
varying vec4 Vertex_UV;
varying vec4 Vertex_Normal;
varying vec4 Vertex_LightDir;
varying vec4 Vertex_EyeVec;
#endif

void main()
{
  vec2 uv = Vertex_UV.xy;
  uv.y *= -1.0;
  vec4 tex01_color = texture(tex0, uv).rgba;
  //vec4 noise_color = texture(tex1, uv).rgba;

  
  vec4 final_color = vec4(0.2, 0.15, 0.15, 1.0) * tex01_color; 
  vec4 N = normalize(Vertex_Normal);
  vec4 L = normalize(Vertex_LightDir);
  float lambertTerm = dot(N,L);
  if (lambertTerm > 0.0)
  {
    final_color += light_diffuse * material_diffuse * lambertTerm * tex01_color;	
    
    vec4 E = normalize(Vertex_EyeVec);
    vec4 R = reflect(-L, N);
    float specular = pow( max(dot(R, E), 0.0), material_shininess);
    final_color += light_specular * material_specular * specular;	
  }

  Out_f4Color.rgb = final_color.rgb;
  Out_f4Color.a = 1.0;
}