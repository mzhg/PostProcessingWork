#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Quadtree.glsl"
#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Attributes.glsl"

// User uniforms
/*layout(binding = 8)*/ uniform sampler2D g_texFoamIntensityMap;
/*layout(binding = 9)*/ uniform sampler2D g_texFoamDiffuseMap;

// User local variables
vec3		g_SkyColor = {0.38, 0.45, 0.56};
vec3		g_BendParam = {0.1, -0.4, 0.2};

vec3		g_SunDir = {0.936016, -0.343206, 0.0780013};
vec3      	g_WaterDeepColor={0.0,0.4,0.6};
vec3      	g_WaterScatterColor={0.0,0.7,0.6};
vec2      	g_WaterColorIntensity={0.2,0.1};

vec3		g_FoamColor = {0.90, 0.95, 1.0};
vec3		g_FoamUnderwaterColor = {0.90, 0.95, 1.0};

// in & out variables
in GFSDK_WAVEWORKS_VERTEX_OUTPUT VSOutput;

out	vec4  color;

void main()
{

	GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES surface_attributes = GFSDK_WaveWorks_GetSurfaceAttributes(VSOutput.interp);

	float fresnel_factor;
	float diffuse_factor;
	float specular_factor;
	float scatter_factor;

	vec3 pixel_to_light_vector=g_SunDir;
	vec3 pixel_to_eye_vector=surface_attributes.eye_dir;
	vec3 reflected_eye_to_pixel_vector = reflect(-surface_attributes.eye_dir, surface_attributes.normal);

	// simulating scattering/double refraction: light hits the side of wave, travels some distance in water, and leaves wave on the other side
	// it's difficult to do it physically correct without photon mapping/ray tracing, so using simple but plausible emulation below

	// only the crests of water waves generate double refracted light
	scatter_factor=1.0*max(0,VSOutput.world_displacement.z*0.001+0.3);


	// the waves that lie between camera and light projection on water plane generate maximal amount of double refracted light
	scatter_factor*=pow(max(0.0,dot(normalize(vec3(pixel_to_light_vector.x,0.0,pixel_to_light_vector.z)),-pixel_to_eye_vector)),2.0);

	// the slopes of waves that are oriented back to light generate maximal amount of double refracted light
	scatter_factor*=pow(max(0.0,0.5-0.5*dot(pixel_to_light_vector,surface_attributes.normal)),3.0);


	// water crests gather more light than lobes, so more light is scattered under the crests
	scatter_factor+=2.0*g_WaterColorIntensity.y*max(0,VSOutput.world_displacement.z*0.001+0.3)*
		// the scattered light is best seen if observing direction is normal to slope surface
		max(0,dot(pixel_to_eye_vector,surface_attributes.normal));


	// calculating fresnel factor
	float r=(1.0 - 1.13)*(1.0 - 1.13)/(1.0 + 1.13);
	fresnel_factor = r + (1.0-r)*pow(saturate(1.0 - dot(surface_attributes.normal,pixel_to_eye_vector)),4.0);

	// calculating diffuse intensity of water surface itself
	diffuse_factor=g_WaterColorIntensity.x+g_WaterColorIntensity.y*max(0,dot(pixel_to_light_vector,surface_attributes.normal));

	vec3 refraction_color=diffuse_factor*g_WaterDeepColor;

	// adding color that provide foam bubbles spread in water
	refraction_color += g_FoamUnderwaterColor*saturate(surface_attributes.foam_turbulent_energy*0.2);

	// adding scatter light component
	refraction_color+=g_WaterScatterColor*scatter_factor;

	// reflection color
	vec3 reflection_color = g_SkyColor;

	// fading reflection color to half if reflected vector points below water surface
	reflection_color.rgb *= 1.0 - 0.5*max(0.0,min(1.0,-reflected_eye_to_pixel_vector.z*4.0));

	// applying Fresnel law
	vec3 water_color = mix(refraction_color,reflection_color,fresnel_factor);

	// applying surface foam provided by turbulent energy

	// low frequency foam map
	float foam_intensity_map_lf = 1.0*texture(g_texFoamIntensityMap, VSOutput.pos_world_undisplaced.xy*0.04*vec2(1,1)).x - 1.0;

	// high frequency foam map
	float foam_intensity_map_hf = 1.0*texture(g_texFoamIntensityMap, VSOutput.pos_world_undisplaced.xy*0.15*vec2(1,1)).x - 1.0;

	// ultra high frequency foam map
	float foam_intensity_map_uhf = 1.0*texture(g_texFoamIntensityMap, VSOutput.pos_world_undisplaced.xy*0.3*vec2(1,1)).x;

	float foam_intensity;
	foam_intensity = saturate(foam_intensity_map_hf + min(3.5,1.0*surface_attributes.foam_turbulent_energy-0.2));
	foam_intensity += (foam_intensity_map_lf + min(1.5,1.0*surface_attributes.foam_turbulent_energy));


	foam_intensity -= 0.1*saturate(-surface_attributes.foam_surface_folding);

	foam_intensity = max(0,foam_intensity);

	foam_intensity *= 1.0+0.8*saturate(surface_attributes.foam_surface_folding);

	float foam_bubbles = texture(g_texFoamDiffuseMap, VSOutput.pos_world_undisplaced.xy).r;
	foam_bubbles = saturate(5.0*(foam_bubbles-0.8));

	// applying foam hats
	foam_intensity += max(0,foam_intensity_map_uhf*2.0*surface_attributes.foam_wave_hats);

	foam_intensity = pow(foam_intensity, 0.7);
	foam_intensity = saturate(foam_intensity*foam_bubbles*1.0);
	//

	// foam diffuse color
	float foam_diffuse_factor=max(0,0.8+max(0,0.2*dot(pixel_to_light_vector,surface_attributes.normal)));

	water_color = mix(water_color,foam_diffuse_factor*vec3(1.0,1.0,1.0),foam_intensity);

	color = vec4(water_color,0);
}
