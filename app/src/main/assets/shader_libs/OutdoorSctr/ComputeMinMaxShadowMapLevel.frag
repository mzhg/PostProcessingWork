
#include "Scattering.frag"

in float4 m_f4UVAndScreenPos;

layout(location = 0) out float2 OutColor;

void main()
{
	int2 uiDstSampleInd = int2(gl_FragCoord.xy);
    int2 uiSrcSample0Ind = int2(g_ui4SrcDstMinMaxLevelOffset.x + (uiDstSampleInd.x - g_ui4SrcDstMinMaxLevelOffset.z) * 2, uiDstSampleInd.y);
    int2 uiSrcSample1Ind = (uiSrcSample0Ind + int2(1,0));
    float2 fnMinMaxDepth0 = 
//    				g_tex2DMinMaxLightSpaceDepth.Load( uint3(uiSrcSample0Ind,0) );
    				texelFetch(g_tex2DMinMaxLightSpaceDepth, uiSrcSample0Ind, 0).xy;
    float2 fnMinMaxDepth1 = 
//    				g_tex2DMinMaxLightSpaceDepth.Load( uint3(uiSrcSample1Ind,0) );
    				texelFetch(g_tex2DMinMaxLightSpaceDepth, uiSrcSample1Ind, 0).xy;

    float2 f2MinMaxDepth;
    f2MinMaxDepth.x = min(fnMinMaxDepth0.x, fnMinMaxDepth1.x);
    f2MinMaxDepth.y = max(fnMinMaxDepth0.y, fnMinMaxDepth1.y);
    OutColor = f2MinMaxDepth;
}