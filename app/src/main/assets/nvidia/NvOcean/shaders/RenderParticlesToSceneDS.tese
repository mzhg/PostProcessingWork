#include "ocean_spray.glsl"
layout(quads, equal_spacing, cw) in;

#define QUAD_INTERP(member) (f0*I[0].member + f1*I[1].member + f2*I[2].member + f3*I[3].member)

in HS_SCENE_PARTICLE_OUTPUT {
    float3 ViewPos                /*: ViewPos*/;
    float3 TextureUVAndOpacity    /*: TEXCOORD0*/;
// NOT USED float3 PSMCoords              : PSMCoords;
    float FogFactor               /*: FogFactor*/;
}I[];

out DS_SCENE_PARTICLE_OUTPUT {
//    float4 Position               : SV_Position;
    float3 ViewPos                /*: ViewPos*/;
    float3 TextureUVAndOpacity    /*: TEXCOORD0*/;
// NOT USED float3 PSMCoords              : PSMCoords;
    float FogFactor               /*: FogFactor*/;
    float3 Lighting               /*: LIGHTING*/;
}outvert;

void main()
{
    float2 f2BilerpCoords = gl_TessCoord.xy;

    float f0 = f2BilerpCoords.x * f2BilerpCoords.y;
    float f1 = (1.f-f2BilerpCoords.x) * f2BilerpCoords.y;
    float f2 = f2BilerpCoords.x * (1.f-f2BilerpCoords.y);
    float f3 = (1.f-f2BilerpCoords.x) * (1.f-f2BilerpCoords.y);

//    DS_SCENE_PARTICLE_OUTPUT outvert;
    outvert.ViewPos = QUAD_INTERP(ViewPos);
    outvert.TextureUVAndOpacity = QUAD_INTERP(TextureUVAndOpacity);
    outvert.FogFactor = QUAD_INTERP(FogFactor);
    gl_Position = mul(float4(outvert.ViewPos,1.f), g_matProj);

    ////////////////////////////////////////////////////////////////////////////////
    // Lighting calcs hoisted from PS from here...
    ////////////////////////////////////////////////////////////////////////////////

    // randomize ppsition in view space to reduce shading banding
    float displacementScale = RND_1d(outvert.TextureUVAndOpacity.xy / 10);
    outvert.ViewPos.z += (displacementScale * 2.0f - 1.0f) * 0.25f;

    // Fake ship AO
    float3 vessel_pos = mul(float4(outvert.ViewPos,1),g_worldToVessel).xyz;
    float ao_profile = 0.5f*kVesselWidth*saturate((0.5f*kVesselLength-abs(vessel_pos.z))/(kVesselWidth));
    float ao_horizontal_range = abs(vessel_pos.x) - ao_profile;
    float ao_vertical_range = vessel_pos.y;
    float ao = 0.2f+ 0.8f*(1.f -saturate(1.f-ao_horizontal_range/5.f)* saturate(1.f-ao_vertical_range/5.f));

    float3 dynamic_lighting = ao*(g_LightColor + g_AmbientColor*2.0) + g_LightningColor;

    for(int ix = 0; ix != g_LightsNum; ++ix) {
        float3 pixel_to_light = g_SpotlightPosition[ix].xyz - outvert.ViewPos;
        float3 pixel_to_light_nml = normalize(pixel_to_light);
        float beam_attn = saturate(1.f*(-dot(g_SpotLightAxisAndCosAngle[ix].xyz,pixel_to_light_nml)-g_SpotLightAxisAndCosAngle[ix].w)/(1.f-g_SpotLightAxisAndCosAngle[ix].w));
        beam_attn *= 1.f/dot(pixel_to_light,pixel_to_light);
        float shadow = 1.0f;
        #if ENABLE_SHADOWS
        if (beam_attn * dot(g_SpotlightColor[ix].xyz, g_SpotlightColor[ix].xyz) > 0.01f)
        {
            shadow = GetShadowValue(g_SpotlightResource[ix], g_SpotlightMatrix[ix], outvert.ViewPos.xyz, true);
        }
            #endif
        dynamic_lighting += beam_attn * g_SpotlightColor[ix].xyz * shadow;
    }

    outvert.Lighting = dynamic_lighting;
}