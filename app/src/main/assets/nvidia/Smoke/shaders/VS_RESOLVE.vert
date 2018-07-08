#include "Voxelizer.glsl"

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_Texcoord;

out VsResOutput
{
    float4 Pos          /*: POSITION*/;
    float3 Tex          /*: TEXCOORD*/;
}_output;

void main()
{
    _output.Pos = float4(In_Position,1);
    _output.Tex = In_Texcoord;
}