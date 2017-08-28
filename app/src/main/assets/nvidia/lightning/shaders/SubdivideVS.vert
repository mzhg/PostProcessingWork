#include "LightningCommon.glsl"

layout(location = 0) in vec3 Start; // start of segment
layout(location = 1) in vec3 End; // end of segment
layout(location = 2) in vec3 Up; // up vector, specifying frame of orientation for deviation parameters
layout(location = 3) in uint Level;  // n + 1 for forked segment, n for jittered segments

out SubdivideVSOut
{
    vec3 Start;
    vec3 End;
    vec3 Up;
    uint Level;
}_output;

void main()
{
    _output.Start = Start;
    _output.End = End;
    _output.Up = Up;
    _output.Level = Level;
}