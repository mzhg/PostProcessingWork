#include "LPV.glsl"

in initLPV_GSOUT
{
//    float4 pos : SV_Position; // 2D slice vertex coordinates in homogenous clip space
    float3 normal /*: NORMAL*/;
    float3 color /*: COLOR*/;
    float fluxWeight /*:FW*/ ;
//    uint RTIndex : SV_RenderTargetArrayIndex;  // used to choose the destination slice
}_input;

layout(location = 0) out float4 coefficientsRed;
layout(location = 1) out float4 coefficientsGreen;
layout(location = 2) out float4 coefficientsBlue;

void main()
{
    //have to add VPLs represented as SH coefficients of clamped cosine lobes oriented in the direction of the normal.

    float3 Normal = normalize(_input.normal);

    float coeffs0, coeffs1, coeffs2, coeffs3;
    clampledCosineCoeff(Normal, coeffs0, coeffs1, coeffs2, coeffs3);

    float flux_red = _input.color.r * _input.fluxWeight * gLightScale;
    float flux_green = _input.color.g * _input.fluxWeight * gLightScale;
    float flux_blue = _input.color.b * _input.fluxWeight * gLightScale;

    coefficientsRed =   float4(flux_red  *coeffs0, flux_red  *coeffs1, flux_red  *coeffs2, flux_red  *coeffs3);
    coefficientsGreen = float4(flux_green*coeffs0, flux_green*coeffs1, flux_green*coeffs2, flux_green*coeffs3);
    coefficientsBlue =  float4(flux_blue *coeffs0, flux_blue *coeffs1, flux_blue *coeffs2, flux_blue *coeffs3);
}