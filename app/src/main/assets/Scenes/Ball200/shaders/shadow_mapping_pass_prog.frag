#include "../../../shader_libs/PostProcessingCommonPS.frag"

uniform sampler2DShadow shadow_map;
uniform sampler2D tex1;

uniform vec4 light_ambient;
uniform vec4 light_diffuse;
uniform vec4 material_diffuse;
uniform vec4 light_specular;
uniform vec4 material_specular;
uniform float light_inv_radius;

#if ENABLE_IN_OUT_FEATURE
in vec4 Vertex_UV;
in vec4 Vertex_Normal;
in vec4 Vertex_LightDir;
in vec4 Vertex_EyeVec;
in vec4 Vertex_ProjCoord;
#else
varying vec4 Vertex_UV;
varying vec4 Vertex_Normal;
varying vec4 Vertex_LightDir;
varying vec4 Vertex_EyeVec;
varying vec4 Vertex_ProjCoord;
#endif

void main()
{
  vec3 shadowUV = vec3(0.0);
  float shadowColor = 1.0;
  float distanceFromLight = 0.0;
  if (Vertex_ProjCoord.w > 0)
  {
	  shadowUV = Vertex_ProjCoord.xyz / Vertex_ProjCoord.w;
	  shadowUV = 0.5*shadowUV+0.5;
    
    float filter_scale = 0.001;
    vec3 filterTaps[12];
	  filterTaps[0] = vec3(0.617481, -0.613392, 0.0);
	  filterTaps[1] = vec3(-0.040254, 0.170019, 0.0);
	  filterTaps[2] = vec3(0.791925, -0.299417, 0.0);
	  filterTaps[3] = vec3(0.493210, 0.645680, 0.0);
	  filterTaps[4] = vec3(0.027070, 0.421003, 0.0);
	  filterTaps[5] = vec3(-0.271096, -0.817194, 0.0);
	  filterTaps[6] = vec3(-0.668203, -0.705374, 0.0);
	  filterTaps[7] = vec3(-0.108615, 0.977050, 0.0);
	  filterTaps[8] = vec3(0.142369, 0.063326, 0.0);
	  filterTaps[9] = vec3(0.326090, -0.667531, 0.0);
	  filterTaps[10] = vec3(-0.295755, -0.098422, 0.0);
	  filterTaps[11] = vec3(0.215369, -0.885922, 0.0);
	  int i = 0;
    shadowColor = 0.0;
    shadowUV.z -= 0.0001;
	  for (i=0; i<12; i++)
		  shadowColor += texture(shadow_map, shadowUV.xyz + filterTaps[i] * filter_scale);
	  shadowColor /= 12.0;
	  shadowColor = clamp(shadowColor, 0.0, 1.0);
    
    //shadowColor = texture(shadow_map, shadowUV.xyz);
  }

  vec2 uv = Vertex_UV.xy;
  uv.y *= -1.0;
  vec3 tex1_color = texture(tex1, uv).rgb;
  
  float distSqr = dot(Vertex_LightDir, Vertex_LightDir);
  float att = clamp(1.0 - light_inv_radius * sqrt(distSqr), 0.0, 1.0);
  vec4 L = Vertex_LightDir * inversesqrt(distSqr);  
  
  vec3 final_color = light_ambient.rgb * tex1_color; 
  vec4 N = normalize(Vertex_Normal);
  //vec4 L = normalize(Vertex_LightDir);
  float lambertTerm = dot(N,L);
  if (lambertTerm > 0.0)
  {
    final_color += light_diffuse.xyz * material_diffuse.xyz * lambertTerm * tex1_color;	
    
    float material_shininess = material_specular.a; 
    vec4 E = normalize(Vertex_EyeVec);    
    vec4 R = reflect(-L, N);
    float specular = pow(max(dot(R, E), 0.0), material_shininess);
    final_color += light_specular.xyz * material_specular.xyz * specular;	
  }

  Out_f4Color.rgb = final_color * shadowColor * att;
  
  // Gamma correction.
  //vec3 cc = final_color * shadowColor * att;
  //Out_Color.rgb = pow(cc.rgb, vec3(1.2));
  
  //Out_Color.rgb = tex1_color * shadowColor;
  //Out_Color.rgb = Vertex_UV.xyz;
  Out_f4Color.a = 1.0;
}