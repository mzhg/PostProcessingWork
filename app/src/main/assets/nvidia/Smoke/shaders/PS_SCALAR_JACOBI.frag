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

layout(location = 0) out float OutColor;

void main()
{
    float pCenter = textureLod(Texture_pressure, _input.texcoords, 0 );   // samPointClamp
    float bC =  textureLod(Texture_divergence, _input.texcoords, 0 );  // samPointClamp

    float pL = textureLod(Texture_pressure, LEFTCELL, 0 );
    float pR = textureLod(Texture_pressure, RIGHTCELL, 0 );
    float pB = textureLod(Texture_pressure, BOTTOMCELL, 0 );
    float pT = textureLod(Texture_pressure, TOPCELL, 0 );
    float pD = textureLod(Texture_pressure, DOWNCELL, 0 );
    float pU = textureLod(Texture_pressure, UPCELL, 0 );

    if( IsBoundaryCell(LEFTCELL) )  pL = pCenter;
    if( IsBoundaryCell(RIGHTCELL) ) pR = pCenter;
    if( IsBoundaryCell(BOTTOMCELL) )pB = pCenter;
    if( IsBoundaryCell(TOPCELL) )   pT = pCenter; 
    if( IsBoundaryCell(DOWNCELL) )  pD = pCenter;  
    if( IsBoundaryCell(UPCELL) )    pU = pCenter;

    OutColor = ( pL + pR + pB + pT + pU + pD - bC ) /6.0;
}
