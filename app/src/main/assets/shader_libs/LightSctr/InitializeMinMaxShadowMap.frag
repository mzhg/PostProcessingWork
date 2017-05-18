
#include "PostProcessingLightScatteringCommon.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float4 OutColor;

//Note that min/max shadow map does not contain finest resolution level
//The first level it contains corresponds to step == 2
void main()
{
	int uiSliceInd = int(gl_FragCoord.y);
    // Load slice direction in shadow map
    float4 f4SliceUVDirAndOrigin = // g_tex2DSliceUVDirAndOrigin.Load( uint3(uiSliceInd,0,0) );
    								texelFetch(g_tex2DSliceUVDirAndOrigin, int2(uiSliceInd, 0), 0);
    // Calculate current sample position on the ray
    float2 f2CurrUV = f4SliceUVDirAndOrigin.zw + f4SliceUVDirAndOrigin.xy * floor(gl_FragCoord.x) * 2.f * g_f2ShadowMapTexelSize;
    
    // Gather 8 depths which will be used for PCF filtering for this sample and its immediate neighbor 
    // along the epipolar slice
    // Note that if the sample is located outside the shadow map, Gather() will return 0 as 
    // specified by the samLinearBorder0. As a result volumes outside the shadow map will always be lit
    float4 f4Depths = // g_tex2DLightSpaceDepthMap.Gather(samLinearBorder0, f2CurrUV);
    					textureGather(g_tex2DLightSpaceDepthBuffer, f2CurrUV);
    // Shift UV to the next sample along the epipolar slice:
    f2CurrUV += f4SliceUVDirAndOrigin.xy * g_f2ShadowMapTexelSize;
    float4 f4NeighbDepths = // g_tex2DLightSpaceDepthMap.Gather(samLinearBorder0, f2CurrUV);
    							textureGather(g_tex2DLightSpaceDepthBuffer, f2CurrUV);

#if ACCEL_STRUCT == ACCEL_STRUCT_MIN_MAX_TREE
    
    float4 f4MinDepth = min(f4Depths, f4NeighbDepths);
    f4MinDepth.xy = min(f4MinDepth.xy, f4MinDepth.zw);
    f4MinDepth.x = min(f4MinDepth.x, f4MinDepth.y);

    float4 f4MaxDepth = max(f4Depths, f4NeighbDepths);
    f4MaxDepth.xy = max(f4MaxDepth.xy, f4MaxDepth.zw);
    f4MaxDepth.x = max(f4MaxDepth.x, f4MaxDepth.y);

    OutColor = float4(f4MinDepth.x, f4MaxDepth.x, 0, 0);

#elif ACCEL_STRUCT == ACCEL_STRUCT_BV_TREE
    
    // Calculate min/max depths for current and next sampling locations
    float2 f2MinDepth = min(f4Depths.xy, f4Depths.zw);
    float fMinDepth = min(f2MinDepth.x, f2MinDepth.y);
    float2 f2MaxDepth = max(f4Depths.xy, f4Depths.zw);
    float fMaxDepth = max(f2MaxDepth.x, f2MaxDepth.y);
    float2 f2NeighbMinDepth = min(f4NeighbDepths.xy, f4NeighbDepths.zw);
    float fNeighbMinDepth = min(f2NeighbMinDepth.x, f2NeighbMinDepth.y);
    float2 f2NeighbMaxDepth = max(f4NeighbDepths.xy, f4NeighbDepths.zw);
    float fNeighbMaxDepth = max(f2NeighbMaxDepth.x, f2NeighbMaxDepth.y);

    OutColor = float4( fMinDepth, fMaxDepth, fNeighbMinDepth, fNeighbMaxDepth );

#endif
}