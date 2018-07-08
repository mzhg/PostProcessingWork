#include "Voxelizer.glsl"

layout(location = 0) in vec3 In_Position;
layout(location = 1) in vec3 In_PrevPos;

struct VsGenVelOutput
{
    float4 Pos	        /*: POSITION*/;
    float3 Velocity     /*: VELOCITY*/;
}_output;

void main()
{
//    VsGenVelOutput output;
    float4 gridPos = mul( float4(In_Position,1), WorldViewProjection );
    float4 prevGridPos = mul( float4(In_PrevPos,1), WorldViewProjection );
    _output.Pos = gridPos;
    _output.Velocity = float3(gridPos - prevGridPos) * 0.5f * gridDim * recTimeStep * velocityMultiplier;
    // - multiply by 0.5f because these positions are in clip space (-1,1) in each axis,
    //   instead of -0.5 to 0.5 (simulation volume space)
    // - multiply by gridDim to move to simulation voxel space,
    //   which is the space in which velocity is assumed to be in FluidSim.fx
//    return output;
}