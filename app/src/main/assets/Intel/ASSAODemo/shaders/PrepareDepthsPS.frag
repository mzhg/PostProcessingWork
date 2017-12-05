#include "ASSAOCommon.frag"

layout(location = 0) out float Out_fColor0; 
layout(location = 1) out float Out_fColor1; 
layout(location = 2) out float Out_fColor2; 
layout(location = 3) out float Out_fColor3;

void main()
{
#if 0   // gather can be a bit faster but doesn't work with input depth buffers that don't match the working viewport
    float2 gatherUV = inPos.xy * g_ASSAOConsts.Viewport2xPixelSize;
    float4 depths = g_DepthSource.GatherRed( g_PointClampSampler, gatherUV );
    float a = depths.w;  // g_DepthSource.Load( int3( int2(inPos.xy) * 2, 0 ), int2( 0, 0 ) ).x;
    float b = depths.z;  // g_DepthSource.Load( int3( int2(inPos.xy) * 2, 0 ), int2( 1, 0 ) ).x;
    float c = depths.x;  // g_DepthSource.Load( int3( int2(inPos.xy) * 2, 0 ), int2( 0, 1 ) ).x;
    float d = depths.y;  // g_DepthSource.Load( int3( int2(inPos.xy) * 2, 0 ), int2( 1, 1 ) ).x;
#else
    int3 baseCoord = int3( int2(gl_FragCoord.xy) * 2, 0 );
//    float a = g_DepthSource.Load( baseCoord, int2( 0, 0 ) ).x;
//    float b = g_DepthSource.Load( baseCoord, int2( 1, 0 ) ).x;
//    float c = g_DepthSource.Load( baseCoord, int2( 0, 1 ) ).x;
//    float d = g_DepthSource.Load( baseCoord, int2( 1, 1 ) ).x;
	float a = texelFetch(      g_DepthSource, baseCoord.xy, baseCoord.z).x;
	float b = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 1, 0 )).x;
	float c = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 0, 1 )).x;
	float d = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 1, 1 )).x;
#endif

    Out_fColor0 = ScreenSpaceToViewSpaceDepth( a );
    Out_fColor1 = ScreenSpaceToViewSpaceDepth( b );
    Out_fColor2 = ScreenSpaceToViewSpaceDepth( c );
    Out_fColor3 = ScreenSpaceToViewSpaceDepth( d );
} 