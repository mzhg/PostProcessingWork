#include "ocean_vessel.glsl"

layout(location = 0) out float4 OutColor;

in VS_OUT
{
//    float4 position : SV_Position;
    float3 world_normal /*: NORMAL*/;
    float3 world_pos /*: WORLD_POS*/;
    float3 eye_pos /*: EYE_POS*/;
    float2 rust_uv /*: TEXCOORD*/;
    float2 rustmap_uv /*: TEXCOORD2*/;
    float  model_z /*: Z*/;
}i;


void main()
{
    OutColor.r = gl_FragCoord.z;	        // r is set to depth
    OutColor.gba = float3(1.f);			// gba is set to 1.f to indicate 'occupied'
}