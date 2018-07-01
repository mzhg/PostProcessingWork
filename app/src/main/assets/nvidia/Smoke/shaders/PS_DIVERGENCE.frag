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
    // Texture_velocity.SampleLevel( samPointClamp
    float4 fieldL = textureLod(Texture_velocity, LEFTCELL, 0 );
    float4 fieldR = textureLod(Texture_velocity, RIGHTCELL, 0 );
    float4 fieldB = textureLod(Texture_velocity, BOTTOMCELL, 0 );
    float4 fieldT = textureLod(Texture_velocity, TOPCELL, 0 );
    float4 fieldD = textureLod(Texture_velocity, DOWNCELL, 0 );
    float4 fieldU = textureLod(Texture_velocity, UPCELL, 0 );

    if( IsBoundaryCell(LEFTCELL) )  fieldL = GetObstVelocity(LEFTCELL);
    if( IsBoundaryCell(RIGHTCELL) ) fieldR = GetObstVelocity(RIGHTCELL);
    if( IsBoundaryCell(BOTTOMCELL) )fieldB = GetObstVelocity(BOTTOMCELL);
    if( IsBoundaryCell(TOPCELL) )   fieldT = GetObstVelocity(TOPCELL);
    if( IsBoundaryCell(DOWNCELL) )  fieldD = GetObstVelocity(DOWNCELL);
    if( IsBoundaryCell(UPCELL) )    fieldU = GetObstVelocity(UPCELL);

    float divergence =  0.5 *
        ( ( fieldR.x - fieldL.x ) + ( fieldT.y - fieldB.y ) + ( fieldU.z - fieldD.z ) );

    OutColor = divergence;
}