#version 400 core

#define PATCH_BLEND_BEGIN		800.0f
#define PATCH_BLEND_END			20000.0f

// #define USE_CONST

layout (location = 0) out vec4 fragColor;

subroutine void Technique();
subroutine uniform Technique technique;

uniform vec3 g_LocalEye;
uniform float g_PerlinSize;
uniform vec2 g_UVBase;

uniform vec3 g_PerlinOctave;
uniform vec2 g_PerlinMovement;
uniform vec3 g_PerlinGradient;

uniform float g_TexelLength_x2;
uniform vec3 g_WaterbodyColor;
uniform vec3 g_BendParam;

uniform float g_Shineness;
uniform vec3 g_SunColor;
uniform vec3 g_SunDir;

uniform vec3 g_SkyColor;

uniform sampler2D g_texGradient;
uniform sampler2D g_texPerlin;
uniform sampler1D g_texFresnel;
uniform samplerCube g_samplerCube;

in VS_OUTPUT
{
   vec2 TexCoord;
   vec3 LocalPos;
}In;

//-----------------------------------------------------------------------------
// Name: OceanSurfPS
// Type: Pixel shader                                      
// Desc: Ocean shading pixel shader. Check SDK document for more details
//-----------------------------------------------------------------------------
subroutine (Technique) void OceanSurfPS()
{
   // Calculate eye vector.
	vec3 eye_vec = g_LocalEye - In.LocalPos;
	vec3 eye_dir = normalize(eye_vec);
	

	// --------------- Blend perlin noise for reducing the tiling artifacts

	// Blend displacement to avoid tiling artifact
	float dist_2d = length(eye_vec.xy);
	float blend_factor = (PATCH_BLEND_END - dist_2d) / (PATCH_BLEND_END - PATCH_BLEND_BEGIN);
	blend_factor = clamp(blend_factor * blend_factor * blend_factor, 0.0, 1.0);

	// Compose perlin waves from three octaves
	vec2 perlin_tc = In.TexCoord * g_PerlinSize + g_UVBase;
	vec2 perlin_tc0 = (blend_factor < 1.0) ? perlin_tc * g_PerlinOctave.x + g_PerlinMovement : vec2(0);
	vec2 perlin_tc1 = (blend_factor < 1.0) ? perlin_tc * g_PerlinOctave.y + g_PerlinMovement : vec2(0);
	vec2 perlin_tc2 = (blend_factor < 1.0) ? perlin_tc * g_PerlinOctave.z + g_PerlinMovement : vec2(0);

	vec2 perlin_0 = texture(g_texPerlin, perlin_tc0).xy;
	vec2 perlin_1 = texture(g_texPerlin, perlin_tc1).xy;
	vec2 perlin_2 = texture(g_texPerlin, perlin_tc2).xy;
	
	vec2 perlin = (perlin_0 * g_PerlinGradient.x + perlin_1 * g_PerlinGradient.y + perlin_2 * g_PerlinGradient.z);


	// --------------- Water body color

	// Texcoord mash optimization: Texcoord of FFT wave is not required when blend_factor > 1
	vec2 fft_tc = (blend_factor > 0) ? In.TexCoord : vec2(0);

	vec2 grad = texture(g_texGradient, fft_tc).xy;
	grad = mix(perlin, grad, blend_factor);

	// Calculate normal here.
	vec3 normal = normalize(vec3(grad, g_TexelLength_x2));
	// Reflected ray
	vec3 reflect_vec = reflect(-eye_dir, normal);
	// dot(N, V)
	float cos_angle = dot(normal, eye_dir);

	// A coarse way to handle transmitted light
	vec3 body_color = g_WaterbodyColor;


	// --------------- Reflected color

	// ramp.x for fresnel term. ramp.y for sky blending
	vec4 ramp = texture(g_texFresnel, cos_angle);
	// A workaround to deal with "indirect reflection vectors" (which are rays requiring multiple
	// reflections to reach the sky).
	if (reflect_vec.z < g_BendParam.x)
		ramp = mix(ramp, vec4(g_BendParam.z), (g_BendParam.x - reflect_vec.z)/(g_BendParam.x - g_BendParam.y));
	reflect_vec.z = max(0, reflect_vec.z);

	vec3 reflection = texture(g_samplerCube, reflect_vec).xyz;
	// Hack bit: making higher contrast
	reflection = reflection * reflection * 2.5f;

	// Blend with predefined sky color
	vec3 reflected_color = mix(g_SkyColor, reflection, ramp.y);

	// Combine waterbody color and reflected color
	vec3 water_color = mix(body_color, reflected_color, ramp.x);


	// --------------- Sun spots

	float cos_spec = clamp(dot(reflect_vec, g_SunDir), 0, 1);
	float sun_spot = pow(cos_spec, g_Shineness);
	water_color += g_SunColor * sun_spot;
	
	fragColor = vec4(water_color, 1.0);
}

//-----------------------------------------------------------------------------
// Name: WireframePS
// Type: Pixel shader                                      
// Desc:
//-----------------------------------------------------------------------------
subroutine (Technique) void WireframePS()
{
    fragColor = vec4(0.9f, 0.9f, 0.9f, 1);
}

void main()
{
    technique();
}