#include "skybox.glsl"

layout(location = 0) out float4 OutColor;

in VS_OUTPUT
{
//    float4 Position	 : SV_Position;
    float3 EyeVec	 /*: TEXCOORD0*/;
    float3 PosWorld  /*: TEXCOORD1*/;
}Iutput;

void main()
{
    float3 n = normalize(In.EyeVec);
    float4 lower = texture(g_texSkyCube0, rotateXY(n,g_SkyCube0RotateSinCos));
    float4 upper = texture(g_texSkyCube1, rotateXY(n,g_SkyCube1RotateSinCos));
    float4 sky_color = g_SkyCubeMult * lerp(lower,upper,g_SkyCubeBlend);

    float zr = n.z * kEarthRadius;
    float distance_to_cloudbase = sqrt(zr * zr + 2.f * kEarthRadius * kCloudbaseHeight + kCloudbaseHeight * kCloudbaseHeight) - zr;
    distance_to_cloudbase = min(kMaxCloudbaseDistance,distance_to_cloudbase);

    float fog_factor = exp(distance_to_cloudbase*distance_to_cloudbase*g_FogExponent);
    fog_factor = kMinFogFactor + (1.f - kMinFogFactor) * fog_factor;
    sky_color.rgb = lerp(g_FogColor + g_LightningColor*0.5,sky_color.rgb,fog_factor);

    AtmosphereColorsType AtmosphereColors = CalculateAtmosphericScattering(In.EyeVec,g_LightPos, 15.0);
    float3 clear_color= AtmosphereColors.RayleighColor + AtmosphereColors.MieColor*5.0;
    float3 result = lerp(clear_color, sky_color, g_CloudFactor);
    OutColor = float4(result,1.0);
}