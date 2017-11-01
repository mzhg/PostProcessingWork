#include "ASSAOCommon.frag"

layout(location = 0) out float4 Out_f4Color; 

void main()
{
	float ao;
    int2 pixPos     = int2(gl_FragCoord.xy);
    int2 pixPosHalf = pixPos / int2(2, 2);

    // calculate index in the four deinterleaved source array texture
    int mx = (pixPos.x % 2);
    int my = (pixPos.y % 2);
    int ic = mx + my * 2;       // center index
    int ih = (1-mx) + my * 2;   // neighbouring, horizontal
    int iv = mx + (1-my) * 2;   // neighbouring, vertical
    int id = (1-mx) + (1-my)*2; // diagonal
    
    float2 centerVal = texelFetch(g_FinalSSAO, ivec3(pixPosHalf, ic), 0).xy;
    
    ao = centerVal.x;

#if 1   // for debugging - set to 0 to disable last pass high-res blur
    float4 edgesLRTB = UnpackEdges( centerVal.y );
    float4 inPos     = gl_FragCoord;

    // return 1.0 - float4( edgesLRTB.x, edgesLRTB.y * 0.5 + edgesLRTB.w * 0.5, edgesLRTB.z, 0.0 ); // debug show edges

    // convert index shifts to sampling offsets
    float fmx   = float(mx);
    float fmy   = float(my);
    
    // in case of an edge, push sampling offsets away from the edge (towards pixel center)
    float fmxe  = (edgesLRTB.y - edgesLRTB.x);
    float fmye  = (edgesLRTB.w - edgesLRTB.z);

    // calculate final sampling offsets and sample using bilinear filter
    float2  uvH = (inPos.xy + float2( fmx + fmxe - 0.5, 0.5 - fmy ) ) * 0.5 * g_ASSAOConsts.HalfViewportPixelSize;
    float   aoH = textureLod( g_FinalSSAO, float3( uvH, ih ), 0 ).x;  // g_LinearClampSampler
    float2  uvV = (inPos.xy + float2( 0.5 - fmx, fmy - 0.5 + fmye ) ) * 0.5 * g_ASSAOConsts.HalfViewportPixelSize;
    float   aoV = textureLod( g_FinalSSAO, float3( uvV, iv ), 0 ).x;  // g_LinearClampSampler
    float2  uvD = (inPos.xy + float2( fmx - 0.5 + fmxe, fmy - 0.5 + fmye ) ) * 0.5 * g_ASSAOConsts.HalfViewportPixelSize;
    float   aoD = textureLod( g_FinalSSAO, float3( uvD, id ), 0 ).x;  // g_LinearClampSampler

    // reduce weight for samples near edge - if the edge is on both sides, weight goes to 0
    float4 blendWeights;
    blendWeights.x = 1.0;
    blendWeights.y = (edgesLRTB.x + edgesLRTB.y) * 0.5;
    blendWeights.z = (edgesLRTB.z + edgesLRTB.w) * 0.5;
    blendWeights.w = (blendWeights.y + blendWeights.z) * 0.5;

    // calculate weighted average
    float blendWeightsSum   = dot( blendWeights, float4( 1.0, 1.0, 1.0, 1.0 ) );
    ao = dot( float4( ao, aoH, aoV, aoD ), blendWeights ) / blendWeightsSum;
#endif

    Out_f4Color = float4(ao.xxx, 0 );
}