#include "FluidSim.glsl"

layout(location = 0) in float3 In_Position ;    // 2D slice vertex coordinates in clip space
layout(location = 1) in float3 In_TextureCoords0;    // 3D cell coordinates (x,y,z in 0-dimension range)

out VS_OUTPUT_FLUIDSIM
{
    float4 pos               /*: SV_Position*/;
    float3 cell0             /*: TEXCOORD0*/;
    float3 texcoords         /*: TEXCOORD1*/;
    float2 LR                /*: TEXCOORD2*/;
    float2 BT                /*: TEXCOORD3*/;
    float2 DU                /*: TEXCOORD4*/;
}_output;

void main()
{
    _output.pos = float4(In_Position.x, In_Position.y, In_Position.z, 1.0);
    _output.cell0 = float3(In_TextureCoords0.x, In_TextureCoords0.y, In_TextureCoords0.z);
    _output.texcoords = float3( (In_TextureCoords0.x)/(textureWidth),
                              (In_TextureCoords0.y)/(textureHeight),
                              (In_TextureCoords0.z+0.5)/(textureDepth));

    float x = _output.texcoords.x;
    float y = _output.texcoords.y;
    float z = _output.texcoords.z;

    // compute single texel offsets in each dimension
    float invW = 1.0/textureWidth;
    float invH = 1.0/textureHeight;
    float invD = 1.0/textureDepth;

    _output.LR = float2(x - invW, x + invW);
    _output.BT = float2(y - invH, y + invH);
    _output.DU = float2(z - invD, z + invD);
}