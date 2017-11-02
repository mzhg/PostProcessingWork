#include "AVSM.glsl"

layout(location = 0) out vec4 OutColor;
void main()
{
    const float adjustRatio = mShadowMapSize / 256.0f;
    float x = adjustRatio * (gl_FragCoord.x - 10.0f);
    float y = adjustRatio * (gl_FragCoord.y - (720.0f - 256.0f - 10.0f));

    uint offset = LT_GetFirstSegmentNodeOffset(int2(x,y));
    OutColor = offset != NODE_LIST_NULL ? float4(0, 0, 0, 0) : float4(1, 1, 1, 1);
}