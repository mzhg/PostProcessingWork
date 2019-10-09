//--------------------------------------------------------------------------------------
// File: DebugDraw.hlsl
//
// HLSL file for the TiledLighting11 sample. Debug drawing.
//--------------------------------------------------------------------------------------


#include "CommonHeader.glsl"
#include "Transparency.glsl"
// disable warning: pow(f, e) will not work for negative f
#pragma warning( disable : 3571 )


//-----------------------------------------------------------------------------------------
// Textures and Buffers
//-----------------------------------------------------------------------------------------

// Save two slots for CDXUTSDKMesh diffuse and normal,
// so start with the third slot, t2
#if 0
Buffer<float4> g_PointLightBufferCenterAndRadius : register( t2 );
Buffer<float4> g_PointLightBufferColor           : register( t3 );
Buffer<uint>   g_PerTileLightIndexBuffer         : register( t4 );

Buffer<float4> g_SpotLightBufferCenterAndRadius  : register( t5 );
Buffer<float4> g_SpotLightBufferColor            : register( t6 );
Buffer<float4> g_SpotLightBufferSpotParams       : register( t7 );
Buffer<uint>   g_PerTileSpotIndexBuffer          : register( t8 );

#if ( VPLS_ENABLED == 1 )
StructuredBuffer<float4> g_VPLBufferCenterAndRadius : register( t9 );
StructuredBuffer<VPLData> g_VPLBufferData           : register( t10 );
Buffer<uint>   g_PerTileVPLIndexBuffer              : register( t11 );
#endif

Buffer<float4> g_SpotLightBufferSpotMatrices     : register( t12 );
#endif


layout(binding = 12) uniform samplerBuffer g_SpotLightBufferSpotMatrices;


//-----------------------------------------------------------------------------------------
// Helper functions
//-----------------------------------------------------------------------------------------
uint GetNumLightsInThisTile(usamplerBuffer PerTileLightIndexBuffer, uint uMaxNumLightsPerTile, uint uMaxNumElementsPerTile, float4 SVPosition)
{
    uint nIndex, nNumLightsInThisTile;
    GetLightListInfo(PerTileLightIndexBuffer, uMaxNumLightsPerTile, uMaxNumElementsPerTile, SVPosition, nIndex, nNumLightsInThisTile);
    return nNumLightsInThisTile;
}