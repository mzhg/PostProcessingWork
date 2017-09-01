#include "Fire_Common.glsl"

layout(location=0) in vec3 In_Position;

out VolumeVertex
{
//    float4   ClipPos      : SV_Position;
    float3   Pos         /*: TEXCOORD0*/;      // vertex position in model space
    float3   RayDir      /*: TEXCOORD1*/;   // ray direction in model space
}_output;

void main()
{
    gl_Position = mul( float4( In_Position, 1 ), WorldViewProj );

    _output.RayDir = In_Position - EyePos;
    _output.Pos = In_Position;   // supposed to have range -0.5 ... 0.5
}