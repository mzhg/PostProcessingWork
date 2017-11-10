#include "MipmapSoftShadowCommon.glsl"

out float2 Out_Color;

void main()
{
    float fDepth = texelFetch(DepthTex0, int2(gl_FragCoord), 0).x;
    Out_Color = float2(fDepth, fDepth);
}