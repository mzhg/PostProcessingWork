#include "ASSAOCommon.frag"

// #define MIP_LEVEL 
layout(location = 0) out float Out_fColor0; 
layout(location = 1) out float Out_fColor1; 
layout(location = 2) out float Out_fColor2; 
layout(location = 3) out float Out_fColor3;

void main()
{
	int2 baseCoords = int2(gl_FragCoord.xy) * 2;

    float4 depthsArr[4];
    float depthsOutArr[4];
    
    // how to Gather a specific mip level?
    depthsArr[0].x = texelFetchOffset(g_ViewspaceDepthSource, baseCoords, MIP_LEVEL, int2( 0, 0 )).x ;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[0].y = texelFetchOffset(g_ViewspaceDepthSource, baseCoords, MIP_LEVEL, int2( 1, 0 )).x ;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[0].z = texelFetchOffset(g_ViewspaceDepthSource, baseCoords, MIP_LEVEL, int2( 0, 1 )).x ;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[0].w = texelFetchOffset(g_ViewspaceDepthSource, baseCoords, MIP_LEVEL, int2( 1, 1 )).x ;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[1].x = texelFetchOffset(g_ViewspaceDepthSource1,baseCoords, MIP_LEVEL, int2( 0, 0 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[1].y = texelFetchOffset(g_ViewspaceDepthSource1,baseCoords, MIP_LEVEL, int2( 1, 0 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[1].z = texelFetchOffset(g_ViewspaceDepthSource1,baseCoords, MIP_LEVEL, int2( 0, 1 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[1].w = texelFetchOffset(g_ViewspaceDepthSource1,baseCoords, MIP_LEVEL, int2( 1, 1 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[2].x = texelFetchOffset(g_ViewspaceDepthSource2,baseCoords, MIP_LEVEL, int2( 0, 0 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[2].y = texelFetchOffset(g_ViewspaceDepthSource2,baseCoords, MIP_LEVEL, int2( 1, 0 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[2].z = texelFetchOffset(g_ViewspaceDepthSource2,baseCoords, MIP_LEVEL, int2( 0, 1 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[2].w = texelFetchOffset(g_ViewspaceDepthSource2,baseCoords, MIP_LEVEL, int2( 1, 1 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[3].x = texelFetchOffset(g_ViewspaceDepthSource3,baseCoords, MIP_LEVEL, int2( 0, 0 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[3].y = texelFetchOffset(g_ViewspaceDepthSource3,baseCoords, MIP_LEVEL, int2( 1, 0 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[3].z = texelFetchOffset(g_ViewspaceDepthSource3,baseCoords, MIP_LEVEL, int2( 0, 1 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;
    depthsArr[3].w = texelFetchOffset(g_ViewspaceDepthSource3,baseCoords, MIP_LEVEL, int2( 1, 1 )).x;// * g_ASSAOConsts.MaxViewspaceDepth;

    const uint2 SVPosui         = uint2( gl_FragCoord.xy );
    const uint pseudoRandomA    = (SVPosui.x ) + 2 * (SVPosui.y );

    float dummyUnused1;
    float dummyUnused2;
    float falloffCalcMulSq, falloffCalcAdd;
 

    for( int i = 0; i < 4; i++ )
    {
        float4 depths = depthsArr[i];

        float closest = min( min( depths.x, depths.y ), min( depths.z, depths.w ) );

        CalculateRadiusParameters( abs( closest ), float2(1.0), dummyUnused1, dummyUnused2, falloffCalcMulSq );

        float4 dists = depths - closest.xxxx;

        float4 weights = saturate( dot(dists, dists) * falloffCalcMulSq.xxxx + 1.0.xxxx );

        float smartAvg = dot( weights, depths ) / dot( weights, float4( 1.0, 1.0, 1.0, 1.0 ) );

        const uint pseudoRandomIndex = ( pseudoRandomA + i ) % 4;

        //depthsOutArr[i] = closest;
        //depthsOutArr[i] = depths[ pseudoRandomIndex ];
        depthsOutArr[i] = smartAvg;
    }

    Out_fColor0 = depthsOutArr[0];
    Out_fColor1 = depthsOutArr[1];
    Out_fColor2 = depthsOutArr[2];
    Out_fColor3 = depthsOutArr[3];		
}