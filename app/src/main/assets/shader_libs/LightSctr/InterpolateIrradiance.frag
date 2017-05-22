#include "PostProcessingLightScatteringCommon.frag"

layout(location = 0) out float4 OutColor;
in vec4 m_f4UVAndScreenPos;

void main()
{
	int uiSampleInd = int(gl_FragCoord.x);
    uint uiSliceInd = uint(gl_FragCoord.y);
    // Get interpolation sources
    uint2 ui2InterpolationSources = // g_tex2DInterpolationSource.Load( uint3(uiSampleInd, uiSliceInd, 0) );
    								texelFetch(g_tex2DInterpolationSource, int2(uiSampleInd, uiSliceInd), 0).xy;
    float fInterpolationPos = float(uiSampleInd - ui2InterpolationSources.x) / float( max(ui2InterpolationSources.y - ui2InterpolationSources.x,1) );

    float3 f3Src0 = // g_tex2DInitialInsctrIrradiance.Load( uint3(ui2InterpolationSources.x, uiSliceInd, 0) );
    					texelFetch(g_tex2DInitialInsctrIrradiance, int2(ui2InterpolationSources.x, uiSliceInd), 0).xyz;
    float3 f3Src1 = // g_tex2DInitialInsctrIrradiance.Load( uint3(ui2InterpolationSources.y, uiSliceInd, 0));
    					texelFetch(g_tex2DInitialInsctrIrradiance, int2(ui2InterpolationSources.y, uiSliceInd), 0).xyz;

    // Ray marching samples are interpolated from themselves
    OutColor.rgb = lerp(f3Src0, f3Src1, fInterpolationPos);
    OutColor.a = 0.0;
}