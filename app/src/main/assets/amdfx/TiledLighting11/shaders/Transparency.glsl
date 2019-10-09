//-----------------------------------------------------------------------------------------
// Textures and Buffers
//-----------------------------------------------------------------------------------------
#if 0
StructuredBuffer<matrix> g_InstanceTransform : register( t0 );

Buffer<float4> g_PointLightBufferCenterAndRadius : register( t2 );
Buffer<float4> g_PointLightBufferColor           : register( t3 );
Buffer<uint>   g_PerTileLightIndexBuffer         : register( t4 );

Buffer<float4> g_SpotLightBufferCenterAndRadius  : register( t5 );
Buffer<float4> g_SpotLightBufferColor            : register( t6 );
Buffer<float4> g_SpotLightBufferSpotParams       : register( t7 );
Buffer<uint>   g_PerTileSpotIndexBuffer          : register( t8 );
#endif

layout(binding = 0) buffer Uniform0
{
    matrix g_InstanceTransform[];
};

layout(binding = 2) uniform samplerBuffer g_PointLightBufferCenterAndRadius;
layout(binding = 3) uniform samplerBuffer g_PointLightBufferColor;
layout(binding = 4) uniform usamplerBuffer g_PerTileLightIndexBuffer;

layout(binding = 5) uniform samplerBuffer g_SpotLightBufferCenterAndRadius;
layout(binding = 6) uniform samplerBuffer g_SpotLightBufferColor;
layout(binding = 7) uniform samplerBuffer g_SpotLightBufferSpotParams;
layout(binding = 8) uniform usamplerBuffer g_PerTileSpotIndexBuffer;

#if ( VPLS_ENABLED == 1 )
layout(binding = 9) uniform samplerBuffer g_VPLBufferCenterAndRadius;
layout(binding = 10) buffer Buffer10
{
    VPLData g_VPLBufferData[];
};
layout(binding = 11) uniform usamplerBuffer g_PerTileVPLIndexBuffer;
#endif
