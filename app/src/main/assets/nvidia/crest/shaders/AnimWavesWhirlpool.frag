#include "OceanLODData.glsl"

in float2 worldOffsetScaledXZ;
uniform float _Amplitude = 20.0;
in float2 worldPos;

layout(location = 0) out float4 OutColor;


void main()
{
    // power 4 smoothstep - no normalize needed
    // credit goes to stubbe's shadertoy: https://www.shadertoy.com/view/4ldSD2
    /*float r2 = dot( worldOffsetScaledXZ, worldOffsetScaledXZ);
    if (r2 > 1.0){
        OutColor = float4(0);
        return;
    }
//    return (float4)0.0;

    */

    float3 centerPos = float3(unity_ObjectToWorld[3][0],unity_ObjectToWorld[3][1],unity_ObjectToWorld[3][2]);
    float2 worldOffsetScaledXZ = worldPos - centerPos.xz;
    float m_LengthToCenter = length(worldOffsetScaledXZ);

    // power 4 smoothstep - no normalize needed
    // credit goes to stubbe's shadertoy: https://www.shadertoy.com/view/4ldSD2
    //    float r2 = dot( worldOffsetScaledXZ, worldOffsetScaledXZ);
    float _Radius = 20.0;
    float r2 = m_LengthToCenter/ _Radius;
    if(r2 >= 1.0)
    {
        OutColor = float4(0.0, 0, 0.0, 0.0);
        return;
    }

    r2 = 1.0 - r2;

    float y = r2 * r2 * _Amplitude;

    OutColor = float4(0.0, -y, 0.0, 0.0);
}