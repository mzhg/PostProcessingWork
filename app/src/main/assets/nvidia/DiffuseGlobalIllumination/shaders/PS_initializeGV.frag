#include "LPV.glsl"

in initLPV_GSOUT
{
//    float4 pos : SV_Position; // 2D slice vertex coordinates in homogenous clip space
    float3 normal /*: NORMAL*/;
    float3 color /*: COLOR*/;
    float fluxWeight /*:FW*/ ;
//    uint RTIndex : SV_RenderTargetArrayIndex;  // used to choose the destination slice
}_input;

layout(location = 0) out float4 occlusionCoefficients;
layout(location = 1) out float4 occluderColor;

void main()
{
    //have to add occluders represented as SH coefficients of clamped cosine lobes oriented in the direction of the normal.

    float3 Normal = normalize(_input.normal);

    float coeffs0, coeffs1, coeffs2, coeffs3;
    clampledCosineCoeff(Normal, coeffs0, coeffs1, coeffs2, coeffs3);

    occlusionCoefficients = float4(coeffs0*_input.fluxWeight,coeffs1*_input.fluxWeight, coeffs2*_input.fluxWeight, coeffs3*_input.fluxWeight);
    occluderColor = float4(_input.color.r,_input.color.g,_input.color.b,1.0f);
}