#include "WavePackets.glsl"

layout(location = 0) in vec2 In_Pos	/*: POSITION*/;  // (x,y) = world position of this vertex, z,w = direction of traveling

out vec2 Tex;
out vec3 Pos; // world space position

// takes a simple 2D vertex on the ground plane, offsets it along y by the land or water offset and projects it on screen
void main()
{
    Tex = 0.5*(In_Pos.xy+float2(1.0,1.0));
    float h = 0.001*(-3.5+80.0*textureLod(g_waterTerrainTex, Tex, 0.0).y);   // LinearSampler
    Pos = SCENE_EXTENT*0.5*float3(In_Pos.x, h, In_Pos.y);
    gl_Position = mul(float4(Pos,1.0), g_mWorldViewProjection);
}