#include "MipmapSoftShadowCommon.glsl"

out float2 Out_Color;

void main()
{
    int3 iPos = int3(int(gl_FragCoord.x) << 1, int(gl_FragCoord.y) << 1, 0);
    float2 vDepth = texelFetch(DepthMip2, iPos.xy, 0), vDepth1;
    ++iPos.x;
    vDepth1 = texelFetch(DepthMip2, iPos.xy, 0);
    vDepth = float2(min(vDepth.x, vDepth1.x), max(vDepth.y, vDepth1.y));
    ++iPos.y;
    vDepth1 = texelFetch(DepthMip2, iPos.xy, 0);
    vDepth = float2(min(vDepth.x, vDepth1.x), max(vDepth.y, vDepth1.y));
    --iPos.x;
    vDepth1 = texelFetch(DepthMip2, iPos.xy, 0);
    vDepth = float2(min(vDepth.x, vDepth1.x), max(vDepth.y, vDepth1.y));
    Out_Color = vDepth;
}