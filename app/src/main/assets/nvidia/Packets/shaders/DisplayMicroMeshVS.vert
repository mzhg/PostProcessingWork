#include "WavePackets.glsl"

layout(location = 0) in vec2 In_Pos	/*: POSITION*/;  // (x,y) = world position of this vertex, z,w = direction of traveling

out vec2 Tex;
out vec3 Pos; // world space position

// takes a simple 2D vertex on the ground plane, offsets it along y by the land or water offset and projects it on screen
void main()
{
    Tex = In_Pos.xy;
    float4 pos = textureLod(g_waterPosTex, In_Pos, 0.0).xyzw;   // PointSampler
    if (pos.w <= 0.0)  // if no position data has been rendered to this texel, there is no water surface here
    {
        Pos = float3(0, 0, 0);
        gl_oPosition = float4(0, 0, 0, -1.0);
    }
    else
    {
        Pos = pos.xyz + textureLod(g_waterHeightTex, In_Pos, 0.0).xyz;  // LinearSampler
        gl_oPosition = mul(float4(Pos.xyz, 1.0), g_mWorldViewProjection);
    }
}