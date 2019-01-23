#include "PerFrameConstants.glsl"

layout(location = 0) in float3 In_Position;
layout(location = 1) in float3 In_Normal;
layout(location = 2) in float2 In_TexCoord;

out GeometryVSOut
{
    float3 positionView /*: positionView*/;      // View space position
    float3 normal       /*: normal*/;
    float2 texCoord     /*: texCoord*/;
}_output;

void main()
{
    gl_Position     = mul(float4(In_Position, 1.0f), mCameraWorldViewProj);
    _output.positionView = mul(float4(In_Position, 1.0f), mCameraWorldView).xyz;
    _output.normal       = mul(float4(In_Normal, 0.0f), mCameraWorldView).xyz;
    _output.texCoord     = In_TexCoord;
}