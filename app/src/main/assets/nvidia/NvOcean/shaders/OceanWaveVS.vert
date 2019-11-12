#include "ocean_surface.glsl"

layout(location = 0) in float4 In_f4Position;

out VS_OUTPUT
{
    float4								worldspace_position	/*: VSO */;
    float								hull_proximity /*: HULL_PROX*/;
}Output;

void main()
{
    GFSDK_WAVEWORKS_VERTEX_OUTPUT wvo = GFSDK_WaveWorks_GetDisplacedVertex(In_f4Position);

    Output.worldspace_position = float4(wvo.pos_world_undisplaced,0.0);

    Output.hull_proximity = 0.f;
    for(int i = 0; i != MaxNumVessels; ++i) {

        // Probably we could calc this elegantly, but easier to hard-code right now
        float mip_level = 6;

        // Sample the vessel hull profile and depress the surface where necessary
        float2 hull_profile_uv = g_HullProfileCoordOffsetAndScale[i].xy + wvo.pos_world.xy * g_HullProfileCoordOffsetAndScale[i].zw;
        float4 hull_profile_sample = textureLod(g_texHullProfileMap[i], hull_profile_uv, mip_level);  // g_samplerHullProfile

        float hull_profile_height = g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].x + g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].y * hull_profile_sample.x;
        Output.hull_proximity += hull_profile_sample.y;
    }
}