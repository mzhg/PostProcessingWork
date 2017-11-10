#include "MipmapSoftShadowCommon.glsl"

out float2 Out_Color;

void main()
{
    int3 iPos;
    int2 vPos = int2(gl_FragCoord);
    if (vPos.x < DEPTH_RES)
    { // we fetch from the most detailed mip level
        iPos.xy = int2(vPos.x, vPos.y);
        iPos.z = 0;
    }
    else
    { // compute the level from which we fetch
      iPos.z = N_LEVELS - int(log2(vPos.y));
      iPos.x = (vPos.x - DEPTH_RES);
      iPos.y = (vPos.y - pow(2, N_LEVELS - iPos.z));
    }

    Out_Color = texelFetch(DepthMip2, iPos.xy, iPos.z).xy;
}