#include "../../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

// ********************************************************************************************************
layout(binding = 0) uniform cbPerModelValues
{
    float4x4 World /*: WORLD*/;
    float4x4 WorldViewProjection /*: WORLDVIEWPROJECTION*/;
    float4x4 InverseWorld /*: INVERSEWORLD*/;
    float4   LightDirection;
    float4   EyePosition;
    float4x4 LightWorldViewProjection;
};

#if !defined(vsmain) && !defined(psmain)
#error  you must define the shader type: vsmain or psmain
#endif

#if vsmain

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Normal;
layout(location = 2) in vec2 In_Uv;

out PS_INPUT
{
//    float4 Pos      : SV_POSITION;
    float3 Norm     /*: NORMAL*/;
    float2 Uv       /*: TEXCOORD0*/;
    float4 LightUv  /*: TEXCOORD1*/;
    float3 Position /*: TEXCOORD2*/; // Object space position
}_output;

out gl_PerVertex
{
    vec4 gl_Position;
};

void main()
{
    gl_Position = mul( float4( In_Position, 1.0f), WorldViewProjection );
    _output.Position = mul( float4( In_Position, 1.0f), World ).xyz;
    // TODO: transform the light into object space instead of the normal into world space
    _output.Norm = mul( In_Normal, float3x3(World) );
    _output.Uv   = In_Uv;
    _output.LightUv   = mul( float4( In_Position, 1.0f), LightWorldViewProjection );
}

#elif psmain

in PS_INPUT
{
//    float4 Pos      : SV_POSITION;
    float3 Norm     /*: NORMAL*/;
    float2 Uv       /*: TEXCOORD0*/;
    float4 LightUv  /*: TEXCOORD1*/;
    float3 Position /*: TEXCOORD2*/; // Object space position
}_input;

layout(location = 0) out vec4 Out_Color;
layout(binding = 0) uniform sampler2D TEXTURE0;

void main()
{
    Out_Color = texture(TEXTURE0, _input.Uv);
}

#endif