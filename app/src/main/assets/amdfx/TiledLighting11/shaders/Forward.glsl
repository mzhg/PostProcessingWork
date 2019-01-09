#include "CommonHeader.glsl"
#include "LightingCommonHeader.glsl"


//-----------------------------------------------------------------------------------------
// Textures and Buffers
//-----------------------------------------------------------------------------------------
/*Texture2D              g_TxDiffuse     : register( t0 );
Texture2D              g_TxNormal      : register( t1 );*/

layout(binding = 0) uniform sampler2D  g_TxDiffuse     /*: register( t0 )*/;
layout(binding = 1) uniform sampler2D  g_TxNormal      /*: register( t1 )*/;

// Save two slots for CDXUTSDKMesh diffuse and normal,
// so start with the third slot, t2
/*Buffer<float4> g_PointLightBufferCenterAndRadius : register( t2 );
Buffer<float4> g_PointLightBufferColor           : register( t3 );
Buffer<uint>   g_PerTileLightIndexBuffer         : register( t4 );*/

layout(binding = 2) readonly buffer Buffer2
{
    float4 g_PointLightBufferCenterAndRadius[];
};

layout(binding = 3) readonly buffer Buffer3
{
    float4 g_PointLightBufferColor[];
};

layout(binding = 4) readonly buffer Buffer4
{
    uint g_PerTileLightIndexBuffer[];
};

/*Buffer<float4> g_SpotLightBufferCenterAndRadius  : register( t5 );
Buffer<float4> g_SpotLightBufferColor            : register( t6 );
Buffer<float4> g_SpotLightBufferSpotParams       : register( t7 );
Buffer<uint>   g_PerTileSpotIndexBuffer          : register( t8 );*/

layout(binding = 5) readonly buffer Buffer5
{
    float4 g_SpotLightBufferCenterAndRadius[];
};

layout(binding = 6) readonly buffer Buffer6
{
    float4 g_SpotLightBufferColor[];
};

layout(binding = 7) readonly buffer Buffer7
{
    uint g_SpotLightBufferSpotParams[];
};

layout(binding = 8) readonly buffer Buffer8
{
    uint g_PerTileSpotIndexBuffer[];
};

#if ( VPLS_ENABLED == 1 )
/*StructuredBuffer<float4> g_VPLBufferCenterAndRadius : register( t9 );
StructuredBuffer<VPLData> g_VPLBufferData           : register( t10 );
Buffer<uint>   g_PerTileVPLIndexBuffer              : register( t11 );*/
layout(binding = 9) readonly buffer Buffer9
{
    float4 g_VPLBufferCenterAndRadius[];
};

layout(binding = 10) readonly buffer Buffer10
{
    VPLData g_VPLBufferData[];
};

layout(binding = 11) readonly buffer Buffer11
{
    uint g_PerTileVPLIndexBuffer[];
};
#endif
