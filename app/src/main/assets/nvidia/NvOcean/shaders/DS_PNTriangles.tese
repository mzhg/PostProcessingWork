#include "ocean_surface.glsl"

in VS_OUTPUT
{
    float4								worldspace_position	/*: VSO */;
    float								hull_proximity /*: HULL_PROX*/;
}I[];

out DS_OUTPUT
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
}Output;

in out HS_ConstantOutput
{
    float fTargetEdgeLength[3] /*: TargetEdgeLength*/;
    float fHullProxMult[3] /*: HullProxMult*/;
}HSConstantData;

layout(triangles, fractional_odd_spacing, cw) in;

void main()
{
    GFSDK_WAVEWORKS_VERTEX_OUTPUT NV_ocean = GFSDK_WaveWorks_GetDisplacedVertexAfterTessellation(I[0].worldspace_position, I[1].worldspace_position, I[2].worldspace_position, f3BarycentricCoords);

    float3 pos_world = NV_ocean.pos_world;
    Output.world_pos_undisplaced = NV_ocean.pos_world - NV_ocean.world_displacement;

    float fTargetEdgeLength = HSConstantData.fTargetEdgeLength[0] * f3BarycentricCoords.x / HSConstantData.fHullProxMult[0] +
    HSConstantData.fTargetEdgeLength[1] * f3BarycentricCoords.y / HSConstantData.fHullProxMult[1] +
    HSConstantData.fTargetEdgeLength[2] * f3BarycentricCoords.z / HSConstantData.fHullProxMult[2];

    // calculating texcoords for wake maps
    float2 wake_uv = mul(float4(NV_ocean.pos_world.x,0.0,NV_ocean.pos_world.y,1.0),g_matWorldToShip).xz;
    wake_uv *= g_WakeTexScale;
    wake_uv += g_WakeTexOffset;
    Output.wake_uv = wake_uv;

    float2 foam_uv = mul(float4(Output.world_pos_undisplaced.x,0.0,Output.world_pos_undisplaced.y,1.0),g_matWorldToShip).xz;
    foam_uv *= g_WakeTexScale;
    foam_uv += g_WakeTexOffset;
    Output.foam_uv = wake_uv;

    if(g_bWakeEnabled) {
        // fetching wakes
        float4 wake = g_texWakeMap.SampleLevel(g_samplerTrilinearClamp, wake_uv,0);

        // applying displacement added by wakes
        float3 wake_displacement;
        wake_displacement.z = 2.0*(wake.a-0.5);
        wake_displacement.xy = float2(0,0);

        NV_ocean.world_displacement.z += wake_displacement.z;
        NV_ocean.pos_world.z += wake_displacement.z;
        pos_world.z += wake_displacement.z;
    }

    for(int i = 0; i != MaxNumVessels; ++i) {

        // Calculate the mip level for sampling the hull profile (aim for one pixel per tessellated tri)
        // float mip_level = log2(fTargetEdgeLength/g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].z);

        // Use the most detailed mip data available
        float mip_level = 0;

        // Sample the vessel hull profile and depress the surface where necessary
        float2 hull_profile_uv = g_HullProfileCoordOffsetAndScale[i].xy + NV_ocean.pos_world.xy * g_HullProfileCoordOffsetAndScale[i].zw;
        float4 hull_profile_sample = g_texHullProfileMap[i].SampleLevel(g_samplerHullProfile, hull_profile_uv, mip_level);
        float hull_profile_height = g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].x + g_HullProfileHeightOffsetAndHeightScaleAndTexelSize[i].y * hull_profile_sample.x;
        float hull_profile_blend = hull_profile_sample.y;

        if(hull_profile_height < NV_ocean.pos_world.z && hull_profile_blend > 0.f)
        {
            Output.penetration += abs(pos_world.z - ((1.f-hull_profile_blend) * pos_world.z + hull_profile_blend * hull_profile_height));

            pos_world.z = (1.f-hull_profile_blend) * pos_world.z + hull_profile_blend * hull_profile_height;
        }
    }


    Output.NV_ocean_interp = NV_ocean.interp;
    gl_Position = mul(float4(pos_world,1), g_matViewProj);
    Output.world_displacement = NV_ocean.world_displacement;
    Output.world_pos = pos_world;
    Output.eye_pos = mul(float4(pos_world,1), g_matView).xyz;
}