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
    // Texture_velocity.SampleLevel( samPointClamp
    float4 L = textureLod(Texture_velocity, LEFTCELL, 0 );
    float4 R = textureLod(Texture_velocity, RIGHTCELL, 0 );
    float4 B = textureLod(Texture_velocity, BOTTOMCELL, 0 );
    float4 T = textureLod(Texture_velocity, TOPCELL, 0 );
    float4 D = textureLod(Texture_velocity, DOWNCELL, 0 );
    float4 U = textureLod(Texture_velocity, UPCELL, 0 );

    float4 vorticity;
    // using central differences: D0_x = (D+_x - D-_x) / 2
    vorticity.xyz = 0.5 * float3( (( T.z - B.z ) - ( U.y - D.y )) ,
                                 (( U.x - D.x ) - ( R.z - L.z )) ,
                                 (( R.y - L.y ) - ( T.x - B.x )) );

    OutColor = float4(vorticity.xyz, 0);
}