#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Attributes.glsl"
#include "ocean_surface.glsl"

layout(location=0) out vec4 Out_f4Color;

in PS_INPUT
{
//	float4								positionClip	 : SV_Position;
	GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT NV_ocean_interp;
	float3								displacementWS/*: TEXCOORD5*/;
	float3								positionWS/*: TEXCOORD6*/;
	float3								world_pos_undisplaced /*: TEXCOORD7*/;
	float3								gerstner_displacement /*: TEXCOORD8*/;
	float2								gerstner_sdfUV /*: TEXCOORD9*/;
	float								gerstner_multiplier /*: TEXCOORD10*/;
	noperspective float3				v_dist /*: TEXCOORD11*/;
}In;

float GetRefractionDepth(float2 position)
{
	return textureLod(g_RefractionDepthTextureResolved,position,0).r;  // SamplerLinearClamp
}

// primitive simulation of non-uniform atmospheric fog
float3 CalculateFogColor(float3 pixel_to_light_vector, float3 pixel_to_eye_vector)
{
	return lerp(g_AtmosphereDarkColor,g_AtmosphereBrightColor,0.5*dot(pixel_to_light_vector,-pixel_to_eye_vector)+0.5);
}

layout(binding = 8) uniform sampler1D g_texColorMap;
layout(binding = 9) uniform samplerCube g_texCubeMap;

const float3		g_BendParam = {0.1f, -0.4f, 0.2f};

const float3        g_SkyColor = {0.38f, 0.45f, 0.56f};
const float3		g_SunDir = {0.936016f, -0.343206f, 0.0780013f};
const float3		g_SunColor = {1.0f, 1.0f, 0.6f};
const float		g_Shineness = 20.0f;
const float3      g_WaterDeepColor={0.0,0.4,0.6};
//const float3      g_WaterScatterColor={0.0,0.7,0.6};
//const float3      g_WaterSpecularColor={1,0.8,0.5};
const float2      g_WaterColorIntensity={0.2,0.1};
//const float		g_WaterSpecularIntensity = 0.7f;

//const float3		g_FoamColor = {0.90f, 0.95f, 1.0f};
const float3		foam_underwater_color = {0.90f, 0.95f, 1.0f};

void main()
{
    GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES surface_attributes = GFSDK_WaveWorks_GetSurfaceAttributes(In.NV_ocean_interp);

    float fresnel_factor;
    float diffuse_factor;
    float specular_factor;
    float scatter_factor;

    float3 pixel_to_light_vector=g_SunDir;
    float3 pixel_to_eye_vector=surface_attributes.eye_dir;
    float3 reflected_eye_to_pixel_vector = reflect(-surface_attributes.eye_dir, surface_attributes.normal);


    float cos_angle = dot(surface_attributes.normal, surface_attributes.eye_dir);
    // ramp.x for fresnel term. ramp.y for atmosphere blending
    float3 ramp = texture(g_texColorMap, cos_angle).xyz;  // g_samplerColorMap
    // A worksaround to deal with "indirect reflection vectors" (which are rays requiring multiple
    // reflections to reach the sky).
    if (reflected_eye_to_pixel_vector.z < g_BendParam.x)
        ramp.y = lerp(ramp.y, g_BendParam.z, (g_BendParam.x - reflected_eye_to_pixel_vector.z)/(g_BendParam.x - g_BendParam.y));
    reflected_eye_to_pixel_vector.z = max(0, reflected_eye_to_pixel_vector.z);



    // simulating scattering/double refraction: light hits the side of wave, travels some distance in water, and leaves wave on the other side
    // it's difficult to do it physically correct without photon mapping/ray tracing, so using simple but plausible emulation below

    // only the crests of water waves generate double refracted light
    scatter_factor=0.01*max(0,In.displacementWS.z*0.001+0.3);

    // the waves that lie between camera and light projection on water plane generate maximal amount of double refracted light
    scatter_factor*=pow(max(0.0,dot(normalize(float3(pixel_to_light_vector.x,0.0,pixel_to_light_vector.z)),-pixel_to_eye_vector)),2.0);

    // the slopes of waves that are oriented back to light generate maximal amount of double refracted light
    scatter_factor*=pow(max(0.0,0.5-0.5*dot(pixel_to_light_vector,surface_attributes.normal)),3.0);

    // water crests gather more light than lobes, so more light is scattered under the crests
    scatter_factor+=2.0*g_WaterColorIntensity.y*max(0,In.displacementWS.z*0.001+0.3)*
        // the scattered light is best seen if observing direction is normal to slope surface
        max(0,dot(pixel_to_eye_vector,surface_attributes.normal));

    fresnel_factor = ramp.x;

    // calculating diffuse intensity of water surface itself
    diffuse_factor=g_WaterColorIntensity.x+g_WaterColorIntensity.y*max(0,dot(pixel_to_light_vector,surface_attributes.normal));

    float3 refraction_color=diffuse_factor*g_WaterDeepColor;

    // adding color that provide foam bubbles spread in water
    refraction_color += foam_underwater_color*saturate(surface_attributes.foam_turbulent_energy*0.2);

    // adding scatter light component
    refraction_color+=g_WaterScatterColor*scatter_factor;

    // reflection color
    float3 reflection_color = lerp(g_SkyColor,texture(g_texCubeMap, reflected_eye_to_pixel_vector).xyz, ramp.y);  // g_samplerCubeMap

    // applying Fresnel law
    float3 water_color = lerp(refraction_color,reflection_color,fresnel_factor);


    // applying surface foam provided by turbulent energy

    // low frequency foam map
    float foam_intensity_map_lf = texture(g_FoamIntensityTexture, In.world_pos_undisplaced.xy*0.04*float2(1,1)).x - 1.0;  // g_samplerTrilinear

    // high frequency foam map
    float foam_intensity_map_hf = texture(g_FoamIntensityTexture, In.world_pos_undisplaced.xy*0.15*float2(1,1)).x - 1.0;

    // ultra high frequency foam map
    float foam_intensity_map_uhf = texture(g_FoamIntensityTexture, In.world_pos_undisplaced.xy*0.3*float2(1,1)).x;

    float foam_intensity;
    foam_intensity = saturate(foam_intensity_map_hf + min(3.5,1.0*surface_attributes.foam_turbulent_energy-0.2));
    foam_intensity += (foam_intensity_map_lf + min(1.5,1.0*surface_attributes.foam_turbulent_energy));


    foam_intensity -= 0.1*saturate(-surface_attributes.foam_surface_folding);

    foam_intensity = max(0,foam_intensity);

    foam_intensity *= 1.0+0.8*saturate(surface_attributes.foam_surface_folding);

    float foam_bubbles = texture(g_FoamDiffuseTexture, In.world_pos_undisplaced.xy).r;
    foam_bubbles = saturate(5.0*(foam_bubbles-0.8));

    // applying foam hats
    foam_intensity += max(0,foam_intensity_map_uhf*2.0*surface_attributes.foam_wave_hats);//*(1.0 + surface_attributes.foam_surface_folding*0.5);

    foam_intensity = pow(foam_intensity, 0.7);
    foam_intensity = saturate(foam_intensity*foam_bubbles*1.0);// + 0.1*foam_bubbles*saturate(surface_attributes.foam_surface_folding)));


    // foam diffuse color
    float foam_diffuse_factor=max(0,0.8+max(0,0.2*dot(pixel_to_light_vector,surface_attributes.normal)));


    water_color = lerp(water_color,foam_diffuse_factor*float3(1.0,1.0,1.0),foam_intensity);

    // calculating specular factor
    reflected_eye_to_pixel_vector=-pixel_to_eye_vector+2.0*dot(pixel_to_eye_vector,surface_attributes.normal)*surface_attributes.normal;
    specular_factor=pow(max(0,dot(pixel_to_light_vector,reflected_eye_to_pixel_vector)),g_Shineness);

    // adding specular component
    //water_color+=g_WaterSpecularIntensity*specular_factor*g_WaterSpecularColor*/*fresnel_factor*/saturate(1.0-5.0*foam_intensity);

    Out_f4Color = float4(water_color, 1);
}