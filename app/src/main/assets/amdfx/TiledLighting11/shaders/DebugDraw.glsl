//--------------------------------------------------------------------------------------
// File: DebugDraw.hlsl
//
// HLSL file for the TiledLighting11 sample. Debug drawing.
//--------------------------------------------------------------------------------------


#include "CommonHeader.glsl"

// disable warning: pow(f, e) will not work for negative f
#pragma warning( disable : 3571 )


//-----------------------------------------------------------------------------------------
// Textures and Buffers
//-----------------------------------------------------------------------------------------

// Save two slots for CDXUTSDKMesh diffuse and normal,
// so start with the third slot, t2
/*Buffer<float4> g_PointLightBufferCenterAndRadius : register( t2 );
Buffer<float4> g_PointLightBufferColor           : register( t3 );
Buffer<uint>   g_PerTileLightIndexBuffer         : register( t4 );*/

layout(binding = 2) buffer Buffer2
{
    float4 g_PointLightBufferCenterAndRadius[];
};

layout(binding = 3) buffer Buffer3
{
    float4 g_PointLightBufferColor[];
};

layout(binding = 4) buffer Buffer4
{
    uint g_PerTileLightIndexBuffer[];
};

/*Buffer<float4> g_SpotLightBufferCenterAndRadius  : register( t5 );
Buffer<float4> g_SpotLightBufferColor            : register( t6 );
Buffer<float4> g_SpotLightBufferSpotParams       : register( t7 );
Buffer<uint>   g_PerTileSpotIndexBuffer          : register( t8 );*/

layout(binding = 5) buffer Buffer5
{
    float4 g_SpotLightBufferCenterAndRadius[];
};

layout(binding = 6) buffer Buffer6
{
    float4 g_SpotLightBufferColor[];
};

layout(binding = 7) buffer Buffer7
{
    float4 g_SpotLightBufferSpotParams[];
};

layout(binding = 8) buffer Buffer8
{
    uint g_PerTileSpotIndexBuffer[];
};

#if ( VPLS_ENABLED == 1 )
/*StructuredBuffer<float4> g_VPLBufferCenterAndRadius : register( t9 );
StructuredBuffer<VPLData> g_VPLBufferData           : register( t10 );
Buffer<uint>   g_PerTileVPLIndexBuffer              : register( t11 );*/

layout(binding = 9) buffer Buffer9
{
    float4 g_VPLBufferCenterAndRadius[];
};

layout(binding = 10) buffer Buffer10
{
    VPLData g_VPLBufferData[];
};

layout(binding = 11) buffer Buffer11
{
    uint g_PerTileVPLIndexBuffer[];
};
#endif

//Buffer<float4> g_SpotLightBufferSpotMatrices     : register( t12 );
layout(binding = 12) buffer Buffer12
{
    float4 g_SpotLightBufferSpotMatrices[];
};

//-----------------------------------------------------------------------------------------
// Helper functions
//-----------------------------------------------------------------------------------------
uint GetNumLightsInThisTile(/*Buffer<uint> PerTileLightIndexBuffer,*/ uint uMaxNumLightsPerTile, uint uMaxNumElementsPerTile, float4 SVPosition)
{
    uint nIndex, nNumLightsInThisTile;
    GetLightListInfo(PerTileLightIndexBuffer, uMaxNumLightsPerTile, uMaxNumElementsPerTile, SVPosition, nIndex, nNumLightsInThisTile);
    return nNumLightsInThisTile;
}