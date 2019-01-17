#include "WavePackets.glsl"

layout(location = 0) in vec4 In_Pos	/*: POSITION*/;  // (x,y) = world position of this vertex, z,w = direction of traveling
layout(location = 1) in vec4 In_Att	/*: TEXTURE0*/;  // x = amplitude, w = time this ripple was initialized
layout(location = 2) in vec4 In_Att2	/*: TEXTURE1*/;

out VS_INPUT_PACKET
{
    vec4 vPos;
    vec4 Att;
    vec4 Att2;
}_output;

void main()
{
    _output.vPos = In_Pos;
    _output.Att = In_Att;
    _output.Att2 = In_Att2;
}