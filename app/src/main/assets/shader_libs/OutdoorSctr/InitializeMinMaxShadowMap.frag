
#include "Scattering.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float2 OutColor;

void main()
{
	uint uiSliceInd;
    float fCascadeInd;
#if USE_COMBINED_MIN_MAX_TEXTURE
    fCascadeInd = floor(gl_FragCoord.y / NUM_EPIPOLAR_SLICES);
    uiSliceInd = uint(gl_FragCoord.y - fCascadeInd * NUM_EPIPOLAR_SLICES);
    fCascadeInd += float(g_iFirstCascade);
#else
    uiSliceInd = uint(gl_FragCoord.y);
    fCascadeInd = g_fCascadeInd;
#endif
    // Load slice direction in shadow map
    float4 f4SliceUVDirAndOrigin = 
//    					g_tex2DSliceUVDirAndOrigin.Load( uint3(uiSliceInd, fCascadeInd, 0) );
						texelFetch(g_tex2DSliceUVDirAndOrigin, int2(uiSliceInd, fCascadeInd), 0);
    // Calculate current sample position on the ray
    float2 f2CurrUV = f4SliceUVDirAndOrigin.zw + f4SliceUVDirAndOrigin.xy * floor(gl_FragCoord.x) * 2.f;
    
    float4 f4MinDepth = float4(1);
    float4 f4MaxDepth = float4(0);
    // Gather 8 depths which will be used for PCF filtering for this sample and its immediate neighbor 
    // along the epipolar slice
    // Note that if the sample is located outside the shadow map, Gather() will return 0 as 
    // specified by the samLinearBorder0. As a result volumes outside the shadow map will always be lit
    for( float i=0; i<=1; ++i )
    {
//        float4 f4Depths = g_tex2DLightSpaceDepthMap.Gather(samLinearBorder0, float3(f2CurrUV + i * f4SliceUVDirAndOrigin.xy, fCascadeInd) );
		float4 f4Depths = textureGather(g_tex2DLightSpaceDepthMap, float3(f2CurrUV + i * f4SliceUVDirAndOrigin.xy, fCascadeInd));
        f4MinDepth = min(f4MinDepth, f4Depths);
        f4MaxDepth = max(f4MaxDepth, f4Depths);
    }

    f4MinDepth.xy = min(f4MinDepth.xy, f4MinDepth.zw);
    f4MinDepth.x = min(f4MinDepth.x, f4MinDepth.y);

    f4MaxDepth.xy = max(f4MaxDepth.xy, f4MaxDepth.zw);
    f4MaxDepth.x = max(f4MaxDepth.x, f4MaxDepth.y);
#if !IS_32BIT_MIN_MAX_MAP
    const float R16_UNORM_PRECISION = 1.f / float(1<<16);
    f4MinDepth.x = floor(f4MinDepth.x/R16_UNORM_PRECISION)*R16_UNORM_PRECISION;
    f4MaxDepth.x =  ceil(f4MaxDepth.x/R16_UNORM_PRECISION)*R16_UNORM_PRECISION;
#endif
    OutColor= float2(f4MinDepth.x, f4MaxDepth.x);
}