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

    if( IsNonEmptyCell(_input.texcoords.xyz) )
    {
        OutColor = float4(0);
        return;
    }

    float4 omega = // Texture_vorticity.SampleLevel( samPointClamp, input.texcoords, 0 );
                    textureLod(Texture_vorticity, _input.texcoords, 0);

    // Potential optimization: don't find length multiple times - do once for the entire texture
    float omegaL = length( textureLod(Texture_vorticity, LEFTCELL, 0 ) );
    float omegaR = length( textureLod(Texture_vorticity, RIGHTCELL, 0 ) );
    float omegaB = length( textureLod(Texture_vorticity, BOTTOMCELL, 0 ) );
    float omegaT = length( textureLod(Texture_vorticity, TOPCELL, 0 ) );
    float omegaD = length( textureLod(Texture_vorticity, DOWNCELL, 0 ) );
    float omegaU = length( textureLod(Texture_vorticity, UPCELL, 0 ) );

    float3 eta = 0.5 * float3( omegaR - omegaL,
                              omegaT - omegaB,
                              omegaU - omegaD );

    eta = normalize( eta + float3(0.001, 0.001, 0.001) );

    float4 force;
    force.xyz = timestep * vortConfinementScale * float3( eta.y * omega.z - eta.z * omega.y,
                                            eta.z * omega.x - eta.x * omega.z,
                                            eta.x * omega.y - eta.y * omega.x );

    // Note: the result is added to the current velocity at each cell using "additive blending"
    OutColor = force;
}