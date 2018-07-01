#include "FluidSim.glsl"

in GS_OUTPUT_FLUIDSIM
{
//    float4 pos               : SV_Position; // 2D slice vertex coordinates in homogenous clip space
    float3 cell0             /*: TEXCOORD0*/;   // 3D cell coordinates (x,y,z in 0-dimension range)
    float3 texcoords         /*: TEXCOORD1*/;   // 3D cell texcoords (x,y,z in 0-1 range)
    float2 LR                /*: TEXCOORD2*/;   // 3D cell texcoords for the Left and Right neighbors
    float2 BT                /*: TEXCOORD3*/;   // 3D cell texcoords for the Bottom and Top neighbors
    float2 DU                /*: TEXCOORD4*/;   // 3D cell texcoords for the Down and Up neighbors
//    uint RTIndex             /*: SV_RenderTargetArrayIndex*/;  // used to choose the destination slice
}_input;

//#include "FluidGradient.glsl"

layout(location = 0) out float4 obstacle;
layout(location = 1) out float4 velocity;

bool PointIsInsideBox(float3 p, float3 LBUcorner, float3 RTDcorner)
{
    return ((p.x > LBUcorner.x) && (p.x < RTDcorner.x)
        &&  (p.y > LBUcorner.y) && (p.y < RTDcorner.y)
        &&  (p.z > LBUcorner.z) && (p.z < RTDcorner.z));
}

void main()
{
    float3 innerobstBoxLBDcorner = obstBoxLBDcorner + 1;
    float3 innerobstBoxRTUcorner = obstBoxRTUcorner - 1;
    // cells completely inside box = 0.5
    if(PointIsInsideBox(_input.cell0, innerobstBoxLBDcorner, innerobstBoxRTUcorner))
    {
        obstacle = OBSTACLE_INTERIOR;
        velocity = float4(0);
        return;
    }

    // cells in box boundary = 1.0
    if(PointIsInsideBox(_input.cell0, obstBoxLBDcorner, obstBoxRTUcorner))
    {
        obstacle = OBSTACLE_BOUNDARY;
        velocity = float4(obstBoxVelocity.xyz,1);
        return ;
    }

    obstacle = OBSTACLE_EXTERIOR;
    velocity = float4(0);
}