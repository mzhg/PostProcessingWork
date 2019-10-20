// Crest Ocean System

// This file is subject to the MIT License as seen in the root of this folder structure (LICENSE)

#if _PROCEDURALSKY_ON
uniform float3 _SkyBase, _SkyAwayFromSun, _SkyTowardsSun;
uniform float _SkyDirectionality;

float3 SkyProceduralDP(in const float3 i_refl, in const float3 i_lightDir)
{
    float dp = dot(i_refl, i_lightDir);
    
    if (dp > _SkyDirectionality)
    {
        dp = (dp - _SkyDirectionality) / (1. - _SkyDirectionality);
        return lerp(_SkyBase, _SkyTowardsSun, dp);
    }
    
    dp = (dp - -1.0) / (_SkyDirectionality - -1.0);
    return lerp(_SkyAwayFromSun, _SkyBase, dp);
}
#endif

#if _PLANARREFLECTIONS_ON
uniform sampler2D _ReflectionTex;
float _PlanarReflectionNormalsStrength;

void PlanarReflection(in const float4 i_screenPos, in const float3 i_n_pixel, inout float3 io_colour)
{
    float4 screenPos = i_screenPos;
    screenPos.xy += _PlanarReflectionNormalsStrength * i_n_pixel.xz;
    float4 refl = tex2Dproj(_ReflectionTex, UNITY_PROJ_COORD(screenPos));
    io_colour = lerp(io_colour, refl.rgb, refl.a);
}
#endif // _PLANARREFLECTIONS_ON

#if _OVERRIDEREFLECTIONCUBEMAP_ON
samplerCUBE _ReflectionCubemapOverride;
#endif // _OVERRIDEREFLECTIONCUBEMAP_ON

uniform float _Specular;
uniform float _FresnelPower;
uniform float  _RefractiveIndexOfAir;
uniform float  _RefractiveIndexOfWater;


#if _COMPUTEDIRECTIONALLIGHT_ON
uniform float _DirectionalLightFallOff;
uniform float _DirectionalLightBoost;
#endif

float CalculateFresnelReflectionCoefficient(float cosTheta)
{
    // Fresnel calculated using Schlick's approximation
    // See: http://www.cs.virginia.edu/~jdl/bib/appearance/analytic%20models/schlick94b.pdf
    // reflectance at facing angle
    float R_0 = (_RefractiveIndexOfAir - _RefractiveIndexOfWater) / (_RefractiveIndexOfAir + _RefractiveIndexOfWater); R_0 *= R_0;
    const float R_theta = R_0 + (1.0 - R_0) * pow(max(0.,1.0 - cosTheta), _FresnelPower);
    return R_theta;
}

void ApplyReflectionSky(in const float3 i_view, in const float3 i_n_pixel, in const float3 i_lightDir, in const float i_shadow, in const float4 i_screenPos, in const float i_weight, inout float3 io_col)
{
    // Reflection
    float3 refl = reflect(-i_view, i_n_pixel);
    // Dont reflect below horizon
    refl.y = max(refl.y, 0.0);

    float3 skyColour;


    #if _PROCEDURALSKY_ON
    // procedural sky cubemap
    skyColour = SkyProceduralDP(refl, i_lightDir);
    #else

    // sample sky cubemap
    #if _OVERRIDEREFLECTIONCUBEMAP_ON
    // User-provided cubemap
    float4 val = texCUBE(_ReflectionCubemapOverride, refl);
    skyColour = val.rgb;
    #else
    // Unity specular reflection cubemap
    float4 val = UNITY_SAMPLE_TEXCUBE_LOD(unity_SpecCube0, refl, 0.);
    skyColour = DecodeHDR(val, unity_SpecCube0_HDR);
    #endif

    #endif

    // Override with anything in the planar reflections
    #if _PLANARREFLECTIONS_ON
    PlanarReflection(i_screenPos, i_n_pixel, skyColour);
    #endif

    // Add primary light
    #if _COMPUTEDIRECTIONALLIGHT_ON
    skyColour += pow(max(0., dot(refl, i_lightDir)), _DirectionalLightFallOff) * _DirectionalLightBoost * _LightColor0 * i_shadow;
    #endif

    // Fresnel
    float R_theta = CalculateFresnelReflectionCoefficient(max(dot(i_n_pixel, i_view), 0.0));
    io_col = lerp(io_col, skyColour, R_theta * _Specular * i_weight);
}

#if _UNDERWATER_ON
void ApplyReflectionUnderwater(in const float3 i_view, in const float3 i_n_pixel, in const float3 i_lightDir, in const float i_shadow, in const float4 i_screenPos, float3 scatterCol, in const float i_weight, inout float3 io_col)
{
    const float3 underwaterColor = scatterCol;
    // The the angle of outgoing light from water's surface
    // (whether refracted form outside or internally reflected)
    const float cosOutgoingAngle = max(dot(i_n_pixel, i_view), 0.);

    // calculate the amount of incident light from the outside world (io_col)
    {
        // have to calculate the incident angle of incoming light to water
        // surface based on how it would be refracted so as to hit the camera
        const float cosIncomingAngle = cos(asin(clamp( (_RefractiveIndexOfWater * sin(acos(cosOutgoingAngle))) / _RefractiveIndexOfAir, -1.0, 1.0) ));
        const float reflectionCoefficient = CalculateFresnelReflectionCoefficient(cosIncomingAngle) * i_weight;
        io_col *= (1.0 - reflectionCoefficient);
        io_col = max(io_col, 0.0);
    }

    // calculate the amount of light reflected from below the water
    {
        // angle of incident is angle of reflection
        const float cosIncomingAngle = cosOutgoingAngle;
        const float reflectionCoefficient = CalculateFresnelReflectionCoefficient(cosIncomingAngle) * i_weight;
        io_col += (underwaterColor * reflectionCoefficient);
    }
}
#endif
