#include "ocean_surface_heights.glsl"

out float4 m_Color;

void main()
{
    int vID = gl_VertexID;

    int markerID = vID/12;
    int markerX = markerID%5;
    int markerY = markerID/5;

    float2 markerOffset = float2((float(markerX)-2.f)*kMarkerSeparation,(float(markerY)-2.f)*kMarkerSeparation);

    float3 world_vertex;

    float3 local_vertex = kMarkerCoords[vID%12];
    world_vertex.y = mul(local_vertex.xzy,(float3x3)g_matWorld).y;	// z offset comes from water lookup

    local_vertex.xy += markerOffset;
    world_vertex.xz = mul(float4(local_vertex.xzy,1),g_matWorld).xz;

    float2 lookup_uv = g_worldToUVScale.xy*rotate_2d(world_vertex.xz+g_worldToUVOffset.xy,g_worldToUVRot.xy);
    float3 lookup_value = g_texLookup.SampleLevel(g_samplerLookup,lookup_uv,0).xyz;
    world_vertex.y += lookup_value.z;

    gl_Position = mul(float4(world_vertex,1.f),g_matViewProj);
    m_Color = float4(((lookup_uv.x > 1.f && lookup_uv.y > 1.f) || (lookup_uv.x < 0.f && lookup_uv.y < 0.f)) ? float2(1,1) : float2(0,0),1.f, 1);
}