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
    int VertexId = gl_VertexID;
    if(0 == VertexId)
    {
        _output.Start = ChainSource;
        _output.End = ChainTargetPositions[0].xyz;
    }
    else
    {
        _output.Start = ChainTargetPositions[VertexId-1].xyz;
        _output.End = ChainTargetPositions[VertexId].xyz;
    }

    _output.Up = float3(0,1,0);
    _output.Level = 0;
}