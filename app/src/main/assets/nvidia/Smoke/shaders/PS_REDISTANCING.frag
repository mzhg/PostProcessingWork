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
    const float dt = 0.1f;

    float phiC = //Texture_phi_next.SampleLevel( samPointClamp, input.texcoords, 0).r;
                textureLod(Texture_phi_next, _input.texcoords, 0).x;

    // avoid redistancing near boundaries, where gradients are ill-defined
    if( (_input.cell0.x < 3) || (_input.cell0.x > (textureWidth-4)) ||
        (_input.cell0.y < 3) || (_input.cell0.y > (textureHeight-4)) ||
        (_input.cell0.z < 3) || (_input.cell0.z > (textureDepth-4)) )
    {
        OutColor = float4(phiC);
        return;
    }
//        return phiC;

    bool isBoundary;
    bool hasHighSlope;
    float3 gradPhi = Gradient(Texture_phi_next, _input, isBoundary, 1.01f, hasHighSlope);
    float normGradPhi = length(gradPhi);

    if( isBoundary || !hasHighSlope || ( normGradPhi < 0.01f ) )
    {
        OutColor = float4(phiC);
        return;
    }
//        return phiC;


    //float signPhi = phiC > 0 ? 1 : -1;
    float phiC0 = textureLod(Texture_phi, _input.texcoords, 0).r;  // samPointClamp
    float signPhi = phiC0 / sqrt( (phiC0*phiC0) + 1);
    //float signPhi = phiC / sqrt( (phiC*phiC) + (normGradPhi*normGradPhi));

    float3 backwardsPos = _input.cell0 - (gradPhi/normGradPhi) * signPhi * dt;
    float3 npos = float3(backwardsPos.x/textureWidth, backwardsPos.y/textureHeight, (backwardsPos.z+0.5f)/textureDepth);

    OutColor = textureLod(Texture_phi_next , npos, 0).r + (signPhi * dt);   //samLinear
}