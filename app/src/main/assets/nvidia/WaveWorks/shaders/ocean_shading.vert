#version 330 core

#define PATCH_BLEND_BEGIN		800.0f
#define PATCH_BLEND_END			20000.0f

layout (location  = 0 ) in vec2 vPos;

uniform mat4 g_matLocal;
uniform mat4 g_matWorldViewProj;

uniform float g_UVScale;
uniform float g_UVOffset;
uniform vec3 g_LocalEye;
uniform float g_PerlinSize;
uniform vec2 g_UVBase;
uniform vec2 g_PerlinMovement;
uniform vec3 g_PerlinOctave;
uniform vec3 g_PerlinAmplitude;

uniform sampler2D g_texPerlin;
uniform sampler2D g_texDisplacement;

out VS_OUTPUT
{
   vec2 TexCoord;
   vec3 LocalPos;
}Output;

void main()
{
    // Local position
    vec4 pos_local = g_matLocal * vec4(vPos, 0, 1);
    // UV
    vec2 uv_local = pos_local.xy * g_UVScale + g_UVOffset;
    
    // Blend displacement to avoid tiling artifact
	vec3 eye_vec = pos_local.xyz - g_LocalEye;
	float dist_2d = length(eye_vec.xy);
	float blend_factor = (PATCH_BLEND_END - dist_2d) / (PATCH_BLEND_END - PATCH_BLEND_BEGIN);
	blend_factor = clamp(blend_factor, 0.0, 1.0);

	// Add perlin noise to distant patches
	float perlin = 0.0;
	if (blend_factor < 1.0)
	{
		vec2 perlin_tc = uv_local * g_PerlinSize + g_UVBase;
		float perlin_0 = textureLod(g_texPerlin, perlin_tc * g_PerlinOctave.x + g_PerlinMovement, 0).w;
		float perlin_1 = textureLod(g_texPerlin, perlin_tc * g_PerlinOctave.y + g_PerlinMovement, 0).w;
		float perlin_2 = textureLod(g_texPerlin, perlin_tc * g_PerlinOctave.z + g_PerlinMovement, 0).w;
		
		perlin = perlin_0 * g_PerlinAmplitude.x + perlin_1 * g_PerlinAmplitude.y + perlin_2 * g_PerlinAmplitude.z;
	}
	
	// Displacement map
	vec3 displacement = vec3(0);
	if (blend_factor > 0.0)
		displacement = textureLod(g_texDisplacement, uv_local, 0.0).xyz;
	displacement = mix(vec3(0, 0, perlin), displacement, blend_factor);
	pos_local.xyz += displacement;
	
	// Transform
	gl_Position = g_matWorldViewProj * pos_local;
	
	Output.LocalPos = pos_local.xyz;
	
	// Pass thru texture coordinate
	Output.TexCoord = uv_local;
}