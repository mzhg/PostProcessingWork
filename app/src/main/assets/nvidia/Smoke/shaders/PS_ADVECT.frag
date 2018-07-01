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

layout(location = 0) out float4 OutColor;

void main()
{
    if( IsOutsideSimulationDomain(_input.texcoords.xyz ) )
    {
        OutColor = float4(0);
        return;
    }

    float decayAmount = decay;

    if(doVelocityAttenuation)
    {
        float obstacle = //Texture_obstacles.SampleLevel(samPointClamp, input.texcoords.xyz, 0).r;
                        textureLod(Texture_obstacles, _input.texcoords.xyz, 0).r;
        if( obstacle <= OBSTACLE_BOUNDARY)
        {
            OutColor = float4(0);
            return;
        }
        decayAmount *= clamp((obstacle-maxDensityAmount)/(1 - maxDensityAmount),0,1)*(1-maxDensityDecay)+maxDensityDecay;
    }
    else if( IsNonEmptyCell(_input.texcoords.xyz) )
    {
        OutColor = float4(0);
        return;
    }

    float3 npos = GetAdvectedPosTexCoords(/*input*/);
    float4 ret = /*Texture_phi.SampleLevel( samLinear, npos, 0)*/
                textureLod(Texture_phi, npos, 0.)
                * decayAmount;



    if(advectAsTemperature)
    {
         ret -= temperatureLoss * timestep;
         ret = clamp(ret,float4(0,0,0,0),float4(5,5,5,5));
    }

    OutColor = ret;
}