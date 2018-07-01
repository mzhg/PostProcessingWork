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

    if( IsBoundaryCell(_input.texcoords.xyz) ){
        OutColor = GetObstVelocity(_input.texcoords.xyz);
        return;
    }

    // Texture_pressure.SampleLevel( samPointClamp
    float pCenter = textureLod(Texture_pressure, _input.texcoords, 0 ).x;
    float pL = textureLod(Texture_pressure, LEFTCELL, 0 ).x;
    float pR = textureLod(Texture_pressure, RIGHTCELL, 0 ).x;
    float pB = textureLod(Texture_pressure, BOTTOMCELL, 0 ).x;
    float pT = textureLod(Texture_pressure, TOPCELL, 0 ).x;
    float pD = textureLod(Texture_pressure, DOWNCELL, 0 ).x;
    float pU = textureLod(Texture_pressure, UPCELL, 0 ).x;

    float4 velocity;
    float3 obstV = float3(0,0,0);
    float3 vMask = float3(1,1,1);
    float3 v;

    float3 vLeft = GetObstVelocity(LEFTCELL);
    float3 vRight = GetObstVelocity(RIGHTCELL);
    float3 vBottom = GetObstVelocity(BOTTOMCELL);
    float3 vTop = GetObstVelocity(TOPCELL);
    float3 vDown = GetObstVelocity(DOWNCELL);
    float3 vUp = GetObstVelocity(UPCELL);

    if( IsBoundaryCell(LEFTCELL) )  { pL = pCenter; obstV.x = vLeft.x; vMask.x = 0; }
    if( IsBoundaryCell(RIGHTCELL) ) { pR = pCenter; obstV.x = vRight.x; vMask.x = 0; }
    if( IsBoundaryCell(BOTTOMCELL) ){ pB = pCenter; obstV.y = vBottom.y; vMask.y = 0; }
    if( IsBoundaryCell(TOPCELL) )   { pT = pCenter; obstV.y = vTop.y; vMask.y = 0; }
    if( IsBoundaryCell(DOWNCELL) )  { pD = pCenter; obstV.z = vDown.z; vMask.z = 0; }
    if( IsBoundaryCell(UPCELL) )    { pU = pCenter; obstV.z = vUp.z; vMask.z = 0; }

    v = ( /*Texture_velocity.SampleLevel( samPointClamp, input.texcoords, 0 ).xyz*/
            textureLod(Texture_velocity, _input.texcoords, 0).xyz -
                 (1.0f/rho)*(0.5*float3( pR - pL, pT - pB, pU - pD )) );

    velocity.xyz = (vMask * v) + obstV;

    OutColor = velocity;
}