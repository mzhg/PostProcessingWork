#include "ocean_surface.glsl"

out float4 OutColor;

in DS_OUTPUT
{
//    precise float4								pos_clip	 : SV_Position;
    GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT NV_ocean_interp;
    float3								world_displacement/*: TEXCOORD4*/;
    float3								world_pos_undisplaced/*: TEXCOORD5*/;
    float3								world_pos/*: TEXCOORD6*/;
    float3								eye_pos/*: TEXCOORD7*/;
    float2								wake_uv/*: TEXCOORD8*/;
    float2								foam_uv/*: TEXCOORD9*/;
    float                               penetration /*: PENETRATION*/;
}In;

void main()
{
    GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES surface_attributes = GFSDK_WaveWorks_GetSurfaceAttributes(In.NV_ocean_interp);

    float fresnel_factor;
    float diffuse_factor;
    float specular_factor;
    float scatter_factor;

    float lightning_diffuse_factor;
    float lightning_specular_factor;
    float lightning_scatter_factor;

    // applying wake normal to surface normal
    float4 wake = g_texWakeMap.Sample(g_samplerTrilinearClamp, In.wake_uv).rgba-float4(0.5,0.5,0.0,0.5);
    wake.rgb *= 2.0;
    wake.rgb = (mul(g_matWorldToShip,float4(wake.rbg,0.0))).rbg;

    if(g_bWakeEnabled) {
        surface_attributes.normal = normalize(surface_attributes.normal + (surface_attributes.normal.b/wake.b)*float3(wake.rg,0));
    }

    // fetching wake energy
    float4 wake_energy = g_bWakeEnabled ? g_texShipFoamMap.Sample(g_samplerTrilinearClamp, In.wake_uv) : 0.f;


    float3 pixel_to_lightning_vector=normalize(g_LightningPosition - In.world_pos);
    float3 pixel_to_light_vector=g_LightDir;
    float3 pixel_to_eye_vector=surface_attributes.eye_dir;
    float3 reflected_eye_to_pixel_vector = reflect(-surface_attributes.eye_dir, surface_attributes.normal);

    // Super-sample the fresnel term
    float3 ramp = float3(0.f);
//    [unroll]
    float4 attr36_ddx = ddx(In.NV_ocean_interp.nv_waveworks_attr36);
    float4 attr36_ddy = ddy(In.NV_ocean_interp.nv_waveworks_attr36);
    float4 attr37_ddx = ddx(In.NV_ocean_interp.nv_waveworks_attr37);
    float4 attr37_ddy = ddy(In.NV_ocean_interp.nv_waveworks_attr37);
    for(int sx = -FRESNEL_TERM_SUPERSAMPLES_RADIUS; sx <= FRESNEL_TERM_SUPERSAMPLES_RADIUS; ++sx)
    {
        float fx = float(sx)/float(FRESNEL_TERM_SUPERSAMPLES_INTERVALS);

        GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT ssx_interp = In.NV_ocean_interp;
        ssx_interp.nv_waveworks_attr36 += fx * attr36_ddx;
        ssx_interp.nv_waveworks_attr37 += fx * attr37_ddx;

//    [unroll]
        for(int sy = -FRESNEL_TERM_SUPERSAMPLES_RADIUS; sy <= FRESNEL_TERM_SUPERSAMPLES_RADIUS; ++sy)
        {
            float fy = float(sy)/float(FRESNEL_TERM_SUPERSAMPLES_INTERVALS);

            GFSDK_WAVEWORKS_INTERPOLATED_VERTEX_OUTPUT ssxy_interp = ssx_interp;
            ssxy_interp.nv_waveworks_attr36 += fy * attr36_ddy;
            ssxy_interp.nv_waveworks_attr37 += fy * attr37_ddy;

            GFSDK_WAVEWORKS_SURFACE_ATTRIBUTES ss_surface_attributes = GFSDK_WaveWorks_GetSurfaceAttributes(ssxy_interp);

            float cos_angle = dot(ss_surface_attributes.normal, surface_attributes.eye_dir);

            // ramp.x for fresnel term. ramp.y for atmosphere blending
            ramp += g_texColorMap.Sample(g_samplerColorMap, cos_angle).xyz;
        }
    }

    ramp *= 1.f/float(FRESNEL_TERM_SUPERSAMPLES_INTERVALS*FRESNEL_TERM_SUPERSAMPLES_INTERVALS);

    /* Disabled - contributes to objectionable shimmering
    // A worksaround to deal with "indirect reflection vectors" (which are rays requiring multiple
    // reflections to reach the sky).
    if (reflected_eye_to_pixel_vector.z < g_BendParam.x)
        ramp = lerp(ramp, g_BendParam.z, (g_BendParam.x - reflected_eye_to_pixel_vector.z)/(g_BendParam.x - g_BendParam.y));
        */

    reflected_eye_to_pixel_vector.z = max(0.f, reflected_eye_to_pixel_vector.z);
    ramp = saturate(ramp);

    // simulating scattering/double refraction
    scatter_factor=5.0*max(0,In.world_displacement.z*0.05+0.3);
    scatter_factor*=pow(max(0.0,dot(normalize(float3(pixel_to_light_vector.x,pixel_to_light_vector.y,0)),-pixel_to_eye_vector)),2.0);
    scatter_factor*=pow(max(0.0,0.5-0.5*dot(pixel_to_light_vector,surface_attributes.normal)),2.0);
    scatter_factor+=3.0*max(0,In.world_displacement.z*0.05+0.3)* max(0,dot(pixel_to_eye_vector,surface_attributes.normal));


    //scattering from lightning
    lightning_scatter_factor=5.0*max(0,In.world_displacement.z*0.05+0.3);
    lightning_scatter_factor*=pow(max(0.0,dot(normalize(float3(pixel_to_lightning_vector.x,pixel_to_lightning_vector.y,0)),-pixel_to_eye_vector)),2.0);
    lightning_scatter_factor*=pow(max(0.0,0.5-0.5*dot(pixel_to_lightning_vector,surface_attributes.normal)),2.0);
    lightning_scatter_factor+=3.0*max(0,In.world_displacement.z*0.05+0.3)*max(0,dot(pixel_to_eye_vector,surface_attributes.normal));


    // calculating fresnel factor
    //float r=(1.2-1.0)*(1.2-1.0)/(1.2+1.0);
    //fresnel_factor = max(0.0,min(1.0,  1.0/pow(r+(1.0-r)*dot(surface_attributes.normal,pixel_to_eye_vector),7.0)  ));

    //float r=(1.0 - 1.13)*(1.0 - 1.13)/(1.0 + 1.13);
    //fresnel_factor = r + (1.0-r)*pow(saturate(1.0 - dot(surface_attributes.normal,pixel_to_eye_vector)),4.0);

    fresnel_factor=ramp.x;

    if(g_bGustsEnabled) {
        // applying wind gust map
        // local gust map
        float gust_factor = g_texGustMap.Sample(g_samplerAnisotropic,(In.world_pos.xy + g_GustUV.xy)*0.0003).r;
        gust_factor *= g_texGustMap.Sample(g_samplerAnisotropic,(In.world_pos.xy + g_GustUV.zw)*0.001).r;

        // distant gusts kicking in at very steep angles
        gust_factor += 3.0*g_texGustMap.Sample(g_samplerAnisotropic,(In.world_pos.xy + g_GustUV.zw)*0.0001).r
        *saturate(10.0*(-pixel_to_eye_vector.z+0.05));

        fresnel_factor *= (1.0 - 0.4*gust_factor);
    }

    // calculating diffuse intensity of water surface itself
    diffuse_factor=0.3*max(0,dot(pixel_to_light_vector,surface_attributes.normal));
    lightning_diffuse_factor=max(0,dot(pixel_to_lightning_vector,surface_attributes.normal));

    float3 dynamic_lighting = g_LightColor;

    float3 surface_lighting = diffuse_factor * g_LightColor;

    for(int ix = 0; ix != g_LightsNum; ++ix) {
        float3 pixel_to_light = g_SpotlightPosition[ix].xyz - In.eye_pos;
        float3 pixel_to_light_nml = normalize(pixel_to_light);
        float beam_attn = saturate(1.f*(-dot(g_SpotLightAxisAndCosAngle[ix].xyz,pixel_to_light_nml)-g_SpotLightAxisAndCosAngle[ix].w)/(1.f-g_SpotLightAxisAndCosAngle[ix].w));
        beam_attn *= 1.f/dot(pixel_to_light,pixel_to_light);
        float shadow = 1.0f;
        #if ENABLE_SHADOWS
        if (beam_attn * dot(g_SpotlightColor[ix].xyz, g_SpotlightColor[ix].xyz) > 0.01f)
        {
            shadow = GetShadowValue(g_SpotlightResource[ix], g_SpotlightMatrix[ix], In.eye_pos.xyz);
        }
            #endif
        surface_lighting += beam_attn * g_SpotlightColor[ix].xyz * saturate(dot(pixel_to_light_nml,surface_attributes.normal)) * shadow;
        dynamic_lighting += beam_attn * g_SpotlightColor[ix].xyz * shadow;
    }

    surface_lighting += g_SkyColor + g_LightningColor;
    float3 refraction_color=surface_lighting*g_WaterDeepColor;

    // adding color that provide foam bubbles spread in water
    refraction_color += g_GlobalFoamFade*(surface_lighting*foam_underwater_color*saturate(surface_attributes.foam_turbulent_energy*0.2) + 0.1*wake_energy.r*surface_lighting);

    // adding scatter light component
    refraction_color += g_WaterScatterColor*scatter_factor*dynamic_lighting + g_WaterScatterColor*lightning_scatter_factor*g_LightningColor;

    // reflection color
    float3 cube_map_sample_vector = reflected_eye_to_pixel_vector;
    float3 refl_lower = g_texCubeMap0.Sample(g_samplerCubeMap, rotateXY(cube_map_sample_vector,g_SkyCube0RotateSinCos)).xyz;
    float3 refl_upper = g_texCubeMap1.Sample(g_samplerCubeMap, rotateXY(cube_map_sample_vector,g_SkyCube1RotateSinCos)).xyz;
    float3 cloudy_reflection_color = g_LightColor*lerp(g_SkyColor,g_SkyCubeMult.xyz * lerp(refl_lower,refl_upper,g_CubeBlend), ramp.y);

    AtmosphereColorsType AtmosphereColors;
    AtmosphereColors = CalculateAtmosphericScattering(reflected_eye_to_pixel_vector,g_LightDir, 15.0);
    float3 clear_reflection_color = AtmosphereColors.RayleighColor + AtmosphereColors.MieColor*5.0*(1.0f-g_CloudFactor);

    float3 reflection_color = lerp(clear_reflection_color,cloudy_reflection_color,g_CloudFactor);

    float2 reflection_disturbance_viewspace = mul(float4(surface_attributes.normal.x,surface_attributes.normal.y,0,0),g_matView).xz * 0.05;

    float2 reflectionCoords = In.pos_clip.xy * g_ScreenSizeInv.xy + reflection_disturbance_viewspace;

    float4 planar_reflection = g_texReflection.SampleLevel(g_samplerPointClamp, reflectionCoords,0);
    float reflectionFactor = 0;

    //if (planar_reflection.a)
    {
        float3 planar_reflection_pos = g_texReflectionPos.SampleLevel(g_samplerPointClamp, reflectionCoords, 0).xyz;

        float pixelDistance = dot(g_ViewForward.xzy, planar_reflection_pos.xyz - In.world_pos.xzy);
        pixelDistance = pixelDistance > 0 ? 1.0 : 0.0;

        reflected_eye_to_pixel_vector = normalize(reflected_eye_to_pixel_vector);
        float3 pixel_to_reflection = normalize(planar_reflection_pos - In.world_pos.xzy);

        reflectionFactor = max(dot(reflected_eye_to_pixel_vector.xzy, pixel_to_reflection), 0);
        reflectionFactor = min(pow(reflectionFactor, 8.0) * 8.0, 1.0) * pixelDistance;
    }

    reflection_color = lerp(reflection_color,planar_reflection.rgb * reflectionFactor, any(planar_reflection.a * reflectionFactor));

    //adding static foam map for ship
    surface_attributes.foam_turbulent_energy += 1.0*wake_energy.g*(1.0 + surface_attributes.foam_surface_folding*0.6-0.3);
    surface_attributes.foam_turbulent_energy += 1.0*wake_energy.b;

    if(g_bWakeEnabled) {
        surface_attributes.foam_surface_folding += 10.0*wake.a;
        surface_attributes.foam_wave_hats += 30.0*wake.a;
    }

    //adding local foam generated by spray (uses same UV as wake map and static foam map)
    surface_attributes.foam_turbulent_energy += 0.2*g_texLocalFoamMap.Sample(g_samplerTrilinearClamp, float2(In.foam_uv.x,1.0-In.foam_uv.y)).r;

    float hullFoamFactor = 0;

    /* NO NEED TO DO THIS SINCE WE HAVE SPRAY FOAM  SIMULATION - tim*/
    /* NO LOOKS LIKE WE STILL NEED THIS BUT TUNED DOWN A BIT - tim - after prolonged meditation*/
//    [unroll]
    for(int i = 0; i != MaxNumVessels; ++i) {

        // Sample the vessel hull profile and depress the surface where necessary
        float2 hull_profile_uv = g_HullProfileCoordOffsetAndScale[i].xy + In.world_pos.xy * g_HullProfileCoordOffsetAndScale[i].zw;
        float4 hull_profile_sample = g_texHullProfileMap[i].SampleLevel(g_samplerHullProfileBorder, hull_profile_uv, 6);
        float hull_profile_height = g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].x + g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].y * hull_profile_sample.x;
        float hull_profile_blend = hull_profile_sample.y;

        hullFoamFactor += pow(hull_profile_blend * 2.3f, 2.0f);
    }
    /* */

    // low frequency foam map
    float foam_intensity_map_lf = 1.0*g_texFoamIntensityMap.Sample(g_samplerTrilinear, In.world_pos_undisplaced.xy*0.1*float2(1,1)).x - 1.0;

    // high frequency foam map
    float foam_intensity_map_hf = 1.0*g_texFoamIntensityMap.Sample(g_samplerTrilinear, In.world_pos_undisplaced.xy*0.4*float2(1,1)).x - 1.0;

    // ultra high frequency foam map
    float foam_intensity_map_uhf = 1.0*g_texFoamIntensityMap.Sample(g_samplerTrilinear, In.world_pos_undisplaced.xy*0.7*float2(1,1)).x;

    float foam_intensity;
    foam_intensity = saturate(foam_intensity_map_hf + min(3.5,1.0*(surface_attributes.foam_turbulent_energy + hullFoamFactor) -0.2));

    foam_intensity += (foam_intensity_map_lf + min(1.5,1.0*surface_attributes.foam_turbulent_energy));

    foam_intensity -= 0.1*saturate(-surface_attributes.foam_surface_folding);

    foam_intensity = max(0,foam_intensity);

    foam_intensity *= 1.0+0.8*saturate(surface_attributes.foam_surface_folding);

    float foam_bubbles = g_texFoamDiffuseMap.Sample(g_samplerTrilinear, In.world_pos_undisplaced.xy*0.25).r;
    foam_bubbles = saturate(5.0*(foam_bubbles-0.8));

    // applying foam hats
    foam_intensity += max(0,foam_intensity_map_uhf*2.0*surface_attributes.foam_wave_hats);

    foam_intensity = pow(foam_intensity, 0.7);
    foam_intensity = saturate(foam_intensity*foam_bubbles*1.0);

    // foam diffuse color
    float foam_diffuse_factor=max(0,0.6+max(0,0.4*dot(pixel_to_light_vector,surface_attributes.normal)));
    float foam_lightning_diffuse_factor=max(0,0.6+max(0,0.4*dot(pixel_to_lightning_vector,surface_attributes.normal)));
    float3 foam_lighting = dynamic_lighting * foam_diffuse_factor + g_LightningColor*foam_lightning_diffuse_factor;
    foam_lighting += g_SkyColor;

    // fading reflection a bit in foamy areas
    reflection_color *= 1.0 - 0.5*foam_intensity;

    // applying Fresnel law
    float3 water_color = lerp(refraction_color,reflection_color,fresnel_factor);

    foam_intensity *= g_GlobalFoamFade;

    water_color = lerp(water_color,foam_lighting,foam_intensity);

    // applying specular
    specular_factor=0;//pow(max(0,dot(pixel_to_light_vector,reflected_eye_to_pixel_vector)),g_WaterSpecularPower)*g_LightColor;


    lightning_specular_factor=pow(max(0,dot(pixel_to_lightning_vector,reflected_eye_to_pixel_vector)),g_WaterLightningSpecularPower);

    water_color += g_WaterSpecularIntensity*(1.0-foam_intensity)*(specular_factor + lightning_specular_factor*g_LightningColor);

    float fog_factor = exp(dot(In.eye_pos,In.eye_pos)*g_FogExponent*g_CloudFactor*g_CloudFactor);

    // overriding output if simplified techniques are requested
    if(g_ShowFoamSim + g_ShowSpraySim >0)
    {
        float3 simple_water_diffuse_color = float3(0.1,0.1,0.1)*(0.2+0.8*fresnel_factor);
        float injected_foam = 0.2*texture(g_texLocalFoamMap, float2(In.foam_uv.x,1.0-In.foam_uv.y)).r;  // g_samplerTrilinearClamp
        simple_water_diffuse_color.r += injected_foam*injected_foam;

        if(g_ShowFoamSim>0)
        {
            simple_water_diffuse_color.g += saturate(0.4*surface_attributes.foam_turbulent_energy);
            simple_water_diffuse_color.b += saturate(0.4*surface_attributes.foam_wave_hats);
        }
        OutColor = float4(simple_water_diffuse_color,1.0);
        return;
    }

    OutColor = float4(lerp(g_SkyColor + g_LightningColor*0.5,water_color,fog_factor), 1);
}