#include "Cloth.h"

layout(location = 0) in uint In_State;
layout(location = 1) in vec3 In_Position;
layout(location = 2) in vec3 In_Normal;
layout(location = 3) in uint In_Old_State;
layout(location = 4) in vec3 In_Old_Position;

out VS_OUT
{
    Particle particle;
    Particle old_particle;
    vec3 normal;
}_output;

void main()
{
    _output.particle.State = In_State;
    _output.particle.Position = In_Position;

    _output.old_particle.State = In_Old_State;
    _output.old_particle.Position = In_Old_Position;

    _output.normal = In_Normal;
}