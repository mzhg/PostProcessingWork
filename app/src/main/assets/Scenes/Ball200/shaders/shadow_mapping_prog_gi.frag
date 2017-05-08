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
in vec4 Vertex_Color;
#else
varying vec4 Vertex_UV;
varying vec4 Vertex_Normal;
varying vec4 Vertex_LightDir;
varying vec4 Vertex_EyeVec;
varying vec4 Vertex_ProjCoord;
varying vec4 Vertex_Color;
#endif

void main()
{
  vec3 shadowUV = vec3(0.0);
  float shadowColor = 1.0;
  float distanceFromLight = 0.0;
  if (Vertex_ProjCoord.w > 0)
  {
	  shadowUV = Vertex_ProjCoord.xyz / Vertex_ProjCoord.w;
	  shadowUV = 0.5 * shadowUV + 0.5;
    
    float filter_scale = 0.001;
    
    vec2 poissonDisk[12];
	  poissonDisk[0] = vec2(0.617481, -0.613392);
	  poissonDisk[1] = vec2(-0.040254, 0.170019);
	  poissonDisk[2] = vec2(0.791925, -0.299417);
	  poissonDisk[3] = vec2(0.493210, 0.645680);
	  poissonDisk[4] = vec2(0.027070, 0.421003);
	  poissonDisk[5] = vec2(-0.271096, -0.817194);
	  poissonDisk[6] = vec2(-0.668203, -0.705374);
	  poissonDisk[7] = vec2(-0.108615, 0.977050);
	  poissonDisk[8] = vec2(0.142369, 0.063326);
	  poissonDisk[9] = vec2(0.326090, -0.667531);
	  poissonDisk[10] = vec2(-0.295755, -0.098422);
	  poissonDisk[11] = vec2(0.215369, -0.885922);
    /*
    vec2 poissonDisk[64];
    poissonDisk[0] = vec2(-0.613392, 0.617481);
    poissonDisk[1] = vec2(0.170019, -0.040254);
    poissonDisk[2] = vec2(-0.299417, 0.791925);
    poissonDisk[3] = vec2(0.645680, 0.493210);
    poissonDisk[4] = vec2(-0.651784, 0.717887);
    poissonDisk[5] = vec2(0.421003, 0.027070);
    poissonDisk[6] = vec2(-0.817194, -0.271096);
    poissonDisk[7] = vec2(-0.705374, -0.668203);
    poissonDisk[8] = vec2(0.977050, -0.108615);
    poissonDisk[9] = vec2(0.063326, 0.142369);
    poissonDisk[10] = vec2(0.203528, 0.214331);
    poissonDisk[11] = vec2(-0.667531, 0.326090);
    poissonDisk[12] = vec2(-0.098422, -0.295755);
    poissonDisk[13] = vec2(-0.885922, 0.215369);
    poissonDisk[14] = vec2(0.566637, 0.605213);
    poissonDisk[15] = vec2(0.039766, -0.396100);
    poissonDisk[16] = vec2(0.751946, 0.453352);
    poissonDisk[17] = vec2(0.078707, -0.715323);
    poissonDisk[18] = vec2(-0.075838, -0.529344);
    poissonDisk[19] = vec2(0.724479, -0.580798);
    poissonDisk[20] = vec2(0.222999, -0.215125);
    poissonDisk[21] = vec2(-0.467574, -0.405438);
    poissonDisk[22] = vec2(-0.248268, -0.814753);
    poissonDisk[23] = vec2(0.354411, -0.887570);
    poissonDisk[24] = vec2(0.175817, 0.382366);
    poissonDisk[25] = vec2(0.487472, -0.063082);
    poissonDisk[26] = vec2(-0.084078, 0.898312);
    poissonDisk[27] = vec2(0.488876, -0.783441);
    poissonDisk[28] = vec2(0.470016, 0.217933);
    poissonDisk[29] = vec2(-0.696890, -0.549791);
    poissonDisk[30] = vec2(-0.149693, 0.605762);
    poissonDisk[31] = vec2(0.034211, 0.979980);
    poissonDisk[32] = vec2(0.503098, -0.308878);
    poissonDisk[33] = vec2(-0.016205, -0.872921);
    poissonDisk[34] = vec2(0.385784, -0.393902);
    poissonDisk[35] = vec2(-0.146886, -0.859249);
    poissonDisk[36] = vec2(0.643361, 0.164098);
    poissonDisk[37] = vec2(0.634388, -0.049471);
    poissonDisk[38] = vec2(-0.688894, 0.007843);
    poissonDisk[39] = vec2(0.464034, -0.188818);
    poissonDisk[40] = vec2(-0.440840, 0.137486);
    poissonDisk[41] = vec2(0.364483, 0.511704);
    poissonDisk[42] = vec2(0.034028, 0.325968);
    poissonDisk[43] = vec2(0.099094, -0.308023);
    poissonDisk[44] = vec2(0.693960, -0.366253);
    poissonDisk[45] = vec2(0.678884, -0.204688);
    poissonDisk[46] = vec2(0.001801, 0.780328);
    poissonDisk[47] = vec2(0.145177, -0.898984);
    poissonDisk[48] = vec2(0.062655, -0.611866);
    poissonDisk[49] = vec2(0.315226, -0.604297);
    poissonDisk[50] = vec2(-0.780145, 0.486251);
    poissonDisk[51] = vec2(-0.371868, 0.882138);
    poissonDisk[52] = vec2(0.200476, 0.494430);
    poissonDisk[53] = vec2(-0.494552, -0.711051);
    poissonDisk[54] = vec2(0.612476, 0.705252);
    poissonDisk[55] = vec2(-0.578845, -0.768792);
    poissonDisk[56] = vec2(-0.772454, -0.090976);
    poissonDisk[57] = vec2(0.504440, 0.372295);
    poissonDisk[58] = vec2(0.155736, 0.065157);
    poissonDisk[59] = vec2(0.391522, 0.849605);
    poissonDisk[60] = vec2(-0.620106, -0.328104);
    poissonDisk[61] = vec2(0.789239, -0.419965);
    poissonDisk[62] = vec2(-0.545396, 0.538133);
    poissonDisk[63] = vec2(-0.178564, -0.596057);
	  */
    int i = 0;
    shadowColor = 0.0;
    
    // Used to lower moir?? pattern and self-shadowing
    shadowUV.z -= 0.01;
    
	  for (i=0; i<12; i++)
		  shadowColor += texture(shadow_map, shadowUV.xyz + vec3(poissonDisk[i], 0) * filter_scale);
	  shadowColor /= 12.0;
	  shadowColor = clamp(shadowColor, 0.0, 1.0);
  }

  //vec2 uv = Vertex_UV.xy;
  //uv.y *= -1.0;
  
  vec4 N = normalize(Vertex_Normal);
  vec2 uv = vec2(0.5 * N.x + 0.5, - 0.5 * N.y - 0.5);
  vec3 tex1_color = texture(tex1, uv).rgb;
  
  
  float distSqr = dot(Vertex_LightDir, Vertex_LightDir);
  float att = clamp(1.0 - light_inv_radius * sqrt(distSqr), 0.0, 1.0);
  vec4 L = Vertex_LightDir * inversesqrt(distSqr);  
  
  vec3 final_color = light_ambient.rgb * tex1_color; 
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

  Out_f4Color.rgb = final_color * shadowColor * att * Vertex_Color.rgb;
  
  // Gamma correction.
  //vec3 cc = final_color * shadowColor * att;
  //Out_Color.rgb = pow(cc.rgb, vec3(1.2));
  
  //Out_Color.rgb = tex1_color * shadowColor;
  //Out_Color.rgb = Vertex_UV.xyz;
  Out_f4Color.a = 1.0;
}
