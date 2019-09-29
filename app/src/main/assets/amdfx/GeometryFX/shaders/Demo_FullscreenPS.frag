#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

/*cbuffer FullscreenConstantBuffer     : register(b0)
{
    uint    windowWidth;
    uint    windowHeight;
    uint    shadowMapWidth;
    uint    shadowMapHeight;
};*/

uniform uint4 g_Uniforms;

#define windowWidth g_Uniforms.x
#define windowHeight g_Uniforms.y
#define shadowMapWidth g_Uniforms.z
#define shadowMapHeight g_Uniforms.w

uint2 tea (uint2 v)
{
    uint sum=0u, delta=0x9e3779b9u;
    uint k[4] = { 0xA341316Cu, 0xC8013EA4u, 0xAD90777Du, 0x7E95761Eu };
    for (uint i = 0; i < 5; ++i)
    {
        sum += delta;
        v.x += ((v.y << 4)+k[0] )^(v.y + sum)^((v.y >> 5)+k[1] );
        v.y += ((v.x << 4)+k[2] )^(v.x + sum)^((v.x >> 5)+k[3] );
    }
    return v;
}

layout(binding = 0) uniform sampler2D    depthMap; //                                       : register(t0);
layout(location = 0) out float4 Out_Color;

//float4 FullscreenPS(float4 pos : SV_POSITION) : SV_Target
void main()
{
    float4 pos = gl_FragCoord;
    // shadow map pixels per output window pixels
    float dx = float (shadowMapWidth) / float (windowWidth);
    float dy = float (shadowMapHeight) / float (windowHeight);

    float scale = max (dx, dy);
    int scaledWidth = int(shadowMapWidth / scale);
    int scaledHeight = int(shadowMapHeight / scale);

    int2 windowCenter = int2 (windowWidth, windowHeight) / 2;
    int2 shadowMapCenter = int2 (scaledWidth, scaledHeight) / 2;
    int2 centerDelta = shadowMapCenter - windowCenter;

    // Jitter inside the pixel footprint
    float sx = 0, sy = 0;

    uint2 screenSpacePos = uint2 (pos.xy) + centerDelta;

    uint2 r = tea (screenSpacePos);
    float2 rf = float2 (r) / 4294967295.0f;

    sx = scale * rf.x;
    sy = scale * rf.y;

    // Center shadow map inside window

    // snap to correct pixel
    int px = int (screenSpacePos.x * scale + sx);
    int py = int (screenSpacePos.y * scale + sy);

    if (px >= int(shadowMapWidth) || py >= int(shadowMapHeight) || px < 0 || py < 0)
    {
        // Out of bounds
        bool stripeIn = bool(uint((pos.x + rf.x * 4 + pos.y + rf.y * 4) / 16) & 2);
        Out_Color = float4 ((stripeIn ? 0.75 : 0.4) * (1.0f - 0.3f * rf.x), 0, 0, 1);
        return;
    }

    float d = texelFetch (depthMap, int2 (px, py), 0).x;
    Out_Color =  float4 (saturate(1.0 - d + rf.x / 256.0f).rrr, 1);
}