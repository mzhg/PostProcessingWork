#include "../PostProcessingHLSLCompatiable.glsl"

// Copyright 2013 Intel Corporation
// All Rights Reserved
//
// Permission is granted to use, copy, distribute and prepare derivative works of this
// software for any purpose and without fee, provided, that the above copyright notice
// and this statement appear in all copies. Intel makes no representations about the
// suitability of this software for any purpose. THIS SOFTWARE IS PROVIDED "AS IS."
// INTEL SPECIFICALLY DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, AND ALL LIABILITY,
// INCLUDING CONSEQUENTIAL AND OTHER INDIRECT DAMAGES, FOR THE USE OF THIS SOFTWARE,
// INCLUDING LIABILITY FOR INFRINGEMENT OF ANY PROPRIETARY RIGHTS, AND INCLUDING THE
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Intel does not
// assume any responsibility for any errors which may appear in this software nor any
// responsibility to update it.
//
// CMAA Version 1.3, by Filip Strugar (filip.strugar@intel.com)
//
/////////////////////////////////////////////////////////////////////////////////////////

const float4 c_edgeDebugColours[5] = float4[5]( float4( 0.5, 0.5, 0.5, 1 ), float4( 1, 0.1, 1.0, 1 ), float4( 0.9, 0, 0, 1 ), float4( 0, 0.9, 0, 1 ), float4( 0, 0, 0.9, 1 ) );

// Expecting values of 1 and 0 only!
uint PackEdge( uint4 edges )
{
//   return dot( edges, uint4( 1, 2, 4, 8 ) );
    uint result = edges.x + edges.y * 2 + edges.z * 4 + edges.w * 8;
    return result;
}

// how .rgba channels from the edge texture maps to pixel edges:
//
//                   A - 0x08
//              |瘄
//              |         |
//     0x04 - B |  pixel  | R - 0x01
//              |         |
//              |_________|
//                   G - 0x02
//
// (A - there's an edge between us and a pixel above us)
// (R - there's an edge between us and a pixel to the right)
// (G - there's an edge between us and a pixel at the bottom)
// (B - there's an edge between us and a pixel to the left)

// some quality settings
#define SETTINGS_ALLOW_SHORT_Zs

// debugging
// #define DEBUG_DISABLE_SIMPLE_SHAPES // enable/disable simple shapes

uint4 UnpackEdge( uint value )
{
   uint4 ret;
   ret.x = uint((value & 0x01u) != 0);
   ret.y = uint((value & 0x02u) != 0);
   ret.z = uint((value & 0x04u) != 0);
   ret.w = uint((value & 0x08u) != 0);
   return ret;
}

uint PackZ( const uint2 screenPos, const bool invertedZShape )
{
   uint retVal = screenPos.x | (screenPos.y << 15);
   if( invertedZShape )
      retVal |= uint(1 << 30);
   return retVal;
}

void UnpackZ( uint packedZ, out uint2 screenPos, out bool invertedZShape )
{
   screenPos.x = packedZ & 0x7FFFu;
   screenPos.y = (packedZ>>15) & 0x7FFFu;
   invertedZShape = (packedZ>>30) == 1;
}

uint PackZ( const uint2 screenPos, const bool invertedZShape, const bool horizontal )
{
   uint retVal = screenPos.x | (screenPos.y << 15);
   if( invertedZShape )
      retVal |= uint(1 << 30);
   if( horizontal )
      retVal |= uint(1 << 31);
   return retVal;
}

void UnpackZ( uint packedZ, out uint2 screenPos, out bool invertedZShape, out bool horizontal )
{
   screenPos.x    = packedZ & 0x7FFFu;
   screenPos.y    = (packedZ>>15) & 0x7FFFu;
   invertedZShape = (packedZ & uint(1 << 30)) != 0;
   horizontal     = (packedZ & uint(1 << 31)) != 0;
}

void UnpackBlurAAInfo( float packedValue, out uint edges, out uint shapeType )
{
    uint packedValueInt = uint(packedValue*255.5);
    edges       = packedValueInt & 0xFu;
    shapeType   = packedValueInt >> 4;
}


//#ifndef CMAA_INCLUDE_JUST_DEBUGGING_STUFF

// this isn't needed if colour UAV is _SRGB but that doesn't work everywhere
#ifdef IN_GAMMA_CORRECT_MODE

/////////////////////////////////////////////////////////////////////////////////////////
//
// SRGB Helper Functions taken from D3DX_DXGIFormatConvert.inl
float D3DX_FLOAT_to_SRGB(float val)
{
    if( val < 0.0031308f )
        val *= 12.92f;
    else
    {
        #ifdef _DEBUG
            val = abs( val );
        #endif
        val = 1.055f * pow(val,1.0f/2.4f) - 0.055f;
    }
    return val;
}
//
float3 D3DX_FLOAT3_to_SRGB(float3 val)
{
    float3 outVal;
    outVal.x = D3DX_FLOAT_to_SRGB( val.x );
    outVal.y = D3DX_FLOAT_to_SRGB( val.y );
    outVal.z = D3DX_FLOAT_to_SRGB( val.z );
    return outVal;
}
//
// SRGB_to_FLOAT_inexact is imprecise due to precision of pow implementations.
float D3DX_SRGB_to_FLOAT(float val)
{
    if( val < 0.04045f )
        val /= 12.92f;
    else
        val = pow((val + 0.055f)/1.055f,2.4f);
    return val;
}
//
float3 D3DX_SRGB_to_FLOAT3(float3 val)
{
    float3 outVal;
    outVal.x = D3DX_SRGB_to_FLOAT( val.x );
    outVal.y = D3DX_SRGB_to_FLOAT( val.y );
    outVal.z = D3DX_SRGB_to_FLOAT( val.z );
    return outVal;
}

#if 0

#define R8G8B8A8_UNORM_to_float4(x) unpackUnorm4x8(x)
#define float4_to_R8G8B8A8_UNORM(x) packUnorm4x8(x)

#else
float4 R8G8B8A8_UNORM_to_float4(uint packedInput)
{
    /*precise*/ float4 unpackedOutput;
    unpackedOutput.r = float  (packedInput      & 0x000000ffu) / 255.0;
    unpackedOutput.g = float ((packedInput>> 8) & 0x000000ffu) / 255.0;
    unpackedOutput.b = float ((packedInput>>16) & 0x000000ffu) / 255.0;
    unpackedOutput.a = float ((packedInput>>24) & 0x000000ffu) / 255.0;
    return unpackedOutput;
}
uint float4_to_R8G8B8A8_UNORM(/*precise*/ float4 unpackedInput)
{
    uint packedOutput;
    unpackedInput = min(max(unpackedInput,0),1); // NaN gets set to 0.
    unpackedInput *= 255.0;
    unpackedInput += 0.5f;
    unpackedInput = floor(unpackedInput);
    packedOutput = ( (uint(unpackedInput.r))      |
                    ((uint(unpackedInput.g)<< 8)) |
                    ((uint(unpackedInput.b)<<16)) |
                    ((uint(unpackedInput.a)<<24)) );
    return packedOutput;
}
#endif
//
/////////////////////////////////////////////////////////////////////////////////////////

#endif

// needed for one Gather call unfortunately :(
//SamplerState PointSampler   : register( s0 ); // { Filter = MIN_MAG_MIP_POINT; AddressU = Clamp; AddressV = Clamp; };
//SamplerState LinearSampler  : register( s1 ); // { Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR; AddressU = Clamp; AddressV = Clamp; };

struct CMAAConstants
{
   float4   LumWeights;                         // .rgb - luminance weight for each colour channel; .w unused for now (maybe will be used for gamma correction before edge detect)

   float    ColorThreshold;                     // for simple edge detection
   float    DepthThreshold;                     // for depth (unused at the moment)
   float    NonDominantEdgeRemovalAmount;       // how much non-dominant edges to remove
   float    Dummy0;

   float2   OneOverScreenSize;
   float    ScreenWidth;
   float    ScreenHeight;

   float4   DebugZoomTool;
};

#if 0
cbuffer CMAAGlobals : register(b4)
{
   CMAAConstants g_CMAA;
}

RWTexture2D<float>              g_resultTexture             : register( u0 );
RWTexture2D<float4>             g_resultTextureFlt4Slot1    : register( u1 );
RWTexture2D<float>              g_resultTextureSlot2        : register( u2 );

Texture2D<float4>               g_screenTexture	            : register( t0 );
Texture2D<float4>               g_depthTexture              : register( t1 );
Texture2D<uint4>                g_src0Texture4Uint          : register( t3 );
Texture2D<float>                g_src0TextureFlt		    : register( t3 );
Texture2D<float>                g_depthTextureFlt		    : register( t4 );

#else
layout(binding = 0) uniform CMAAGlobals
{
    CMAAConstants g_CMAA;
};

layout(r8, binding = 0) uniform image2D g_resultTexture;
layout(rgba16f, binding = 1) uniform image2D g_resultTextureFlt4Slot1;
layout(r8, binding = 2) uniform image2D g_resultTextureSlot2;

layout(binding = 0) uniform sampler2D g_screenTexture;
layout(binding = 1) uniform sampler2D g_depthTexture;
layout(binding = 2) uniform usampler2D g_src0Texture4Uint;
layout(binding = 3) uniform sampler2D g_src0TextureFlt;
layout(binding = 4) uniform sampler2D g_depthTextureFlt;


#endif



// Must be even number; Will work with ~16 pretty good too for additional performance, or with ~64 for highest quality.
/*static*/ const uint c_maxLineLength   = 64;

float EdgeDetectColorCalcDiff( float3 colorA, float3 colorB )
{
#if 0
   // CONSIDER THIS as highest quality:
   // Weighted Euclidean distance
   // (Copyright ?2010, Thiadmer Riemersma, ITB CompuPhase, see http://www.compuphase.com/cmetric.htm for details)
   float rmean = ( colorA.r + colorB.r ) / 2.0;
   float3 delta = colorA - colorB;
   return sqrt( ( (2.0+rmean)*delta.r*delta.r ) + 4*delta.g*delta.g + ( (3.0-rmean)*delta.b*delta.b ) ) * 0.28;
   // (0.28 is an empirically set fudge to match two functions below)
#endif

// two versions, very similar results and almost identical performance
//   - maybe a bit higher quality per-color diff (use this by default)
//   - maybe a bit lower quality luma only diff (use this if luma already available in alpha channel)
#if 1
	float3 LumWeights   = g_CMAA.LumWeights.rgb;

	return dot( abs( colorA.rgb - colorB.rgb  ), LumWeights.rgb );
#else
    const float3 cLumaConsts = float3(0.299, 0.587, 0.114);                     // this matches FXAA (http://en.wikipedia.org/wiki/CCIR_601); above code uses http://en.wikipedia.org/wiki/Rec._709
    return abs( dot( colorA, cLumaConsts ) - dot( colorB, cLumaConsts ) );
#endif
}

bool EdgeDetectColor( float3 colorA, float3 colorB )
{
     return EdgeDetectColorCalcDiff( colorA, colorB ) > g_CMAA.ColorThreshold;
}

float PackBlurAAInfo( uint2 pixelPos, uint shapeType )
{
    uint packedEdges = uint(texelFetch(g_src0TextureFlt, int2(pixelPos.xy), 0 ).r * 255.5);

    uint retval = packedEdges + (shapeType << 4);

    return float(retval) / 255.0;
}

void FindLineLength( out uint lineLengthLeft, out uint lineLengthRight, int2 screenPos,
                /*uniform*/ bool horizontal, /*uniform*/ bool invertedZShape, const int2 stepRight )
{

   /////////////////////////////////////////////////////////////////////////////////////////////////////////
   // TODO: there must be a cleaner and faster way to get to these - a precalculated array indexing maybe?
   uint maskLeft, bitsContinueLeft, maskRight, bitsContinueRight;
   {
      // Horizontal (vertical is the same, just rotated 90?counter-clockwise)
      // Inverted Z case:              // Normal Z case:
      //   __                          // __
      //  X|                           //  X|
      //                               //
      uint maskTraceLeft, maskTraceRight;
      uint maskStopLeft, maskStopRight;
      if( horizontal )
      {
         if( invertedZShape )
         {
            maskTraceLeft    = 0x02; // tracing bottom edge
            maskTraceRight   = 0x08; // tracing top edge
         }
         else
         {
            maskTraceLeft    = 0x08; // tracing top edge
            maskTraceRight   = 0x02; // tracing bottom edge
         }
         maskStopLeft   = 0x01; // stop on right edge
         maskStopRight  = 0x04; // stop on left edge
      }
      else
      {
         if( invertedZShape )
         {
            maskTraceLeft    = 0x01; // tracing right edge
            maskTraceRight   = 0x04; // tracing left edge
         }
         else
         {
            maskTraceLeft    = 0x04; // tracing left edge
            maskTraceRight   = 0x01; // tracing right edge
         }
         maskStopLeft   = 0x08; // stop on top edge
         maskStopRight  = 0x02; // stop on bottom edge
      }

      maskLeft         = maskTraceLeft | maskStopLeft;
      bitsContinueLeft = maskTraceLeft;
      maskRight        = maskTraceRight | maskStopRight;
      bitsContinueRight= maskTraceRight;
   }
   /////////////////////////////////////////////////////////////////////////////////////////////////////////

   uint stopLimiter = c_maxLineLength*2;
#ifdef SETTINGS_ALLOW_SHORT_Zs
   uint i = 1;
#else
   uint i = 2; // starting from 2 because we already know it's at least 2...
#endif
   //[unroll]
   //[allow_uav_condition]
//   [loop]
   for( ; i < c_maxLineLength; i++ )
   {
      uint edgeLeft  = uint(texelFetch(g_src0TextureFlt, int2( screenPos.xy - stepRight * int(i)),       0).r * 255.5);
      uint edgeRight = uint(texelFetch(g_src0TextureFlt, int2( screenPos.xy + stepRight * int(i+1)),   0  ).r * 255.5);

      // stop on encountering 'stopping' edge (as defined by masks)
      //bool stopLeft  = ( (edgeLeft & maskStopLeft) != 0   ) || ( (edgeLeft & maskTraceLeft) == 0 );
      //bool stopRight = ( (edgeRight & maskStopRight) != 0 ) || ( (edgeRight & maskTraceRight) == 0 );
      bool stopLeft  = (edgeLeft & maskLeft) != bitsContinueLeft;
      bool stopRight = (edgeRight & maskRight) != bitsContinueRight;

      if( stopLeft || stopRight )
      {
         lineLengthLeft = 1 + i - uint(stopLeft);
         lineLengthRight = 1 + i - uint(stopRight);
         return;
      }
   }
   lineLengthLeft = lineLengthRight = i;
}

void ProcessDetectedZ( int2 screenPos, bool horizontal, bool invertedZShape )
{
   uint lineLengthLeft, lineLengthRight;

   const int2 stepRight     = (horizontal)?( int2( 1, 0 ) ):( int2( 0,  -1 ) );
   const float2 blendDir    = (horizontal)?( float2( 0, -1 ) ):( float2( -1,  0 ) );

   FindLineLength( lineLengthLeft, lineLengthRight, screenPos, horizontal, invertedZShape, stepRight );

   int width, height;
//   g_screenTexture.GetDimensions( width, height );
    int2 tex_size = textureSize(g_screenTexture, 0);
    width = tex_size.x;
    height = tex_size.y;
   float2 pixelSize = float2( 1.0 / float(width), 1.0 / float(height) );

   float leftOdd  = 0.15 * float(lineLengthLeft % 2);
   float rightOdd = 0.15 * float(lineLengthRight % 2);

   int loopFrom = -int((lineLengthLeft+1)/2)+1;
   int loopTo   = int((lineLengthRight+1)/2);

   float totalLength = float(loopTo - loopFrom)+1 - leftOdd - rightOdd;

   //[allow_uav_condition]
//   [loop]
   for( int i = loopFrom; i <= loopTo; i++ )
   {
      int2      pixelPos    = screenPos + stepRight * i;
      float2    pixelPosFlt = float2( pixelPos.x + 0.5, pixelPos.y + 0.5 );

#ifdef DEBUG_OUTPUT_AAINFO
//      g_resultTextureSlot2[ pixelPos ] = PackBlurAAInfo( pixelPos, 1 );
      imageStore(g_resultTextureSlot2, pixelPos, float4(PackBlurAAInfo( pixelPos, 1 )));
#endif

      // debug output a.)
//      g_resultTextureFlt4Slot1[pixelPos] = float4( (i > 0)?(float3(1, 0, horizontal)):(float3(0, 1, horizontal)), 1.0 );
        imageStore(g_resultTextureFlt4Slot1, pixelPos, float4( (i > 0)?(float3(1, 0, horizontal)):(float3(0, 1, horizontal)), 1.0 ));

      // debug output b.)
      //g_resultTextureFlt4Slot1[pixelPos] = float4( float3( lineLengthLeft*10 / 255.0, lineLengthRight*10/255.0, horizontal ), 1.0 );
      //continue;

      float m = (i + 0.5 - leftOdd - loopFrom) / totalLength;
      m = saturate( m );
      float k = m - float(i > 0);
      k = (invertedZShape)?(-k):(k);

      // debug output c.)
      // g_resultTextureFlt4Slot1[pixelPos] = float4( ( i > 0 )?( float3( 0.5-k, 0, horizontal ) ):( float3( 0, 0.5-k, horizontal ) ), 1.0 );

      float4 _output = textureLod( g_screenTexture, (pixelPosFlt + blendDir * k) * pixelSize, 0.0 );  //LinearSampler

#ifdef IN_GAMMA_CORRECT_MODE
      _output.rgb = D3DX_FLOAT3_to_SRGB( _output.rgb );
#endif

//      g_resultTextureFlt4Slot1[pixelPos] = float4( _output.rgba ); //, pixelC.a );
        imageStore(g_resultTextureFlt4Slot1, pixelPos, _output.rgba);
   }
}

float4 CalcDbgDisplayColor( const float4 blurMap )
{
   float3 pixelC = float3( 0.0, 0.0, 0.0 );
   float3 pixelL = float3( 0.0, 0.0, 1.0 );
   float3 pixelT = float3( 1.0, 0.0, 0.0 );
   float3 pixelR = float3( 0.0, 1.0, 0.0 );
   float3 pixelB = float3( 0.8, 0.8, 0.0 );

   const float centerWeight = 1.0;
   const float fromBelowWeight   = (1 / (1 - blurMap.x)) - 1;
   const float fromAboveWeight   = (1 / (1 - blurMap.y)) - 1;
   const float fromRightWeight   = (1 / (1 - blurMap.z)) - 1;
   const float fromLeftWeight    = (1 / (1 - blurMap.w)) - 1;

   const float weightSum = centerWeight + dot( float4( fromBelowWeight, fromAboveWeight, fromRightWeight, fromLeftWeight ), float4( 1, 1, 1, 1 ) );

   float4 pixel;

   //pixel = tex2D( g_xScreenTextureSampler, pixel_UV );
   pixel.rgb = pixelC.rgb + fromAboveWeight * pixelT + fromBelowWeight * pixelB +
      fromLeftWeight * pixelL + fromRightWeight * pixelR;
   pixel.rgb /= weightSum;

   pixel.a = dot( pixel.rgb, float3( 1, 1, 1 ) ) * 100.0;

   //pixel.rgb = lerp( pixel.rgb, float3( 1, 0, 0 ), 0.5 );

   return saturate( pixel );
}

