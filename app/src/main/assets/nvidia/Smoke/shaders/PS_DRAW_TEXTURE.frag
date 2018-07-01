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

#include "FluidGradient.glsl"

layout(location = 0) out float4 OutColor;

void main()
{
    //("Phi as density"),
    if( drawTextureNumber == 1){
        OutColor = float4(abs(Texture_phi.SampleLevel(samLinear,_input.texcoords,0).r), 0.0f, 0.0f, 1.0f);
        return;
    }
    //("Phi as level set"),
    else if( drawTextureNumber == 2)
    {
        float levelSet = textureLod(Texture_phi,_input.texcoords,0).r/(textureDepth);   // samLinear
        float color = lerp(1.0f, 0.0f, abs(levelSet));
        if( levelSet < 0 ){
            OutColor = float4(0.0f,  color, 0.0f, 1.0f);
            return;
        }

        OutColor = float4(color, 0.0f, 0.0f, 1.0f);
        return;
    }
    //("Gradient of phi"),
    else if( drawTextureNumber == 3){
        OutColor = float4(Gradient(Texture_phi/*, input*/), 1.0f);
        return;
    }
    //("Velocity Field"),
    else if( drawTextureNumber == 4){
        OutColor = float4(abs(textureLod(Texture_velocity,_input.texcoords,0).xyz),1.0f);  // samLinear
        return;
    }
    //("Pressure Field"),
    else if ( drawTextureNumber == 5){
        OutColor = float4(abs(textureLod(Texture_pressure,_input.texcoords,0).xyz), 1.0f);  // samLinear
        return;
    }
    //("Voxelized Obstacles"),
    else
    {
        float obstColor = (textureLod(Texture_obstacles,_input.texcoords,0).r - OBSTACLE_INTERIOR) / (OBSTACLE_EXTERIOR - OBSTACLE_INTERIOR);  // samLinear
        OutColor = float4(abs(textureLod(Texture_obstvelocity,_input.texcoords,0).xy), obstColor, 1.0f);   // samLinear
    }
}