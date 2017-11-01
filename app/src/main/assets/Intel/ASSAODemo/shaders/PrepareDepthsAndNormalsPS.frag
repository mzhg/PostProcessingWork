#include "ASSAOCommon.frag"

layout(location = 0) out float Out_fColor0; 
layout(location = 1) out float Out_fColor1; 
layout(location = 2) out float Out_fColor2; 
layout(location = 3) out float Out_fColor3;

layout (rgba8, binding = 0) uniform image2D g_NormalsOutputUAV;

void main()
{
	float2 inPos = gl_FragCoord.xy;
	int2 baseCoords = int2(inPos.xy) * 2;
    float2 upperLeftUV = (inPos.xy - 0.25) * g_ASSAOConsts.Viewport2xPixelSize;

#if 0   // gather can be a bit faster but doesn't work with input depth buffers that don't match the working viewport
    float2 gatherUV = inPos.xy * g_ASSAOConsts.Viewport2xPixelSize;
    float4 depths = g_DepthSource.GatherRed( g_PointClampSampler, gatherUV );
    out0 = ScreenSpaceToViewSpaceDepth( depths.w );
    out1 = ScreenSpaceToViewSpaceDepth( depths.z );
    out2 = ScreenSpaceToViewSpaceDepth( depths.x );
    out3 = ScreenSpaceToViewSpaceDepth( depths.y );
#else
    int3 baseCoord = int3( int2(inPos.xy) * 2, 0 );
//    out0 = ScreenSpaceToViewSpaceDepth( g_DepthSource.Load( baseCoord, int2( 0, 0 ) ).x );
//    out1 = ScreenSpaceToViewSpaceDepth( g_DepthSource.Load( baseCoord, int2( 1, 0 ) ).x );
//    out2 = ScreenSpaceToViewSpaceDepth( g_DepthSource.Load( baseCoord, int2( 0, 1 ) ).x );
//    out3 = ScreenSpaceToViewSpaceDepth( g_DepthSource.Load( baseCoord, int2( 1, 1 ) ).x );
    
    float a = texelFetch(g_DepthSource, baseCoord.xy, baseCoord.z).x;
	float b = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 1, 0 )).x;
	float c = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 0, 1 )).x;
	float d = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 1, 1 )).x;
	
	Out_fColor0 = ScreenSpaceToViewSpaceDepth( a );
    Out_fColor1 = ScreenSpaceToViewSpaceDepth( b );
    Out_fColor2 = ScreenSpaceToViewSpaceDepth( c );
    Out_fColor3 = ScreenSpaceToViewSpaceDepth( d );
#endif

    float pixZs[4][4];

    // middle 4
    pixZs[1][1] = Out_fColor0;
    pixZs[2][1] = Out_fColor1;
    pixZs[1][2] = Out_fColor2;
    pixZs[2][2] = Out_fColor3;
    // left 2
    pixZs[0][1] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2( -1, 0 ) ).x ); // g_PointClampSampler
    pixZs[0][2] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2( -1, 1 ) ).x ); 
    // right 2
    pixZs[3][1] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2(  2, 0 ) ).x ); 
    pixZs[3][2] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2(  2, 1 ) ).x ); 
    // top 2
    pixZs[1][0] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2(  0, -1 ) ).x );
    pixZs[2][0] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2(  1, -1 ) ).x );
    // bottom 2
    pixZs[1][3] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2(  0,  2 ) ).x );
    pixZs[2][3] = ScreenSpaceToViewSpaceDepth(  textureLodOffset( g_DepthSource, upperLeftUV, 0.0, int2(  1,  2 ) ).x );

    float4 edges0 = CalculateEdges( pixZs[1][1], pixZs[0][1], pixZs[2][1], pixZs[1][0], pixZs[1][2] );
    float4 edges1 = CalculateEdges( pixZs[2][1], pixZs[1][1], pixZs[3][1], pixZs[2][0], pixZs[2][2] );
    float4 edges2 = CalculateEdges( pixZs[1][2], pixZs[0][2], pixZs[2][2], pixZs[1][1], pixZs[1][3] );
    float4 edges3 = CalculateEdges( pixZs[2][2], pixZs[1][2], pixZs[3][2], pixZs[2][1], pixZs[2][3] );

    float3 pixPos[4][4];
    // middle 4
    pixPos[1][1] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 0.0,  0.0 ), pixZs[1][1] );
    pixPos[2][1] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 1.0,  0.0 ), pixZs[2][1] );
    pixPos[1][2] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 0.0,  1.0 ), pixZs[1][2] );
    pixPos[2][2] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 1.0,  1.0 ), pixZs[2][2] );
    // left 2
    pixPos[0][1] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( -1.0,  0.0), pixZs[0][1] );
    pixPos[0][2] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( -1.0,  1.0), pixZs[0][2] );
    // right 2                                                                                     
    pixPos[3][1] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2(  2.0,  0.0), pixZs[3][1] );
    pixPos[3][2] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2(  2.0,  1.0), pixZs[3][2] );
    // top 2                                                                                       
    pixPos[1][0] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 0.0, -1.0 ), pixZs[1][0] );
    pixPos[2][0] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 1.0, -1.0 ), pixZs[2][0] );
    // bottom 2                                                                                    
    pixPos[1][3] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 0.0,  2.0 ), pixZs[1][3] );
    pixPos[2][3] = NDCToViewspace( upperLeftUV + g_ASSAOConsts.ViewportPixelSize * float2( 1.0,  2.0 ), pixZs[2][3] );

    float3 norm0 = CalculateNormal( edges0, pixPos[1][1], pixPos[0][1], pixPos[2][1], pixPos[1][0], pixPos[1][2] );
    float3 norm1 = CalculateNormal( edges1, pixPos[2][1], pixPos[1][1], pixPos[3][1], pixPos[2][0], pixPos[2][2] );
    float3 norm2 = CalculateNormal( edges2, pixPos[1][2], pixPos[0][2], pixPos[2][2], pixPos[1][1], pixPos[1][3] );
    float3 norm3 = CalculateNormal( edges3, pixPos[2][2], pixPos[1][2], pixPos[3][2], pixPos[2][1], pixPos[2][3] );

//    g_NormalsOutputUAV[ baseCoords + int2( 0, 0 ) ] = float4( norm0 * 0.5 + 0.5, 0.0 );
//    g_NormalsOutputUAV[ baseCoords + int2( 1, 0 ) ] = float4( norm1 * 0.5 + 0.5, 0.0 );
//    g_NormalsOutputUAV[ baseCoords + int2( 0, 1 ) ] = float4( norm2 * 0.5 + 0.5, 0.0 );
//    g_NormalsOutputUAV[ baseCoords + int2( 1, 1 ) ] = float4( norm3 * 0.5 + 0.5, 0.0 );
    
    imageStore(g_NormalsOutputUAV, baseCoords + int2( 0, 0 ), float4( norm0 * 0.5 + 0.5, 0.0 ));
    imageStore(g_NormalsOutputUAV, baseCoords + int2( 1, 0 ), float4( norm1 * 0.5 + 0.5, 0.0 ));
    imageStore(g_NormalsOutputUAV, baseCoords + int2( 0, 1 ), float4( norm2 * 0.5 + 0.5, 0.0 ));
    imageStore(g_NormalsOutputUAV, baseCoords + int2( 1, 1 ), float4( norm3 * 0.5 + 0.5, 0.0 ));
}