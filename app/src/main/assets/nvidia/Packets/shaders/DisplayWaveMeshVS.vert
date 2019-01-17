#include "WavePackets.glsl"

layout(location = 0) in vec2 In_Pos	/*: POSITION*/;  // (x,y) = world position of this vertex, z,w = direction of traveling

out float2 Tex;
out float3 Pos; // world space position

void main()
{
    Tex = float2(In_Pos.x, In_Pos.y);
    Pos = 0.5*SCENE_EXTENT*float3(In.pos.x, 0, In.pos.y);
    gl_Position = mul( float4(Pos.xyz,1.0), g_mWorldViewProjection);
}