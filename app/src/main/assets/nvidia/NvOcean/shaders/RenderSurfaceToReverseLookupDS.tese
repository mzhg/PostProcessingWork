#include "ocean_surface_heights.glsl"

in HS_OUT
{
    float m_uv;
}I[];

out float4 m_Color;

layout(quads, equal_spacing, cw) in;
void main()
{
    float2 f2BilerpCoords = gl_TessCoord.xy;
    float2 uv01 = lerp(I[0].uv,I[1].uv,f2BilerpCoords.y);
    float2 uv23 = lerp(I[2].uv,I[3].uv,f2BilerpCoords.y);

    float2 water_in = lerp(uv01,uv23,f2BilerpCoords.x);

    float3 undisplaced_coords = GFSDK_WaveWorks_GetUndisplacedVertexWorldPosition(water_in);

    GFSDK_WAVEWORKS_VERTEX_OUTPUT water_out = GFSDK_WaveWorks_GetDisplacedVertexAfterTessellation(float4(undisplaced_coords,1.f),0.f,0.f,float3(1,0,0));

    float2 clip_uv = rotate_2d(water_out.pos_world.xy-g_clipToWorldOffset.xy,float2(g_clipToWorldRot.x,-g_clipToWorldRot.y)) * g_worldToClipScale.xy;

    float4 clip_pos = float4(2.f*clip_uv-1.f,0.5f,1.f);
    clip_pos.y = -clip_pos.y;
    gl_Position = clip_pos;
    m_Color = float4(undisplaced_coords.xy,water_out.pos_world.z, 1);
}
