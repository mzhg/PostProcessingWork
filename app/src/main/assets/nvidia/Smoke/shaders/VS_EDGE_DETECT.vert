#include "VolumeRenderer.glsl"
layout(location = 0) in vec3 In_Position;

out VS_OUTPUT_EDGE
{
    // There's no textureUV11 because its weight is zero.
//    float4 position      : SV_Position;   // vertex position
    float2 textureUV00   ;  // kernel tap texture coords
    float2 textureUV01   ;  // kernel tap texture coords
    float2 textureUV02   ;  // kernel tap texture coords
    float2 textureUV10   ;  // kernel tap texture coords
    float2 textureUV12   ;  // kernel tap texture coords
    float2 textureUV20   ;  // kernel tap texture coords
    float2 textureUV21   ;  // kernel tap texture coords
    float2 textureUV22   ;  // kernel tap texture coords
}_output;

// A full-screen edge detection pass to locate artifacts
void main()
{
    gl_Position = float4(In_Position,1);
    
    float2 texelSize = 1.0 / float2(RTWidth,RTHeight);
    float2 center = float2( (In_Position.x+1)/2.0 , 1.0 - (In_Position.y+1)/2.0 );

    // Eight nearest neighbours needed for Sobel.
    _output.textureUV00 = center + float2(-texelSize.x, -texelSize.y);
    _output.textureUV01 = center + float2(-texelSize.x,  0);
    _output.textureUV02 = center + float2(-texelSize.x,  texelSize.y);

    _output.textureUV10 = center + float2(0, -texelSize.y);
    _output.textureUV12 = center + float2(0,  texelSize.y);

    _output.textureUV20 = center + float2(texelSize.x, -texelSize.y);
    _output.textureUV21 = center + float2(texelSize.x,  0);
    _output.textureUV22 = center + float2(texelSize.x,  texelSize.y);
}