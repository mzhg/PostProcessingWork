///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2016, Intel Corporation
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
// the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
// SOFTWARE.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// File changes (yyyy-mm-dd)
// 2016-09-07: filip.strugar@intel.com: first commit
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#include "../../../shader_libs/PostProcessingHLSLCompatiable.glsl"

// progressive poisson-like pattern; x, y are in [-1, 1] range, .z is length( float2(x,y) ), .w is log2( z )
#define INTELSSAO_MAIN_DISK_SAMPLE_COUNT (32)
const float4 g_samplePatternMain[INTELSSAO_MAIN_DISK_SAMPLE_COUNT] =
{
    float4( 0.78488064,  0.56661671,  1.500000, -0.126083),    float4( 0.26022232, -0.29575172,  1.500000, -1.064030),    float4( 0.10459357,  0.08372527,  1.110000, -2.730563),    float4(-0.68286800,  0.04963045,  1.090000, -0.498827),
    float4(-0.13570161, -0.64190155,  1.250000, -0.532765),    float4(-0.26193795, -0.08205118,  0.670000, -1.783245),    float4(-0.61177456,  0.66664219,  0.710000, -0.044234),    float4( 0.43675563,  0.25119025,  0.610000, -1.167283),
    float4( 0.07884444,  0.86618668,  0.640000, -0.459002),    float4(-0.12790935, -0.29869005,  0.600000, -1.729424),    float4(-0.04031125,  0.02413622,  0.600000, -4.792042),    float4( 0.16201244, -0.52851415,  0.790000, -1.067055),
    float4(-0.70991218,  0.47301072,  0.640000, -0.335236),    float4( 0.03277707, -0.22349690,  0.600000, -1.982384),    float4( 0.68921727,  0.36800742,  0.630000, -0.266718),    float4( 0.29251814,  0.37775412,  0.610000, -1.422520),
    float4(-0.12224089,  0.96582592,  0.600000, -0.426142),    float4( 0.11071457, -0.16131058,  0.600000, -2.165947),    float4( 0.46562141, -0.59747696,  0.600000, -0.189760),    float4(-0.51548797,  0.11804193,  0.600000, -1.246800),
    float4( 0.89141309, -0.42090443,  0.600000,  0.028192),    float4(-0.32402530, -0.01591529,  0.600000, -1.543018),    float4( 0.60771245,  0.41635221,  0.600000, -0.605411),    float4( 0.02379565, -0.08239821,  0.600000, -3.809046),
    float4( 0.48951152, -0.23657045,  0.600000, -1.189011),    float4(-0.17611565, -0.81696892,  0.600000, -0.513724),    float4(-0.33930185, -0.20732205,  0.600000, -1.698047),    float4(-0.91974425,  0.05403209,  0.600000,  0.062246),
    float4(-0.15064627, -0.14949332,  0.600000, -1.896062),    float4( 0.53180975, -0.35210401,  0.600000, -0.758838),    float4( 0.41487166,  0.81442589,  0.600000, -0.505648),    float4(-0.24106961, -0.32721516,  0.600000, -1.665244)
};

// these values can be changed (up to SSAO_MAX_TAPS) with no changes required elsewhere; values for 4th and 5th preset are ignored but array needed to avoid compilation errors
// the actual number of texture samples is two times this value (each "tap" has two symmetrical depth texture samples)
const uint g_numTaps[5]   = { 3U, 5U, 12U, 0U, 0U };

// an example of higher quality low/medium/high settings
// static const uint g_numTaps[5]   = { 4, 9, 16, 0, 0 };

// ** WARNING ** if changing anything here, please remember to update the corresponding C++ code!
struct ASSAOConstants
{
    float2                  ViewportPixelSize;                      // .zw == 1.0 / ViewportSize.xy
    float2                  HalfViewportPixelSize;                  // .zw == 1.0 / ViewportHalfSize.xy

    float2                  DepthUnpackConsts;
    float2                  CameraTanHalfFOV;

    float2                  NDCToViewMul;
    float2                  NDCToViewAdd;

    int2                    PerPassFullResCoordOffset;
    float2                  PerPassFullResUVOffset;

    float2                  Viewport2xPixelSize;
    float2                  Viewport2xPixelSize_x_025;              // Viewport2xPixelSize * 0.25 (for fusing add+mul into mad)

    float                   EffectRadius;                           // world (viewspace) maximum size of the shadow
    float                   EffectShadowStrength;                   // global strength of the effect (0 - 5)
    float                   EffectShadowPow;
    float                   EffectShadowClamp;

    float                   EffectFadeOutMul;                       // effect fade out from distance (ex. 25)
    float                   EffectFadeOutAdd;                       // effect fade out to distance   (ex. 100)
    float                   EffectHorizonAngleThreshold;            // limit errors on slopes and caused by insufficient geometry tessellation (0.05 to 0.5)
    float                   EffectSamplingRadiusNearLimitRec;          // if viewspace pixel closer than this, don't enlarge shadow sampling radius anymore (makes no sense to grow beyond some distance, not enough samples to cover everything, so just limit the shadow growth; could be SSAOSettingsFadeOutFrom * 0.1 or less)

    float                   DepthPrecisionOffsetMod;
    float                   NegRecEffectRadius;                     // -1.0 / EffectRadius
    float                   LoadCounterAvgDiv;                      // 1.0 / ( halfDepthMip[SSAO_DEPTH_MIP_LEVELS-1].sizeX * halfDepthMip[SSAO_DEPTH_MIP_LEVELS-1].sizeY )
    float                   AdaptiveSampleCountLimit;

    float                   InvSharpness;
    int                     PassIndex;
    float2                  QuarterResPixelSize;                    // used for importance map only

    float4                  PatternRotScaleMatrices[5];

    float                   NormalsUnpackMul;
    float                   NormalsUnpackAdd;
    float                   DetailAOStrength;
    float                   Dummy0;

#if SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION
    float4x4                NormalsWorldToViewspaceMatrix;
#endif
};

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Optional parts that can be enabled for a required quality preset level and above (0 == Low, 1 == Medium, 2 == High, 3 == Highest/Adaptive, 4 == reference/unused )
// Each has its own cost. To disable just set to 5 or above.
//
// (experimental) tilts the disk (although only half of the samples!) towards surface normal; this helps with effect uniformity between objects but reduces effect distance and has other side-effects
#define SSAO_TILT_SAMPLES_ENABLE_AT_QUALITY_PRESET                      (99)        // to disable simply set to 99 or similar
#define SSAO_TILT_SAMPLES_AMOUNT                                        (0.4)
//
#define SSAO_HALOING_REDUCTION_ENABLE_AT_QUALITY_PRESET                 (1)         // to disable simply set to 99 or similar
#define SSAO_HALOING_REDUCTION_AMOUNT                                   (0.6)       // values from 0.0 - 1.0, 1.0 means max weighting (will cause artifacts, 0.8 is more reasonable)
//
#define SSAO_NORMAL_BASED_EDGES_ENABLE_AT_QUALITY_PRESET                (2)         // to disable simply set to 99 or similar
#define SSAO_NORMAL_BASED_EDGES_DOT_THRESHOLD                           (0.5)       // use 0-0.1 for super-sharp normal-based edges
//
#define SSAO_DETAIL_AO_ENABLE_AT_QUALITY_PRESET                         (1)         // whether to use DetailAOStrength; to disable simply set to 99 or similar
//
#define SSAO_DEPTH_MIPS_ENABLE_AT_QUALITY_PRESET                        (2)         // !!warning!! the MIP generation on the C++ side will be enabled on quality preset 2 regardless of this value, so if changing here, change the C++ side too
#define SSAO_DEPTH_MIPS_GLOBAL_OFFSET                                   (-4.3)      // best noise/quality/performance tradeoff, found empirically
//
// !!warning!! the edge handling is hard-coded to 'disabled' on quality level 0, and enabled above, on the C++ side; while toggling it here will work for 
// testing purposes, it will not yield performance gains (or correct results)
#define SSAO_DEPTH_BASED_EDGES_ENABLE_AT_QUALITY_PRESET                 (1)     
//
#define SSAO_REDUCE_RADIUS_NEAR_SCREEN_BORDER_ENABLE_AT_QUALITY_PRESET  (99)        // 99 means disabled; only helpful if artifacts at the edges caused by lack of out of screen depth data are not acceptable with the depth sampler in either clamp or mirror modes
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#if 0
uniform SSAOConstantsBuffer                     //: register( b0 )    // corresponds to SSAO_CONSTANTS_BUFFERSLOT
{
    ASSAOConstants                    g_ASSAOConsts;
}
#else
uniform ASSAOConstants                    g_ASSAOConsts;	
#endif

#if 0
SamplerState        g_PointClampSampler         : register( s0 );   // corresponds to SSAO_SAMPLERS_SLOT0
SamplerState        g_LinearClampSampler        : register( s1 );   // corresponds to SSAO_SAMPLERS_SLOT1
SamplerState        g_PointMirrorSampler        : register( s2 );   // corresponds to SSAO_SAMPLERS_SLOT2
SamplerState        g_ViewspaceDepthTapSampler  : register( s3 );   // corresponds to SSAO_SAMPLERS_SLOT3

Texture2D<float>    g_DepthSource               : register( t0 );   // corresponds to SSAO_TEXTURE_SLOT0
Texture2D           g_NormalmapSource           : register( t1 );   // corresponds to SSAO_TEXTURE_SLOT1

Texture2D<float>    g_ViewspaceDepthSource      : register( t0 );   // corresponds to SSAO_TEXTURE_SLOT0
Texture2D<float>    g_ViewspaceDepthSource1     : register( t1 );   // corresponds to SSAO_TEXTURE_SLOT1
Texture2D<float>    g_ViewspaceDepthSource2     : register( t2 );   // corresponds to SSAO_TEXTURE_SLOT2
Texture2D<float>    g_ViewspaceDepthSource3     : register( t3 );   // corresponds to SSAO_TEXTURE_SLOT3

Texture2D<float>    g_ImportanceMap             : register( t3 );   // corresponds to SSAO_TEXTURE_SLOT3

Texture1D<uint>     g_LoadCounter               : register( t2 );   // corresponds to SSAO_TEXTURE_SLOT2

Texture2D           g_BlurInput                 : register( t2 );   // corresponds to SSAO_TEXTURE_SLOT2

Texture2DArray      g_FinalSSAO                 : register( t4 );   // corresponds to SSAO_TEXTURE_SLOT4

RWTexture2D<float4> g_NormalsOutputUAV          : register( u4 );   // corresponds to SSAO_NORMALMAP_OUT_UAV_SLOT
RWTexture1D<uint>   g_LoadCounterOutputUAV      : register( u4 );   // corresponds to SSAO_LOAD_COUNTER_UAV_SLOT
#else
#define TEX2D_DEPTH_BUFFER  0
#define TEX2D_NORMAL_BUFFER    1
#define TEX2D_VIEW_DEPTH 2
#define TEX2D_VIEW_DEPTH1   3
#define TEX2D_VIEW_DEPTH2  4
#define TEX2D_VIEW_DEPTH3 5
#define TEX2D_IMPORTANCE_MAP   6
#define TEX2D_LOAD_COUNTER 7
#define TEX2D_BLUR_INPUT 8
#define TEX2D_FINAL_SSAO 9

layout(binding = TEX2D_DEPTH_BUFFER) uniform sampler2D  g_DepthSource;
layout(binding = TEX2D_NORMAL_BUFFER) uniform sampler2D  g_NormalmapSource;

layout(binding = TEX2D_VIEW_DEPTH) uniform sampler2D  g_ViewspaceDepthSource;
layout(binding = TEX2D_VIEW_DEPTH1) uniform sampler2D  g_ViewspaceDepthSource1;
layout(binding = TEX2D_VIEW_DEPTH2) uniform sampler2D  g_ViewspaceDepthSource2;
layout(binding = TEX2D_VIEW_DEPTH3) uniform sampler2D  g_ViewspaceDepthSource3;

layout(binding = TEX2D_VIEW_DEPTH1) uniform sampler2D  g_ViewspaceDepthTapSource;

layout(binding = TEX2D_IMPORTANCE_MAP) uniform sampler2D  g_ImportanceMap;

//layout(binding = TEX2D_LOAD_COUNTER) uniform sampler1D  g_LoadCounter;

layout(binding = TEX2D_BLUR_INPUT) uniform sampler2D  g_BlurInput;

layout(binding = TEX2D_FINAL_SSAO) uniform sampler2DArray  g_FinalSSAO;

layout(binding = 0) uniform usampler1D g_LoadCounter;
//layout(binding = TEX2D_IMPORTANCE_MAP) uniform sampler2D  g_NormalsOutputUAV;
//layout(binding = TEX2D_IMPORTANCE_MAP) uniform sampler2D  g_LoadCounterOutputUAV;

#endif 

float4 RandomMapping(float2 uv)
{
	float x = pow(abs(uv.x), 0.3);
    float y = pow(abs(uv.y), 0.5);
    float z = pow(abs(uv.x), 0.6);
    float w = pow(abs(uv.y), 0.9);

	return float4(x,y,z,w);
}

// packing/unpacking for edges; 2 bits per edge mean 4 gradient values (0, 0.33, 0.66, 1) for smoother transitions!
float PackEdges( float4 edgesLRTB )
{
//    int4 edgesLRTBi = int4( saturate( edgesLRTB ) * 3.0 + 0.5 );
//    return ( (edgesLRTBi.x << 6) + (edgesLRTBi.y << 4) + (edgesLRTBi.z << 2) + (edgesLRTBi.w << 0) ) / 255.0;

    // optimized, should be same as above
    edgesLRTB = round( saturate( edgesLRTB ) * 3.05 );
    return dot( edgesLRTB, float4( 64.0 / 255.0, 16.0 / 255.0, 4.0 / 255.0, 1.0 / 255.0 ) ) ;
}

float4 UnpackEdges( float _packedVal )
{
    uint packedVal = uint(_packedVal * 255.5);
    float4 edgesLRTB;
    edgesLRTB.x = float((packedVal >> 6) & 0x03) / 3.0;          // there's really no need for mask (as it's an 8 bit input) but I'll leave it in so it doesn't cause any trouble in the future
    edgesLRTB.y = float((packedVal >> 4) & 0x03) / 3.0;
    edgesLRTB.z = float((packedVal >> 2) & 0x03) / 3.0;
    edgesLRTB.w = float((packedVal >> 0) & 0x03) / 3.0;

    return saturate( edgesLRTB + g_ASSAOConsts.InvSharpness );
}

float ScreenSpaceToViewSpaceDepth( float screenDepth )
{
    float depthLinearizeMul = g_ASSAOConsts.DepthUnpackConsts.x;
    float depthLinearizeAdd = g_ASSAOConsts.DepthUnpackConsts.y;

    // Optimised version of "-cameraClipNear / (cameraClipFar - projDepth * (cameraClipFar - cameraClipNear)) * cameraClipFar"

    // Set your depthLinearizeMul and depthLinearizeAdd to:
    // depthLinearizeMul = ( cameraClipFar * cameraClipNear) / ( cameraClipFar - cameraClipNear );
    // depthLinearizeAdd = cameraClipFar / ( cameraClipFar - cameraClipNear );

    return depthLinearizeMul / ( depthLinearizeAdd - screenDepth );
}

// from [0, width], [0, height] to [-1, 1], [-1, 1]
float2 ScreenSpaceToClipSpacePositionXY( float2 screenPos )
{
    return screenPos * g_ASSAOConsts.Viewport2xPixelSize.xy - float2( 1.0f, 1.0f );
}

float3 ScreenSpaceToViewSpacePosition( float2 screenPos, float viewspaceDepth )
{
    return float3( g_ASSAOConsts.CameraTanHalfFOV.xy * viewspaceDepth * ScreenSpaceToClipSpacePositionXY( screenPos ), viewspaceDepth );
}

float3 ClipSpaceToViewSpacePosition( float2 clipPos, float viewspaceDepth )
{
    return float3( g_ASSAOConsts.CameraTanHalfFOV.xy * viewspaceDepth * clipPos, viewspaceDepth );
}

float3 NDCToViewspace( float2 pos, float viewspaceDepth )
{
#if SSAO_DEBUG_STATIC_SCENE == 0
    viewspaceDepth = -viewspaceDepth;
#endif
    float3 ret;

    ret.xy = (g_ASSAOConsts.NDCToViewMul * pos.xy + g_ASSAOConsts.NDCToViewAdd) * viewspaceDepth;

    ret.z = viewspaceDepth;

    return ret;
}

// calculate effect radius and fit our screen sampling pattern inside it
void CalculateRadiusParameters( const float pixCenterLength, const float2 pixelDirRBViewspaceSizeAtCenterZ, out float pixLookupRadiusMod, out float effectRadius, out float falloffCalcMulSq )
{
    effectRadius = g_ASSAOConsts.EffectRadius;

    // leaving this out for performance reasons: use something similar if radius needs to scale based on distance
    //effectRadius *= pow( pixCenterLength, g_ASSAOConsts.RadiusDistanceScalingFunctionPow);

    // when too close, on-screen sampling disk will grow beyond screen size; limit this to avoid closeup temporal artifacts
    const float tooCloseLimitMod = saturate( pixCenterLength * g_ASSAOConsts.EffectSamplingRadiusNearLimitRec ) * 0.8 + 0.2;
    
    effectRadius *= tooCloseLimitMod;

    // 0.85 is to reduce the radius to allow for more samples on a slope to still stay within influence
    pixLookupRadiusMod = (0.85 * effectRadius) / pixelDirRBViewspaceSizeAtCenterZ.x;

    // used to calculate falloff (both for AO samples and per-sample weights)
    falloffCalcMulSq= -1.0f / (effectRadius*effectRadius);
}

float4 CalculateEdges( const float centerZ, const float leftZ, const float rightZ, const float topZ, const float bottomZ )
{
    // slope-sensitive depth-based edge detection
    float4 edgesLRTB = float4( leftZ, rightZ, topZ, bottomZ ) - centerZ;
    float4 edgesLRTBSlopeAdjusted = edgesLRTB + edgesLRTB.yxwz;
    edgesLRTB = min( abs( edgesLRTB ), abs( edgesLRTBSlopeAdjusted ) );
    return saturate( ( 1.3 - edgesLRTB / (centerZ * 0.040) ) );

    // cheaper version but has artifacts
    // edgesLRTB = abs( float4( leftZ, rightZ, topZ, bottomZ ) - centerZ; );
    // return saturate( ( 1.3 - edgesLRTB / (pixZ * 0.06 + 0.1) ) );
}

float3 CalculateNormal(float4 edgesLRTB, float3 pixCenterPos, float3 pixLPos, float3 pixRPos, float3 pixTPos, float3 pixBPos )
{
    // Get this pixel's viewspace normal
    float4 acceptedNormals  = float4( edgesLRTB.x*edgesLRTB.z, edgesLRTB.z*edgesLRTB.y, edgesLRTB.y*edgesLRTB.w, edgesLRTB.w*edgesLRTB.x );

    pixLPos = normalize(pixLPos - pixCenterPos);
    pixRPos = normalize(pixRPos - pixCenterPos);
    pixTPos = normalize(pixTPos - pixCenterPos);
    pixBPos = normalize(pixBPos - pixCenterPos);

    float3 pixelNormal = float3( 0, 0, -0.0005 );
    pixelNormal += ( acceptedNormals.x ) * cross( pixLPos, pixTPos );
    pixelNormal += ( acceptedNormals.y ) * cross( pixTPos, pixRPos );
    pixelNormal += ( acceptedNormals.z ) * cross( pixRPos, pixBPos );
    pixelNormal += ( acceptedNormals.w ) * cross( pixBPos, pixLPos );
    pixelNormal = normalize( pixelNormal );
    
    return pixelNormal;
}

float3 DecodeNormal( float3 encodedNormal )
{
    float3 normal = encodedNormal * g_ASSAOConsts.NormalsUnpackMul.xxx + g_ASSAOConsts.NormalsUnpackAdd.xxx;

#if SSAO_ENABLE_NORMAL_WORLD_TO_VIEW_CONVERSION
    normal = mul( normal, (float3x3)g_ASSAOConsts.NormalsWorldToViewspaceMatrix ).xyz; 
#endif

    // normal = normalize( normal );    // normalize adds around 2.5% cost on High settings but makes little (PSNR 66.7) visual difference when normals are as in the sample (stored in R8G8B8A8_UNORM,
    //                                  // decoded in the shader), however it will likely be required if using different encoding/decoding or the inputs are not normalized, etc.

    return normal;
}

float3 LoadNormal( int2 pos )
{
//    float3 encodedNormal = g_NormalmapSource.Load( int3( pos, 0 ) ).xyz;
	float3 encodedNormal = texelFetch(g_NormalmapSource, pos, 0).xyz;
    return DecodeNormal( encodedNormal );
}

float3 LoadNormal( int2 pos, int2 offset )
{
//    float3 encodedNormal = g_NormalmapSource.Load( int3( pos, 0 ), offset ).xyz;
    float3 encodedNormal = texelFetchOffset(g_NormalmapSource, pos, 0, offset).xyz;
    return DecodeNormal( encodedNormal );
}

// all vectors in viewspace
float CalculatePixelObscurance( float3 pixelNormal, float3 hitDelta, float falloffCalcMulSq )
{
  float lengthSq = dot( hitDelta, hitDelta );
  float NdotD = dot(pixelNormal, hitDelta) / sqrt(lengthSq);

  float falloffMult = max( 0.0, lengthSq * falloffCalcMulSq + 1.0 );

  return max( 0, NdotD - g_ASSAOConsts.EffectHorizonAngleThreshold ) * falloffMult;
}

void SSAOTapInner( const int qualityLevel, inout float obscuranceSum, inout float weightSum, const float2 samplingUV, const float mipLevel, const float3 pixCenterPos, const float3 negViewspaceDir,float3 pixelNormal, const float falloffCalcMulSq, const float weightMod, const int dbgTapIndex )
{
    // get depth at sample
//    float viewspaceSampleZ = g_ViewspaceDepthSource.SampleLevel( g_ViewspaceDepthTapSampler, samplingUV.xy, mipLevel ).x; // * g_ASSAOConsts.MaxViewspaceDepth;
	float viewspaceSampleZ = textureLod(g_ViewspaceDepthTapSource, samplingUV.xy, mipLevel).x;
	
    // convert to viewspace
    float3 hitPos = NDCToViewspace( samplingUV.xy, viewspaceSampleZ ).xyz;
    float3 hitDelta = hitPos - pixCenterPos;

    float obscurance = CalculatePixelObscurance( pixelNormal, hitDelta, falloffCalcMulSq );
    float weight = 1.0;
        
    if( qualityLevel >= SSAO_HALOING_REDUCTION_ENABLE_AT_QUALITY_PRESET )
    {
        //float reduct = max( 0, dot( hitDelta, negViewspaceDir ) );
        float reduct = max( 0, -hitDelta.z ); // cheaper, less correct version
        reduct = saturate( reduct * g_ASSAOConsts.NegRecEffectRadius + 2.0 ); // saturate( 2.0 - reduct / g_ASSAOConsts.EffectRadius );
        weight = SSAO_HALOING_REDUCTION_AMOUNT * reduct + (1.0 - SSAO_HALOING_REDUCTION_AMOUNT);
    }
    weight *= weightMod;
    obscuranceSum += obscurance * weight;
    weightSum += weight;
}

void SSAOTap( const int qualityLevel, inout float obscuranceSum, inout float weightSum, const int tapIndex, const float2x2 rotScale,
              const float3 pixCenterPos, const float3 negViewspaceDir, float3 pixelNormal, const float2 normalizedScreenPos,
              const float mipOffset, const float falloffCalcMulSq, float weightMod, float2 normXY, float normXYLength )
{
    float2  sampleOffset;
    float   samplePow2Len;

    // patterns
    {
        float4 newSample = g_samplePatternMain[tapIndex];
        sampleOffset    = rotScale * newSample.xy;
        samplePow2Len   = newSample.w;                      // precalculated, same as: samplePow2Len = log2( length( newSample.xy ) );
        weightMod *= newSample.z;
    }

    // snap to pixel center (more correct obscurance math, avoids artifacts)
    sampleOffset                    = round(sampleOffset);

    // calculate MIP based on the sample distance from the centre, similar to as described 
    // in http://graphics.cs.williams.edu/papers/SAOHPG12/.
    float mipLevel = ( qualityLevel < SSAO_DEPTH_MIPS_ENABLE_AT_QUALITY_PRESET )?(0):(samplePow2Len + mipOffset);

    float2 samplingUV = sampleOffset * g_ASSAOConsts.Viewport2xPixelSize + normalizedScreenPos;

    SSAOTapInner( qualityLevel, obscuranceSum, weightSum, samplingUV, mipLevel, pixCenterPos, negViewspaceDir, pixelNormal,
                  falloffCalcMulSq, weightMod, tapIndex * 2 );

    // for the second tap, just use the mirrored offset
    float2 sampleOffsetMirroredUV    = -sampleOffset;

    // tilt the second set of samples so that the disk is effectively rotated by the normal
    // effective at removing one set of artifacts, but too expensive for lower quality settings
    if( qualityLevel >= SSAO_TILT_SAMPLES_ENABLE_AT_QUALITY_PRESET )
    {
        float dotNorm = dot( sampleOffsetMirroredUV, normXY );
        sampleOffsetMirroredUV -= dotNorm * normXYLength * normXY;
        sampleOffsetMirroredUV = round(sampleOffsetMirroredUV);
    }
    
    // snap to pixel center (more correct obscurance math, avoids artifacts)
    float2 samplingMirroredUV = sampleOffsetMirroredUV * g_ASSAOConsts.Viewport2xPixelSize + normalizedScreenPos;

    SSAOTapInner( qualityLevel, obscuranceSum, weightSum, samplingMirroredUV, mipLevel, pixCenterPos, negViewspaceDir, pixelNormal, falloffCalcMulSq, weightMod, tapIndex * 2 + 1 );
}

// this function is designed to only work with half/half depth at the moment - there's a couple of hardcoded paths that expect pixel/texel size, so it will not work for full res
void GenerateSSAOShadowsInternal( out float outShadowTerm, out float4 outEdges, out float outWeight, const float2 SVPos/*, const float2 normalizedScreenPos*/, /*uniform*/
                                  int qualityLevel, bool adaptiveBase )
{
    float2 SVPosRounded = trunc( SVPos );
    uint2 SVPosui = uint2( SVPosRounded ); //same as uint2( SVPos )

    const int numberOfTaps = (adaptiveBase)?(SSAO_ADAPTIVE_TAP_BASE_COUNT) : int( g_numTaps[qualityLevel] );
    float pixZ, pixLZ, pixTZ, pixRZ, pixBZ;

#if 0
	float2 uv           = SVPosRounded * g_ASSAOConsts.HalfViewportPixelSize;
	float4 valuesUL     = RandomMapping(uv);

	uv                  += g_ASSAOConsts.HalfViewportPixelSize;
	float4 valuesBR     = RandomMapping(uv);
#else
	float4 valuesUL = textureGather(g_ViewspaceDepthSource,       SVPosRounded * g_ASSAOConsts.HalfViewportPixelSize);
	float4 valuesBR = textureGatherOffset(g_ViewspaceDepthSource, SVPosRounded * g_ASSAOConsts.HalfViewportPixelSize, int2(1));
#endif

#if 1
    // get this pixel's viewspace depth
    pixZ = valuesUL.y; //float pixZ = g_ViewspaceDepthSource.SampleLevel( g_PointMirrorSampler, normalizedScreenPos, 0.0 ).x; // * g_ASSAOConsts.MaxViewspaceDepth;

    // get left right top bottom neighbouring pixels for edge detection (gets compiled out on qualityLevel == 0)
    pixLZ   = valuesUL.x;
    pixTZ   = valuesUL.z;
    pixRZ   = valuesBR.z;
    pixBZ   = valuesBR.x;
#else
    pixZ = valuesUL.w;
    pixLZ   = valuesUL.x;
    pixTZ   = valuesUL.z;
    pixRZ   = valuesBR.z;
    pixBZ   = valuesBR.x;
#endif

    float2 normalizedScreenPos = SVPosRounded * g_ASSAOConsts.Viewport2xPixelSize + g_ASSAOConsts.Viewport2xPixelSize_x_025;
    float3 pixCenterPos = NDCToViewspace( normalizedScreenPos, pixZ ); // g

    // Load this pixel's viewspace normal
    uint2 fullResCoord = SVPosui * 2 + g_ASSAOConsts.PerPassFullResCoordOffset.xy;
    float3 pixelNormal = LoadNormal( int2(fullResCoord) );

    const float2 pixelDirRBViewspaceSizeAtCenterZ = pixCenterPos.z * g_ASSAOConsts.NDCToViewMul * g_ASSAOConsts.Viewport2xPixelSize;  // optimized approximation of:  float2 pixelDirRBViewspaceSizeAtCenterZ = NDCToViewspace( normalizedScreenPos.xy + g_ASSAOConsts.ViewportPixelSize.xy, pixCenterPos.z ).xy - pixCenterPos.xy;

    float pixLookupRadiusMod;
    float falloffCalcMulSq;

    // calculate effect radius and fit our screen sampling pattern inside it
    float effectViewspaceRadius;
    CalculateRadiusParameters( length( pixCenterPos ), pixelDirRBViewspaceSizeAtCenterZ, pixLookupRadiusMod, effectViewspaceRadius, falloffCalcMulSq );

    // calculate samples rotation/scaling
    float2x2 rotScale;
    {
        // reduce effect radius near the screen edges slightly; ideally, one would render a larger depth buffer (5% on each side) instead
        if( !adaptiveBase && (qualityLevel >= SSAO_REDUCE_RADIUS_NEAR_SCREEN_BORDER_ENABLE_AT_QUALITY_PRESET) )
        {
            float nearScreenBorder = min( min( normalizedScreenPos.x, 1.0 - normalizedScreenPos.x ), min( normalizedScreenPos.y, 1.0 - normalizedScreenPos.y ) );
            nearScreenBorder = saturate( 10.0 * nearScreenBorder + 0.6 );
            pixLookupRadiusMod *= nearScreenBorder;
        }

        // load & update pseudo-random rotation matrix
        uint pseudoRandomIndex = uint( SVPosRounded.y * 2 + SVPosRounded.x ) % 5;
        float4 rs = g_ASSAOConsts.PatternRotScaleMatrices[ pseudoRandomIndex ];
        rotScale = float2x2( rs.x * pixLookupRadiusMod, rs.y * pixLookupRadiusMod, rs.z * pixLookupRadiusMod, rs.w * pixLookupRadiusMod );
        rotScale = transpose(rotScale);
    }

    // the main obscurance & sample weight storage
    float obscuranceSum = 0.0;
    float weightSum = 0.0;

    // edge mask for between this and left/right/top/bottom neighbour pixels - not used in quality level 0 so initialize to "no edge" (1 is no edge, 0 is edge)
    float4 edgesLRTB = float4( 1.0, 1.0, 1.0, 1.0 );

    // Move center pixel slightly towards camera to avoid imprecision artifacts due to using of 16bit depth buffer; a lot smaller offsets needed when using 32bit floats
    pixCenterPos *= g_ASSAOConsts.DepthPrecisionOffsetMod;

    if( !adaptiveBase && (qualityLevel >= SSAO_DEPTH_BASED_EDGES_ENABLE_AT_QUALITY_PRESET) )
    {
        edgesLRTB = CalculateEdges( pixZ, pixLZ, pixRZ, pixTZ, pixBZ );
    }

    // adds a more high definition sharp effect, which gets blurred out (reuses left/right/top/bottom samples that we used for edge detection)
    if( !adaptiveBase && (qualityLevel >= SSAO_DETAIL_AO_ENABLE_AT_QUALITY_PRESET) )
    {
        // disable in case of quality level 4 (reference)
        if( qualityLevel != 4 )
        {
            //approximate neighbouring pixels positions (actually just deltas or "positions - pixCenterPos" )
            float3 viewspaceDirZNormalized = float3( pixCenterPos.xy / pixCenterPos.zz, 1.0 );
            float3 pixLDelta  = float3( -pixelDirRBViewspaceSizeAtCenterZ.x, 0.0, 0.0 ) + viewspaceDirZNormalized * (pixLZ - pixCenterPos.z); // very close approximation of: float3 pixLPos  = NDCToViewspace( normalizedScreenPos + float2( -g_ASSAOConsts.HalfViewportPixelSize.x, 0.0 ), pixLZ ).xyz - pixCenterPos.xyz;
            float3 pixRDelta  = float3( +pixelDirRBViewspaceSizeAtCenterZ.x, 0.0, 0.0 ) + viewspaceDirZNormalized * (pixRZ - pixCenterPos.z); // very close approximation of: float3 pixRPos  = NDCToViewspace( normalizedScreenPos + float2( +g_ASSAOConsts.HalfViewportPixelSize.x, 0.0 ), pixRZ ).xyz - pixCenterPos.xyz;
            float3 pixTDelta  = float3( 0.0, -pixelDirRBViewspaceSizeAtCenterZ.y, 0.0 ) + viewspaceDirZNormalized * (pixTZ - pixCenterPos.z); // very close approximation of: float3 pixTPos  = NDCToViewspace( normalizedScreenPos + float2( 0.0, -g_ASSAOConsts.HalfViewportPixelSize.y ), pixTZ ).xyz - pixCenterPos.xyz;
            float3 pixBDelta  = float3( 0.0, +pixelDirRBViewspaceSizeAtCenterZ.y, 0.0 ) + viewspaceDirZNormalized * (pixBZ - pixCenterPos.z); // very close approximation of: float3 pixBPos  = NDCToViewspace( normalizedScreenPos + float2( 0.0, +g_ASSAOConsts.HalfViewportPixelSize.y ), pixBZ ).xyz - pixCenterPos.xyz;

            const float rangeReductionConst         = 4.0f;                         // this is to avoid various artifacts
            const float modifiedFalloffCalcMulSq    = rangeReductionConst * falloffCalcMulSq;

            float4 additionalObscurance;
            additionalObscurance.x = CalculatePixelObscurance( pixelNormal, pixLDelta, modifiedFalloffCalcMulSq );
            additionalObscurance.y = CalculatePixelObscurance( pixelNormal, pixRDelta, modifiedFalloffCalcMulSq );
            additionalObscurance.z = CalculatePixelObscurance( pixelNormal, pixTDelta, modifiedFalloffCalcMulSq );
            additionalObscurance.w = CalculatePixelObscurance( pixelNormal, pixBDelta, modifiedFalloffCalcMulSq );

            obscuranceSum += g_ASSAOConsts.DetailAOStrength * dot( additionalObscurance, edgesLRTB );
        }
    }

    // Sharp normals also create edges - but this adds to the cost as well
    if( !adaptiveBase && (qualityLevel >= SSAO_NORMAL_BASED_EDGES_ENABLE_AT_QUALITY_PRESET ) )
    {
        float3 neighbourNormalL  = LoadNormal( int2(fullResCoord), int2( -2,  0 ) );
        float3 neighbourNormalR  = LoadNormal( int2(fullResCoord), int2(  2,  0 ) );
        float3 neighbourNormalT  = LoadNormal( int2(fullResCoord), int2(  0, -2 ) );
        float3 neighbourNormalB  = LoadNormal( int2(fullResCoord), int2(  0,  2 ) );

        const float dotThreshold = SSAO_NORMAL_BASED_EDGES_DOT_THRESHOLD;

        float4 normalEdgesLRTB;
        normalEdgesLRTB.x = saturate( (dot( pixelNormal, neighbourNormalL ) + dotThreshold ) );
        normalEdgesLRTB.y = saturate( (dot( pixelNormal, neighbourNormalR ) + dotThreshold ) );
        normalEdgesLRTB.z = saturate( (dot( pixelNormal, neighbourNormalT ) + dotThreshold ) );
        normalEdgesLRTB.w = saturate( (dot( pixelNormal, neighbourNormalB ) + dotThreshold ) );

//#define SSAO_SMOOTHEN_NORMALS // fixes some aliasing artifacts but kills a lot of high detail and adds to the cost - not worth it probably but feel free to play with it
#ifdef SSAO_SMOOTHEN_NORMALS
        //neighbourNormalL  = LoadNormal( fullResCoord, int2( -1,  0 ) );
        //neighbourNormalR  = LoadNormal( fullResCoord, int2(  1,  0 ) );
        //neighbourNormalT  = LoadNormal( fullResCoord, int2(  0, -1 ) );
        //neighbourNormalB  = LoadNormal( fullResCoord, int2(  0,  1 ) );
        pixelNormal += neighbourNormalL * edgesLRTB.x + neighbourNormalR * edgesLRTB.y + neighbourNormalT * edgesLRTB.z + neighbourNormalB * edgesLRTB.w;
        pixelNormal = normalize( pixelNormal );
#endif

        edgesLRTB *= normalEdgesLRTB;
    }

    const float globalMipOffset     = SSAO_DEPTH_MIPS_GLOBAL_OFFSET;
    float mipOffset = ( qualityLevel < SSAO_DEPTH_MIPS_ENABLE_AT_QUALITY_PRESET ) ? ( 0 ) : ( log2( pixLookupRadiusMod ) + globalMipOffset );

    // Used to tilt the second set of samples so that the disk is effectively rotated by the normal
    // effective at removing one set of artifacts, but too expensive for lower quality settings
    float2 normXY = float2( pixelNormal.x, pixelNormal.y );
    float normXYLength = length( normXY );
    normXY /= float2( normXYLength, -normXYLength );
    normXYLength *= SSAO_TILT_SAMPLES_AMOUNT;

    const float3 negViewspaceDir = -normalize( pixCenterPos );

    // standard, non-adaptive approach
    if( (qualityLevel != 3) || adaptiveBase )
    {
        // [unroll] // <- doesn't seem to help on any platform, although the compilers seem to unroll anyway if const number of tap used!
        for( int i = 0; i < numberOfTaps; i++ )
        {
            SSAOTap( qualityLevel, obscuranceSum, weightSum, i, rotScale, pixCenterPos, negViewspaceDir, pixelNormal,
                     normalizedScreenPos, mipOffset, falloffCalcMulSq, 1.0, normXY, normXYLength );
        }
    }
    else // if( qualityLevel == 3 ) adaptive approach
    {
        // add new ones if needed
        float2 fullResUV = normalizedScreenPos + g_ASSAOConsts.PerPassFullResUVOffset.xy;
//        float importance = g_ImportanceMap.SampleLevel( g_LinearClampSampler, fullResUV, 0.0 ).x;
		float importance = textureLod(g_ImportanceMap, fullResUV, 0.0 ).x;

        // this is to normalize SSAO_DETAIL_AO_AMOUNT across all pixel regardless of importance
        obscuranceSum *= (SSAO_ADAPTIVE_TAP_BASE_COUNT / float(SSAO_MAX_TAPS)) + (importance * SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT / float(SSAO_MAX_TAPS));

        // load existing base values
        float2 baseValues = texelFetch(g_FinalSSAO, int3( SVPosui, g_ASSAOConsts.PassIndex), 0).xy;
        weightSum += baseValues.y * (SSAO_ADAPTIVE_TAP_BASE_COUNT * 4.0);
        obscuranceSum += (baseValues.x) * weightSum;
        
        // increase importance around edges
        float edgeCount = dot( 1.0-edgesLRTB, float4( 1.0, 1.0, 1.0, 1.0 ) );
        //importance += edgeCount / (float)SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT;

        float avgTotalImportance = float(/*imageLoad(g_LoadCounter, int2(0))*/  texelFetch(g_LoadCounter, 0, 0).x) * g_ASSAOConsts.LoadCounterAvgDiv;

        float importanceLimiter = saturate( g_ASSAOConsts.AdaptiveSampleCountLimit / avgTotalImportance );
        importance *= importanceLimiter;

        float additionalSampleCountFlt = SSAO_ADAPTIVE_TAP_FLEXIBLE_COUNT * importance;

        const float blendRange = 3.0; // use 1 to just blend the last one; use larger number to blend over more for a more smooth transition
        const float blendRangeInv = 1.0 / blendRange;

        additionalSampleCountFlt += 0.5;
        uint additionalSamples   = uint( additionalSampleCountFlt );
        uint additionalSamplesTo = min( SSAO_MAX_TAPS, additionalSamples + SSAO_ADAPTIVE_TAP_BASE_COUNT );

        // additional manual unroll doesn't help unfortunately
//        [loop]
        for( uint i = SSAO_ADAPTIVE_TAP_BASE_COUNT; i < additionalSamplesTo; i++ )
        {
            additionalSampleCountFlt -= 1.0f;
            float weightMod = saturate(additionalSampleCountFlt * blendRangeInv); // slowly blend in the last few samples
            SSAOTap( qualityLevel, obscuranceSum, weightSum, int(i), rotScale, pixCenterPos, negViewspaceDir, pixelNormal, normalizedScreenPos,
                     mipOffset, falloffCalcMulSq, weightMod, normXY, normXYLength );
        }
    }

    // early out for adaptive base - just output weight (used for the next pass)
    if( adaptiveBase )
    {
        float obscurance = obscuranceSum / weightSum;

        outShadowTerm   = obscurance;
        outEdges        = float4(0);
        outWeight       = weightSum;
        return;
    }

    // calculate weighted average
    float obscurance = obscuranceSum / weightSum;

    // calculate fadeout (1 close, gradient, 0 far)
    float fadeOut = saturate( pixCenterPos.z * g_ASSAOConsts.EffectFadeOutMul + g_ASSAOConsts.EffectFadeOutAdd );
  
    // Reduce the SSAO shadowing if we're on the edge to remove artifacts on edges (we don't care for the lower quality one)
    if( !adaptiveBase && (qualityLevel >= SSAO_DEPTH_BASED_EDGES_ENABLE_AT_QUALITY_PRESET) )
    {
        // float edgeCount = dot( 1.0-edgesLRTB, float4( 1.0, 1.0, 1.0, 1.0 ) );

        // when there's more than 2 opposite edges, start fading out the occlusion to reduce aliasing artifacts
        float edgeFadeoutFactor = saturate( (1.0 - edgesLRTB.x - edgesLRTB.y) * 0.35) + saturate( (1.0 - edgesLRTB.z - edgesLRTB.w) * 0.35 );

        // (experimental) if you want to reduce the effect next to any edge
        // edgeFadeoutFactor += 0.1 * saturate( dot( 1 - edgesLRTB, float4( 1, 1, 1, 1 ) ) );

        fadeOut *= saturate( 1.0 - edgeFadeoutFactor );
    }
    
    // same as a bove, but a lot more conservative version
    // fadeOut *= saturate( dot( edgesLRTB, float4( 0.9, 0.9, 0.9, 0.9 ) ) - 2.6 );

    // strength
    obscurance = g_ASSAOConsts.EffectShadowStrength * obscurance;
    
    // clamp
    obscurance = min( obscurance, g_ASSAOConsts.EffectShadowClamp );
    
    // fadeout
    obscurance *= fadeOut;

    // conceptually switch to occlusion with the meaning being visibility (grows with visibility, occlusion == 1 implies full visibility), 
    // to be in line with what is more commonly used.
    float occlusion = 1.0 - obscurance;

    // modify the gradient
    // note: this cannot be moved to a later pass because of loss of precision after storing in the render target
    occlusion = pow( saturate( occlusion ), g_ASSAOConsts.EffectShadowPow );

    // outputs!
    outShadowTerm   = occlusion;    // Our final 'occlusion' term (0 means fully occluded, 1 means fully lit)
    outEdges        = edgesLRTB;    // These are used to prevent blurring across edges, 1 means no edge, 0 means edge, 0.5 means half way there, etc.
    outWeight       = weightSum;
}

// ********************************************************************************************************
// Pixel shader that does smart blurring (to avoid bleeding)
void AddSample( float ssaoValue, float edgeValue, inout float sum, inout float sumWeight )
{
    float weight = edgeValue;    

    sum += (weight * ssaoValue);
    sumWeight += weight;
}

float2 SampleBlurredWide( float4 inPos, float2 coord )
{
    float2 vC           = textureLodOffset( g_BlurInput, coord, 0.0, int2( 0,  0 ) ).xy;   //g_PointMirrorSampler
    float2 vL           = textureLodOffset( g_BlurInput, coord, 0.0, int2( -2, 0 ) ).xy;
    float2 vT           = textureLodOffset( g_BlurInput, coord, 0.0, int2( 0, -2 ) ).xy;
    float2 vR           = textureLodOffset( g_BlurInput, coord, 0.0, int2(  2, 0 ) ).xy;
    float2 vB           = textureLodOffset( g_BlurInput, coord, 0.0, int2( 0,  2 ) ).xy;

    float packedEdges   = vC.y;
    float4 edgesLRTB    = UnpackEdges( packedEdges );
    edgesLRTB.x         *= UnpackEdges( vL.y ).y;
    edgesLRTB.z         *= UnpackEdges( vT.y ).w;
    edgesLRTB.y         *= UnpackEdges( vR.y ).x;
    edgesLRTB.w         *= UnpackEdges( vB.y ).z;

    float ssaoValue     = vC.x;
    float ssaoValueL    = vL.x;
    float ssaoValueT    = vT.x;
    float ssaoValueR    = vR.x;
    float ssaoValueB    = vB.x;

    float sumWeight = 0.8f;
    float sum = ssaoValue * sumWeight;

    AddSample( ssaoValueL, edgesLRTB.x, sum, sumWeight );
    AddSample( ssaoValueR, edgesLRTB.y, sum, sumWeight );
    AddSample( ssaoValueT, edgesLRTB.z, sum, sumWeight );
    AddSample( ssaoValueB, edgesLRTB.w, sum, sumWeight );

    float ssaoAvg = sum / sumWeight;

    ssaoValue = ssaoAvg; //min( ssaoValue, ssaoAvg ) * 0.2 + ssaoAvg * 0.8;

    return float2( ssaoValue, packedEdges );
}

float2 SampleBlurred( float4 inPos, float2 coord )
{
    float packedEdges   = texelFetch(g_BlurInput, int2(inPos.xy), 0 ).y;
    float4 edgesLRTB    = UnpackEdges( packedEdges );

    float4 valuesUL     = textureGather( g_BlurInput, coord - g_ASSAOConsts.HalfViewportPixelSize * 0.5 );  // g_PointMirrorSampler
    float4 valuesBR     = textureGather( g_BlurInput, coord + g_ASSAOConsts.HalfViewportPixelSize * 0.5 );

    float ssaoValue     = valuesUL.y;
    float ssaoValueL    = valuesUL.x;
    float ssaoValueT    = valuesUL.z;
    float ssaoValueR    = valuesBR.z;
    float ssaoValueB    = valuesBR.x;

    float sumWeight = 0.5f;
    float sum = ssaoValue * sumWeight;

    AddSample( ssaoValueL, edgesLRTB.x, sum, sumWeight );
    AddSample( ssaoValueR, edgesLRTB.y, sum, sumWeight );

    AddSample( ssaoValueT, edgesLRTB.z, sum, sumWeight );
    AddSample( ssaoValueB, edgesLRTB.w, sum, sumWeight );

    float ssaoAvg = sum / sumWeight;

    ssaoValue = ssaoAvg; //min( ssaoValue, ssaoAvg ) * 0.2 + ssaoAvg * 0.8;

    return float2( ssaoValue, packedEdges );
}