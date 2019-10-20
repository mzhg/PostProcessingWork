#include "OceanLODData.glsl"

layout(location = 0) out vec4 OutColor;

uniform float _CrestTime;
uniform float _MeniscusWidth;

in struct Varyings
{
//    float4 positionCS : SV_POSITION;
    float2 uv /*: TEXCOORD0*/;
    half4 foam_screenPos /*: TEXCOORD1*/;
    half4 grabPos /*: TEXCOORD2*/;
    float3 worldPos /*: TEXCOORD3*/;
}_input;

void main()
{
    const float3 col = 1.3*float3(0.37, 0.4, 0.5);
    float alpha = abs(_input.uv.y - 0.5);
    alpha = pow(smoothstep(0.5, 0.0, alpha), 0.5);
    OutColor = float4(lerp(float3(1), col, alpha), alpha);
}