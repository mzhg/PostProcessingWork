#include "VolumeRenderer.glsl"
layout(location = 0) in vec3 In_Position;

out PS_INPUT_RAYDATA_FRONT
{
//    float4 pos      : SV_Position;
    float3 posInGrid/*: POSITION*/;
    float3 worldViewPos /*: TEXCOORD0*/;
}_output;

void main()
{
    gl_Position =  mul(float4(In_Position,1), WorldViewProjection);
    _output.posInGrid = In_Position;
    _output.worldViewPos = mul(float4(In_Position,1), WorldView).xyz;
}