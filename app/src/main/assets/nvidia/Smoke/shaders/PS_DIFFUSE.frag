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
    float4 phi0C  = /*Texture_phi.SampleLevel( samPointClamp, _input.texcoords, 0 )*/
                    textureLod(Texture_phi, _input.texcoords, 0);

    // Texture_phi_next.SampleLevel( samPointClamp
    float4 phin0L = textureLod(Texture_phi_next, LEFTCELL, 0 );
    float4 phin0R = textureLod(Texture_phi_next, RIGHTCELL, 0 );
    float4 phin0B = textureLod(Texture_phi_next, BOTTOMCELL, 0 );
    float4 phin0T = textureLod(Texture_phi_next, TOPCELL, 0 );
    float4 phin0D = textureLod(Texture_phi_next, DOWNCELL, 0 );
    float4 phin0U = textureLod(Texture_phi_next, UPCELL, 0 );

    float dT = timestep;
    float v = viscosity;
    float dX = 1.;

    float4 phin1C = ( (phi0C * dX*dX) - (dT * v * ( phin0L + phin0R + phin0B + phin0T + phin0D + phin0T )) ) / ((6 * dT * v) + (dX*dX));

    OutColor = phin1C;
}