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

    // get advected new position
    float3 npos = _input.cell0 - timestep *
//                  Texture_velocity.SampleLevel( samPointClamp, _input.texcoords, 0 ).xyz;
                    textureLod(Texture_velocity, _input.texcoords, 0.).xyz;

    // convert new position to texture coordinates
    float3 nposTC = float3(npos.x/textureWidth, npos.y/textureHeight, (npos.z+0.5)/textureDepth);

    // find the texel corner closest to the semi-Lagrangian "particle"
    float3 nposTexel = floor( npos + float3( 0.5f, 0.5f, 0.5f ) );
    float3 nposTexelTC = float3( nposTexel.x/textureWidth, nposTexel.y/textureHeight, nposTexel.z/textureDepth);

    // ht (half-texel)
    float3 ht = float3(0.5f/textureWidth, 0.5f/textureHeight, 0.5f/textureDepth);

    // get the values of nodes that contribute to the interpolated value
    // (texel centers are at half-integer locations)
    float4 nodeValues[8];
    // Texture_phi.SampleLevel( samPointClamp  TODO
    nodeValues[0] = textureLod(Texture_phi, nposTexelTC + float3(-ht.x, -ht.y, -ht.z), 0 );
    nodeValues[1] = textureLod(Texture_phi, nposTexelTC + float3(-ht.x, -ht.y,  ht.z), 0 );
    nodeValues[2] = textureLod(Texture_phi, nposTexelTC + float3(-ht.x,  ht.y, -ht.z), 0 );
    nodeValues[3] = textureLod(Texture_phi, nposTexelTC + float3(-ht.x,  ht.y,  ht.z), 0 );
    nodeValues[4] = textureLod(Texture_phi, nposTexelTC + float3( ht.x, -ht.y, -ht.z), 0 );
    nodeValues[5] = textureLod(Texture_phi, nposTexelTC + float3( ht.x, -ht.y,  ht.z), 0 );
    nodeValues[6] = textureLod(Texture_phi, nposTexelTC + float3( ht.x,  ht.y, -ht.z), 0 );
    nodeValues[7] = textureLod(Texture_phi, nposTexelTC + float3( ht.x,  ht.y,  ht.z), 0 );

    // determine a valid range for the result
    float4 phiMin = min(min(min(nodeValues[0], nodeValues [1]), nodeValues [2]), nodeValues [3]);
    phiMin = min(min(min(min(phiMin, nodeValues [4]), nodeValues [5]), nodeValues [6]), nodeValues [7]);

    float4 phiMax = max(max(max(nodeValues[0], nodeValues [1]), nodeValues [2]), nodeValues [3]);
    phiMax = max(max(max(max(phiMax, nodeValues [4]), nodeValues [5]), nodeValues [6]), nodeValues [7]);

    float4 ret;
    // Perform final MACCORMACK advection step:
    // You can use point sampling and keep Texture_phi_1_hat
    //  r = Texture_phi_1_hat.SampleLevel( samPointClamp, _input.texcoords, 0 )
    // OR use bilerp to avoid the need to keep a separate texture for phi_n_1_hat
    ret = // Texture_phi.SampleLevel( samLinear, nposTC, 0)
           textureLod(Texture_phi, nposTC, 0.)
        + 0.5 * ( textureLod(Texture_phi, _input.texcoords, 0 ) -
//                 Texture_phi_hat.SampleLevel( samPointClamp, _input.texcoords, 0 ) );
                   textureLod(Texture_phi_hat, _input.texcoords, 0));

    // clamp result to the desired range
    ret = max( min( ret, phiMax ), phiMin ) * decay;

    if(advectAsTemperature)
    {
         ret -= temperatureLoss * timestep;
         ret = clamp(ret,float4(0,0,0,0),float4(5,5,5,5));
    }

    OutColor = ret;
}