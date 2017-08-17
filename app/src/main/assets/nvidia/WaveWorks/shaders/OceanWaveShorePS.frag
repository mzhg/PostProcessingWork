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

void main()
{
    float3 color;
    float3 normal;
    float fresnel_factor;
    float specular_factor;
    float scatter_factor;
    float3 refraction_color;
    float3 reflection_color;
    float4 disturbance_eyespace;


    float water_depth;

//    float3 water_vertex_positionWS = In.positionWS.xzy;
    float3 water_vertex_positionWS = ConvertToWorldPos(In.positionWS.xyz);

    float3 pixel_to_light_vector = normalize(g_LightPosition-water_vertex_positionWS);
    float3 pixel_to_eye_vector = normalize(g_CameraPosition-water_vertex_positionWS);

    GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES surface_attributes = GFSDK_WaveWorks_GetSurfaceAttributes(In.NV_ocean_interp);

    float3 gerstner_normal = float3(0.0,1.0,0.0);
    float gerstner_breaker = 0;
    float gerstner_foamtrail = 0;

    if(g_enableShoreEffects > 0)
    {
        if(In.gerstner_multiplier > 0)
        {
            GetGerstnerSurfaceAttributes( In.gerstner_sdfUV, In.world_pos_undisplaced.xy, gerstner_normal, gerstner_breaker, gerstner_foamtrail);
        }
        surface_attributes.normal = lerp(float3(0,1,0),surface_attributes.normal.xzy, 1.0-0.9*In.gerstner_multiplier*In.gerstner_multiplier); // Leaving just 10% of original open ocean normals in areas affected by shore waves
        surface_attributes.foam_turbulent_energy += gerstner_foamtrail*3.0;
        surface_attributes.foam_wave_hats += gerstner_breaker*15.0;				// 15.0*breaker so the breaker foam has rough edges

        // using PD normal combination
        normal = normalize(float3(surface_attributes.normal.xz*gerstner_normal.y + gerstner_normal.xz*surface_attributes.normal.y, surface_attributes.normal.y*gerstner_normal.y));
//        normal = normal.xzy;
        normal = ConvertToWorldPos(normal);
    }
    else
    {
//        normal = surface_attributes.normal.xzy;
        normal = ConvertToWorldPos(surface_attributes.normal);
    }

    float3 reflected_eye_to_pixel_vector=-pixel_to_eye_vector+2*dot(pixel_to_eye_vector,normal)*normal;

    // calculating pixel position in light space
    float4 positionLS = mul(float4(water_vertex_positionWS,1),g_LightModelViewProjectionMatrix);
    positionLS.xyz/=positionLS.w;
//    positionLS.x=(positionLS.x+1)*0.5;
//    positionLS.y=(1-positionLS.y)*0.5;
    positionLS.xyz = 0.5 * positionLS.xyz + 0.5;
    positionLS.z = min(0.99,positionLS.z);

    // calculating shadow multiplier to be applied to diffuse/scatter/specular light components
    float shadow_factor = texture(g_ShadowmapTexture,float3(positionLS.xy,positionLS.z* 0.995f));  // SamplerDepthAnisotropic

    // simulating scattering/double refraction: light hits the side of wave, travels some distance in water, and leaves wave on the other side
    // it's difficult to do it physically correct without photon mapping/ray tracing, so using simple but plausible emulation below

    // only the crests of water waves generate double refracted light
    scatter_factor = g_WaterScatterIntensity*
        // the waves that lie between camera and light projection on water plane generate maximal amount of double refracted light
        pow(max(0.0,dot(normalize(float3(pixel_to_light_vector.x,0.0,pixel_to_light_vector.z)),-pixel_to_eye_vector)),2.0)*
        // the slopes of waves that are oriented back to light generate maximal amount of double refracted light
        shadow_factor*pow(max(0.0,1.0-dot(pixel_to_light_vector,normal)),2.0);

    scatter_factor += g_WaterScatterIntensity*
        // the scattered light is best seen if observing direction is normal to slope surface
        max(0,dot(pixel_to_eye_vector,normal));//*

    // calculating fresnel factor
    float r=(1.0 - 1.33)*(1.0 - 1.33)/((1.0 + 1.33)*(1.0 + 1.33));
    fresnel_factor = r + (1.0-r)*pow(saturate(1.0 - dot(normal,pixel_to_eye_vector)),5.0);

    // calculating specular factor
    specular_factor=shadow_factor*pow(max(0,dot(pixel_to_light_vector,reflected_eye_to_pixel_vector)),g_WaterSpecularPower);

    // calculating disturbance which has to be applied to planar reflections/refractions to give plausible results
    disturbance_eyespace=mul(float4(normal.x,normal.z,0,0),g_ModelViewMatrix);

    float2 reflection_disturbance = float2(disturbance_eyespace.x,disturbance_eyespace.z)*0.06;
    float2 refraction_disturbance = float2(-disturbance_eyespace.x,disturbance_eyespace.y)*0.9*
        // fading out refraction disturbance at distance so refraction doesn't look noisy at distance
        (100.0/(100+length(g_CameraPosition-water_vertex_positionWS)));

    // picking refraction depth at non-displaced point, need it to scale the refraction texture displacement amount according to water depth
    float refraction_depth = GetRefractionDepth(gl_FragCoord.xy*g_ScreenSizeInv);
    refraction_depth = g_ZFar*g_ZNear / (g_ZFar-refraction_depth*(g_ZFar-g_ZNear));
    float4 vertex_in_viewspace = mul(float4(In.positionWS.xyz,1),g_ModelViewMatrix);
    water_depth = refraction_depth-abs(vertex_in_viewspace.z);

    if(water_depth < 0)
    {
        refraction_disturbance = float2(0);
    }
    water_depth = max(0,water_depth);
    refraction_disturbance *= min(1.0f,water_depth*0.03);

    // getting refraction depth again, at displaced point now
    refraction_depth = GetRefractionDepth(gl_FragCoord.xy*g_ScreenSizeInv+refraction_disturbance);
    refraction_depth = g_ZFar*g_ZNear / (g_ZFar-refraction_depth*(g_ZFar-g_ZNear));
    vertex_in_viewspace= mul(float4(In.positionWS.xyz,1),g_ModelViewMatrix);
    water_depth = max(water_depth,refraction_depth-abs(vertex_in_viewspace.z));
    water_depth = max(0,water_depth);
    float depth_damper = min(1,water_depth*3.0);
    float depth_damper_sss = min(1,water_depth*0.5);

    // getting reflection and refraction color at disturbed texture coordinates
    reflection_color = textureLod(g_ReflectionTexture,float2(gl_FragCoord.x*g_ScreenSizeInv.x,1.0-gl_FragCoord.y*g_ScreenSizeInv.y)+reflection_disturbance,0).rgb;// SamplerLinearClamp
    refraction_color = textureLod(g_RefractionTexture,gl_FragCoord.xy*g_ScreenSizeInv+refraction_disturbance,0).rgb;

    // fading fresnel factor to 0 to soften water surface edges
    fresnel_factor*=depth_damper;

    // fading fresnel factor to 0 for rays that reflect below water surface
    fresnel_factor*= 1.0 - 1.0*saturate(-2.0*reflected_eye_to_pixel_vector.y);

    // applying water absorbtion according to distance that refracted ray travels in water
    // note that we multiply this by 2 since light travels through water twice: from light to seafloor then from seafloor back to eye
    refraction_color.r *= exp(-1.0*water_depth*2.0*g_WaterTransmittance.r);
    refraction_color.g *= exp(-1.0*water_depth*2.0*g_WaterTransmittance.g);
    refraction_color.b *= exp(-1.0*water_depth*2.0*g_WaterTransmittance.b);

    // applying water scatter factor
    refraction_color += scatter_factor*shadow_factor*g_WaterScatterColor*depth_damper_sss;

    // adding milkiness due to mixed-in foam
    refraction_color += g_FoamUnderwaterColor*saturate(surface_attributes.foam_turbulent_energy*0.2)*depth_damper_sss;

    // combining final water color
    color = lerp(refraction_color, reflection_color, fresnel_factor);
    // adding specular
    color.rgb += specular_factor*g_WaterSpecularIntensity*g_WaterSpecularColor*shadow_factor*depth_damper;

    // applying surface foam provided by turbulent energy

    // low frequency foam map
    float foam_intensity_map_lf = 1.0*texture(g_FoamIntensityTexture, In.world_pos_undisplaced.xy*0.04*float2(1,1)).x - 1.0;  // SamplerLinearWrap

    // high frequency foam map
    float foam_intensity_map_hf = 1.0*texture(g_FoamIntensityTexture, In.world_pos_undisplaced.xy*0.15*float2(1,1)).x - 1.0;

    // ultra high frequency foam map
    float foam_intensity_map_uhf = 1.0*texture(g_FoamIntensityTexture, In.world_pos_undisplaced.xy*0.3*float2(1,1)).x;

    float foam_intensity;
    foam_intensity = saturate(foam_intensity_map_hf + min(3.5,1.0*surface_attributes.foam_turbulent_energy-0.2));
    foam_intensity += (foam_intensity_map_lf + min(1.5,1.0*surface_attributes.foam_turbulent_energy));


    foam_intensity -= 0.1*saturate(-surface_attributes.foam_surface_folding);

    foam_intensity = max(0,foam_intensity);

    foam_intensity *= 1.0+0.8*saturate(surface_attributes.foam_surface_folding);

    float foam_bubbles = texture(g_FoamDiffuseTexture, In.world_pos_undisplaced.xy*0.5).r;  // SamplerLinearWrap
    foam_bubbles = saturate(5.0*(foam_bubbles-0.8));

    // applying foam hats
    foam_intensity += max(0,foam_intensity_map_uhf*2.0*surface_attributes.foam_wave_hats);

    foam_intensity = pow(foam_intensity, 0.7);
    foam_intensity = saturate(foam_intensity*foam_bubbles*1.0);

    foam_intensity*=depth_damper;

    // foam diffuse color
    float foam_diffuse_factor = max(0,0.8+max(0,0.2*dot(pixel_to_light_vector,surface_attributes.normal)));

    color = lerp(color, foam_diffuse_factor*float3(1.0,1.0,1.0),foam_intensity);

    // applying atmospheric fog to water surface
    float fog_factor = min(1,exp(-length(g_CameraPosition-water_vertex_positionWS)*g_FogDensity));
    color = lerp(color, CalculateFogColor(normalize(g_LightPosition),pixel_to_eye_vector).rgb, fresnel_factor*(1.0-fog_factor));

    // applying solid wireframe
    float d = min(In.v_dist.x,min(In.v_dist.y,In.v_dist.z));
    float I = exp2(-2.0*d*d);
    Out_f4Color = float4(color + g_Wireframe*I*0.5, 1.0);
}