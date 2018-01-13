#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"
#include "AVSM_Node_defs.h"

layout(location = 0) out vec4 Out_Color;

in PS_INPUT
{
//    float4 Pos      : SV_POSITION;
    float3 Norm     ;// NORMAL;
    float2 Uv       ;// TEXCOORD0;
    float4 LightUv  ;// TEXCOORD1;
    float3 Position ;// TEXCOORD2; // Object space position
}_input;

uniform cbPerModelValues
{
    float4x4 World /*: WORLD*/;
    float4x4 WorldViewProjection /*: WORLDVIEWPROJECTION*/;
    float4x4 InverseWorld /*: INVERSEWORLD*/;
    float4   LightDirection;
    float4   EyePosition;
    float4x4 LightWorldViewProjection;
};

uniform cbMyAVSMValues
{
              float4   DummyColor;
};

// ********************************************************************************************************
// TODO: Note: nothing sets these values yet
uniform cbPerFrameValues
{
    float4x4  View;
    float4x4  Projection;
};

//Texture2D    TEXTURE0 : register( t0 );
//    SamplerState SAMPLER0 : register( s0 );
//    Texture2D    _Shadow  : register( t1 );

layout(binding = 0) uniform sampler2D TEXTURE0;
layout(binding = 1) uniform sampler2DShadow _Shadow;

//float4 PSMain( PS_INPUT input ) : SV_Target
void main()
{
    float3  lightUv = _input.LightUv.xyz / _input.LightUv.w;
    lightUv.xyz = lightUv.xyz * 0.5f + 0.5f; // TODO: Move scale and offset to matrix.
//    lightUv.y  = 1.0f - lightUv.y;
//    float   shadowAmount = _Shadow.SampleCmp( SAMPLER1, lightUv, lightUv.z );
    float  shadowAmount   = texture(_Shadow, lightUv);
    float3 normal         = normalize(_input.Norm);
    float  nDotL          = saturate( dot( normal, -LightDirection.xyz ) );
    float3 eyeDirection   = normalize(_input.Position - EyePosition.xyz);
    float3 reflection     = reflect( eyeDirection, normal );
    float  rDotL          = saturate(dot( reflection, -LightDirection.xyz ));
    float  specular       = pow(rDotL, 16.0f);
    specular              = min( shadowAmount, specular );
    float4 diffuseTexture = /*TEXTURE0.Sample( SAMPLER0, _input.Uv )*/ texture(TEXTURE0,_input.Uv);
    float ambient = 0.05;
    float3 result = (min(shadowAmount, nDotL)+ambient) * diffuseTexture.rgb + specular;
    Out_Color =  float4( result, 1.0f );
}