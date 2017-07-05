#include "Cloud_Common.glsl"
layout(location = 0) in float4 In_Pos;

out float2 vTex;

void main()
{
    // compute world position
    float4 vWorldPos;
    vWorldPos.xz = In_Pos.xy * vXZParam.xy + vXZParam.zw;
    // height is propotional to the square distance in horizontal direction.
    float2 vDir = vEye.xz - vWorldPos.xz;
    float fSqDistance = dot( vDir, vDir );
    vWorldPos.y = fSqDistance * vHeight.x + vHeight.y;
    vWorldPos.w = 1.0f;

    // transform and projection
    gl_Position = mul( vWorldPos, mW2C);

    // texture coordinate
    vTex = In_Pos.zw * vUVParam.xy + vUVParam.zw;
}