//--------------------------------------------------------------------------------------
// File: CommonHeader.glsl
//
// GLSL file for the TiledLighting11 sample. Common header file for all shaders.
//--------------------------------------------------------------------------------------
#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

//--------------------------------------------------------------------------------------
// Miscellaneous constants
//--------------------------------------------------------------------------------------
const float4 kRadarColors[14] = float4[14]
(
    float4(0,0.9255,0.9255,1),   // cyan
    float4(0,0.62745,0.9647,1),  // light blue
    float4(0,0,0.9647,1),        // blue
    float4(0,1,0,1),             // bright green
    float4(0,0.7843,0,1),        // green
    float4(0,0.5647,0,1),        // dark green
    float4(1,1,0,1),             // yellow
    float4(0.90588,0.75294,0,1), // yellow-orange
    float4(1,0.5647,0,1),        // orange
    float4(1,0,0,1),             // bright red
    float4(0.8392,0,0,1),        // red
    float4(0.75294,0,0,1),       // dark red
    float4(1,0,1,1),             // magenta
    float4(0.6,0.3333,0.7882,1)  // purple
);

//--------------------------------------------------------------------------------------
// Light culling constants.
// These must match their counterparts in CommonUtil.h
//--------------------------------------------------------------------------------------
#define TILE_RES 16
#define MAX_NUM_LIGHTS_PER_TILE 272
#define MAX_NUM_VPLS_PER_TILE 1024

//--------------------------------------------------------------------------------------
// Shadow constants.
// These must match their counterparts in CommonConstants.h
//--------------------------------------------------------------------------------------
#define MAX_NUM_SHADOWCASTING_POINTS     12
#define MAX_NUM_SHADOWCASTING_SPOTS      12

//--------------------------------------------------------------------------------------
// Constant Buffers
//--------------------------------------------------------------------------------------
/*cbuffer cbPerObject : register( b0 )
{
    matrix  g_mWorld                   : packoffset( c0 );
}

cbuffer cbPerCamera : register( b1 )
{
    matrix  g_mViewProjection          : packoffset( c0 );
};*/

uniform mat4 g_mWorld;
uniform mat4 g_mViewProjection;
uniform float2 g_CameraRange;   // x: near; y: far

layout(binding = 0) uniform cbPerFrame //: register( b2 )
{
    matrix              g_mView                      /*: packoffset( c0 )*/;
    matrix              g_mProjection                /*: packoffset( c4 )*/;
    matrix              g_mProjectionInv             /*: packoffset( c8 )*/;
    matrix              g_mViewProjectionInvViewport /*: packoffset( c12 )*/;
    float4              g_AmbientColorUp             /*: packoffset( c16 )*/;
    float4              g_AmbientColorDown           /*: packoffset( c17 )*/;
    float3              g_vCameraPos                 /*: packoffset( c18 )*/;
    float               g_fAlphaTest                 /*: packoffset( c18.w )*/;
    uint                g_uNumLights                 /*: packoffset( c19 )*/;
    uint                g_uNumSpotLights             /*: packoffset( c19.y )*/;
    uint                g_uWindowWidth               /*: packoffset( c19.z )*/;
    uint                g_uWindowHeight              /*: packoffset( c19.w )*/;
    uint                g_uMaxNumLightsPerTile       /*: packoffset( c20 )*/;
    uint                g_uMaxNumElementsPerTile     /*: packoffset( c20.y )*/;
    uint                g_uNumTilesX                 /*: packoffset( c20.z )*/;
    uint                g_uNumTilesY                 /*: packoffset( c20.w )*/;
    uint                g_uMaxVPLs                   /*: packoffset( c21 )*/;
    uint                g_uMaxNumVPLsPerTile         /*: packoffset( c21.y )*/;
    uint                g_uMaxNumVPLElementsPerTile  /*: packoffset( c21.z )*/;
    float               g_fVPLSpotStrength           /*: packoffset( c21.w )*/;
    float               g_fVPLSpotRadius             /*: packoffset( c22 )*/;
    float               g_fVPLPointStrength          /*: packoffset( c22.y )*/;
    float               g_fVPLPointRadius            /*: packoffset( c22.z )*/;
    float               g_fVPLRemoveBackFaceContrib  /*: packoffset( c22.w )*/;
    float               g_fVPLColorThreshold         /*: packoffset( c23 )*/;
    float               g_fVPLBrightnessThreshold    /*: packoffset( c23.y )*/;
    float               g_fPerFramePad1              /*: packoffset( c23.z )*/;
    float               g_fPerFramePad2              /*: packoffset( c23.w )*/;
};

layout(binding = 1) uniform cbShadowConstants //: register( b3 )
{
    matrix              g_mPointShadowViewProj[ MAX_NUM_SHADOWCASTING_POINTS ][ 6 ];
    matrix              g_mSpotShadowViewProj[ MAX_NUM_SHADOWCASTING_SPOTS ];
    float4              g_ShadowBias;  // X: Texel offset from edge, Y: Underscan scale, Z: Z bias for points, W: Z bias for spots
};

layout(binding = 2) uniform cbVPLConstants //: register( b4 )
{
    uint                g_uNumVPLs;
    uint                g_uVPLPad[ 3 ];
};

struct VPLData
{
    float4 Direction;
    float4 Color;
    float4 SourceLightDirection;
};

//-----------------------------------------------------------------------------------------
// Samplers
//-----------------------------------------------------------------------------------------
//SamplerState           g_Sampler       : register( s0 );
//SamplerComparisonState g_ShadowSampler : register( s1 );


//-----------------------------------------------------------------------------------------
// Helper functions
//-----------------------------------------------------------------------------------------

// convert a point from post-projection space into view space
float4 ConvertProjToView( float4 p )
{
    p = mul( p, g_mProjectionInv );
    p /= p.w;
    return p;
}

// convert a depth value from post-projection space into view space
float ConvertProjDepthToView( float z )
{
    float mZFar =g_CameraRange.y;
    float mZNear = g_CameraRange.x;
    float fCamSpaceZ = mZFar*mZNear/(mZFar-z*(mZFar-mZNear));

    return fCamSpaceZ;
}

uint GetTileIndex(float2 ScreenPos)
{
    float fTileRes = float(TILE_RES);
    uint nTileIdx = uint(floor(ScreenPos.x/fTileRes)+floor(ScreenPos.y/fTileRes)*g_uNumTilesX);
    return nTileIdx;
}

// PerTileLightIndexBuffer layout:
// | HalfZ High Bits | HalfZ Low Bits | Light Count List A | Light Count List B | space for max num lights per tile light indices (List A) | space for max num lights per tile light indices (List B) |
void GetLightListInfo(usamplerBuffer PerTileLightIndexBuffer, in uint uMaxNumLightsPerTile, in uint uMaxNumElementsPerTile, in float4 SVPosition, out uint uFirstLightIndex, out uint uNumLights)
{
    uint nTileIndex = GetTileIndex(SVPosition.xy);
    int nStartIndex = int(uMaxNumElementsPerTile*nTileIndex);

    // reconstruct fHalfZ
    uint uHalfZBitsHigh = texelFetch(PerTileLightIndexBuffer, nStartIndex).x;
    uint uHalfZBitsLow = texelFetch(PerTileLightIndexBuffer, nStartIndex+1).x;
    uint uHalfZBits = (uHalfZBitsHigh << 16) | uHalfZBitsLow;
    float fHalfZ = asfloat(uHalfZBits);

    float fViewPosZ = ConvertProjDepthToView( SVPosition.z );

    uFirstLightIndex = (fViewPosZ < fHalfZ) ? (nStartIndex + 4) : (nStartIndex + 4 + uMaxNumLightsPerTile);
    uNumLights = (fViewPosZ < fHalfZ) ? texelFetch(PerTileLightIndexBuffer, nStartIndex+2).x : texelFetch(PerTileLightIndexBuffer, nStartIndex+3).x;
}

float4 ConvertNumberOfLightsToGrayscale(uint nNumLightsInThisTile, uint uMaxNumLightsPerTile)
{
    float fPercentOfMax = float(nNumLightsInThisTile) / float(uMaxNumLightsPerTile);
    return float4(fPercentOfMax, fPercentOfMax, fPercentOfMax, 1.0f);
}

float4 ConvertNumberOfLightsToRadarColor(uint nNumLightsInThisTile, uint uMaxNumLightsPerTile)
{
    // black for no lights
    if( nNumLightsInThisTile == 0 ) return float4(0,0,0,1);
    // light purple for reaching the max
    else if( nNumLightsInThisTile == uMaxNumLightsPerTile ) return float4(0.847,0.745,0.921,1);
    // white for going over the max
    else if ( nNumLightsInThisTile > uMaxNumLightsPerTile ) return float4(1,1,1,1);
    // else use weather radar colors
    else
    {
        // use a log scale to provide more detail when the number of lights is smaller

        // want to find the base b such that the logb of uMaxNumLightsPerTile is 14
        // (because we have 14 radar colors)
        float fLogBase = exp2(0.07142857f*log2(float(uMaxNumLightsPerTile)));

        // change of base
        // logb(x) = log2(x) / log2(b)
        int nColorIndex = int(floor(log2(float(nNumLightsInThisTile)) / log2(fLogBase)));
        return kRadarColors[nColorIndex];
    }
}