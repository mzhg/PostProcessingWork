#include "VolumeRenderer.glsl"
layout(location = 0) in vec3 In_Position;

out VS_OUTPUT_GLOW_9
{
//    float4 position    : SV_Position;   // vertex position
    float2 textureM4   ;  // kernel tap texture coords
    float2 textureM3   ;  // kernel tap texture coords
    float2 textureM2   ;  // kernel tap texture coords
    float2 textureM1   ;  // kernel tap texture coords
    float2 texture0    ;  // kernel tap texture coords
    float2 textureP1   ;  // kernel tap texture coords
    float2 textureP2   ;  // kernel tap texture coords
    float2 textureP3   ;  // kernel tap texture coords
    float2 textureP4   ;  // kernel tap texture coords
}_output;

void main()
{
    gl_Position = float4(In_Position,1);
    
    float texelSize = 1.0 / RTHeight;
    float2 center = float2( (In_Position.x+1)/2.0 , 1.0 - (In_Position.y+1)/2.0 );

    // 9 taps for a guassian filter with sigma of 3
    _output.textureM4 = center + float2(0,-texelSize*4.0f);
    _output.textureM3 = center + float2(0,-texelSize*3.0f);
    _output.textureM2 = center + float2(0,-texelSize*2.0f);
    _output.textureM1 = center + float2(0,-texelSize);
    _output.texture0  = center;
    _output.textureP1 = center + float2(0,texelSize);
    _output.textureP2 = center + float2(0,texelSize*2.0f);
    _output.textureP3 = center + float2(0,texelSize*3.0f);
    _output.textureP4 = center + float2(0,texelSize*4.0f);
}