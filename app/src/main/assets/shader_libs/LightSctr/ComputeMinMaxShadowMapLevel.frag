
#include "PostProcessingLightScatteringCommon.frag"

layout(location = 0) out float4 OutColor;

//1D min max mip map is arranged as follows:
//
// g_MiscParams.ui4SrcDstMinMaxLevelOffset.x
//  |
//  |      g_MiscParams.ui4SrcDstMinMaxLevelOffset.z
//  |_______|____ __
//  |       |    |  |
//  |       |    |  |
//  |       |    |  |
//  |       |    |  |
//  |_______|____|__|
//  |<----->|<-->|
//      |     |
//      |    uiMinMaxShadowMapResolution/
//   uiMinMaxShadowMapResolution/2
//                      
void main()
{
	int2 uiDstSampleInd = int2(gl_FragCoord.xy);
    int2 uiSrcSample0Ind = int2(g_ui4SrcDstMinMaxLevelOffset.x + (uiDstSampleInd.x - g_ui4SrcDstMinMaxLevelOffset.z)*2, uiDstSampleInd.y);
    int2 uiSrcSample1Ind = uiSrcSample0Ind + int2(1,0);
    float4 fnMinMaxDepth0 = // g_tex2DMinMaxLightSpaceDepth.Load( uint3(uiSrcSample0Ind,0) );
    						texelFetch(g_tex2DMinMaxLightSpaceDepth, uiSrcSample0Ind, 0);
    float4 fnMinMaxDepth1 = // g_tex2DMinMaxLightSpaceDepth.Load( uint3(uiSrcSample1Ind,0) );
    						texelFetch(g_tex2DMinMaxLightSpaceDepth, uiSrcSample1Ind, 0);
#if ACCEL_STRUCT == ACCEL_STRUCT_MIN_MAX_TREE
    float2 f2MinMaxDepth;
    f2MinMaxDepth.x = min(fnMinMaxDepth0.x, fnMinMaxDepth1.x);
    f2MinMaxDepth.y = max(fnMinMaxDepth0.y, fnMinMaxDepth1.y);
    OutColor = float4(f2MinMaxDepth, 0, 0);
#elif ACCEL_STRUCT == ACCEL_STRUCT_BV_TREE

    float4 f4MinMaxDepth;
    //
    //                fnMinMaxDepth0.z        fnMinMaxDepth1.z
    //                      *                       *
    //                                 *
    //           *              fnMinMaxDepth1.x
    //  fnMinMaxDepth0.x
    // Start by drawing line from the first to the last points:
    f4MinMaxDepth.x = fnMinMaxDepth0.x;
    f4MinMaxDepth.z = fnMinMaxDepth1.z;
    // Check if second and first points are above the line and update its ends if required 
    float fDelta = lerp(f4MinMaxDepth.x, f4MinMaxDepth.z, 1.f/3.f) - fnMinMaxDepth0.z;
    f4MinMaxDepth.x -= 3.f/2.f * max(fDelta, 0);
    fDelta = lerp(f4MinMaxDepth.x, f4MinMaxDepth.z, 2.f/3.f) - fnMinMaxDepth1.x;
    f4MinMaxDepth.z -= 3.f/2.f * max(fDelta, 0);

    //
    //                fnMinMaxDepth0.w        fnMinMaxDepth1.w
    //                      *                       *
    //                                 *
    //           *              fnMinMaxDepth1.y
    //  fnMinMaxDepth0.y  
    f4MinMaxDepth.y = fnMinMaxDepth0.y;
    f4MinMaxDepth.w = fnMinMaxDepth1.w;
    fDelta = fnMinMaxDepth0.w - lerp(f4MinMaxDepth.y, f4MinMaxDepth.w, 1.f/3.f);
    f4MinMaxDepth.y += 3.f/2.f * max(fDelta, 0);
    fDelta = fnMinMaxDepth1.y - lerp(f4MinMaxDepth.y, f4MinMaxDepth.w, 2.f/3.f);
    f4MinMaxDepth.w += 3.f/2.f * max(fDelta, 0);
    
    // Check if the horizontal bounding box is better
    float2 f2MaxDepth = max(fnMinMaxDepth0.yw, fnMinMaxDepth1.yw);
    float fMaxDepth = max(f2MaxDepth.x, f2MaxDepth.y);

    float2 f2MinDepth = min(fnMinMaxDepth0.xz, fnMinMaxDepth1.xz);
    float fMinDepth = min(f2MinDepth.x, f2MinDepth.y);

    float fThreshold = (fMaxDepth-fMinDepth) * 0.01;
    if( any(greaterThan(f4MinMaxDepth.yw, float2(fMaxDepth + fThreshold))) )
        f4MinMaxDepth.yw = fMaxDepth;

    if( any(lessThan(f4MinMaxDepth.xz, float2(fMinDepth - fThreshold))) )
        f4MinMaxDepth.xz = fMinDepth;

    OutColor = f4MinMaxDepth;
#endif
	
}