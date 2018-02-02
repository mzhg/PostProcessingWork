#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "AVSM_Node_defs.h"

layout(location = 0) out vec4 Out_Color;

in PS_INPUT
{
//    float4 Pos      : SV_POSITION;
    float3 Norm     ;// NORMAL;
    float4 LightUv  ;// TEXCOORD1;
    float3 Position ;// TEXCOORD2; // Object space position
}_input;

//Texture2D    TEXTURE0 : register( t0 );
//    SamplerState SAMPLER0 : register( s0 );
//    Texture2D    _Shadow  : register( t1 );

layout(binding = 0) uniform sampler2D TEXTURE0;
layout(binding = 1) uniform sampler2DShadow _Shadow;

layout(binding = 0) uniform cbPerModelValues
{
    float4x4 World /*: WORLD*/;
    float4x4 WorldViewProjection /*: WORLDVIEWPROJECTION*/;
    float4x4 InverseWorld /*: INVERSEWORLD*/;
    float4   LightDirection;
    float4   EyePosition;
    float4x4 LightWorldViewProjection;
};

layout(binding = 7) uniform cbMyAVSMValues
{
    float4   DummyColor;
};

// ********************************************************************************************************
//float4 PSMainNoTexture( PS_INPUT_NO_TEX input ) : SV_Target
void main()
{
    float3 lightUv = _input.LightUv.xyz / _input.LightUv.w;
    lightUv.xyz = lightUv.xyz * 0.5f + 0.5f;
//    float2 uvInvertY = float2(uv.x, 1.0f-uv.y);
    float shadowAmount = /*_Shadow.SampleCmp( SAMPLER1, uvInvertY, lightUv.z )*/texture(_Shadow, lightUv);
    float3 eyeDirection = normalize(_input.Position - EyePosition.xyz);
    float3 normal       = normalize(_input.Norm);
    float  nDotL = saturate( dot( normal, -normalize(LightDirection.xyz) ) );
    nDotL = shadowAmount * nDotL;
    float3 reflection   = reflect( eyeDirection, normal );
    float  rDotL        = saturate(dot( reflection, -LightDirection.xyz ));
    float  specular     = 0.2f * pow( rDotL, 4.0f );
    specular = min( shadowAmount, specular );
    Out_Color =  DummyColor* float4((nDotL + specular).xxx,  1.0f);
}