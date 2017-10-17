#include "PostProcessingCMAA_Common.glsl"

layout(location = 0) out uint4 outEdges;

float2 UnpackThresholds( uint val )
{
    return float2( val & 0x0Fu, val >> 4 ) / 15.0f;
}

uint PruneNonDominantEdges( float4 edges[3] )
{
    float4 maxE4    = float4( 0.0, 0.0, 0.0, 0.0 );

    float avg = 0.0;

//    [unroll]
    for( int i = 0; i < 3; i++ )
    {
        maxE4 = max( maxE4, edges[i] );

        avg = dot( edges[i], float4( 1, 1, 1, 1 ) / ( 3.0 * 4.0 ) );
    }

    float2 maxE2    = max( maxE4.xy, maxE4.zw );
    float maxE      = max( maxE2.x, maxE2.y );

    float threshold = avg * (1.0 - g_CMAA.NonDominantEdgeRemovalAmount) + maxE * (g_CMAA.NonDominantEdgeRemovalAmount);

//    threshold = 0.0001; // this disables non-dominant edge pruning!

    bool cx = edges[0].x >= threshold;
    bool cy = edges[0].y >= threshold;

    return PackEdge( uint4( cx, cy, 0, 0 ) );
}

void CollectEdges( int offX, int offY, out float4 edges[3], const uint packedVals[6][6] )
{
    float2 pixelP0P0 = UnpackThresholds( packedVals[offX][offY] );
    float2 pixelP1P0 = UnpackThresholds( packedVals[offX+1][offY] );
    float2 pixelP0P1 = UnpackThresholds( packedVals[offX][offY+1] );
    float2 pixelM1P0 = UnpackThresholds( packedVals[offX-1][offY] );
    float2 pixelP0M1 = UnpackThresholds( packedVals[offX][offY-1] );
    float2 pixelP1M1 = UnpackThresholds( packedVals[offX+1][offY-1] );
    float2 pixelM1P1 = UnpackThresholds( packedVals[offX-1][offY+1] );

    edges[ 0].x = pixelP0P0.x;
    edges[ 0].y = pixelP0P0.y;
    edges[ 0].z = pixelP1P0.x;
    edges[ 0].w = pixelP1P0.y;
    edges[ 1].x = pixelP0P1.x;
    edges[ 1].y = pixelP0P1.y;
    edges[ 1].z = pixelM1P0.x;
    edges[ 1].w = pixelM1P0.y;
    edges[ 2].x = pixelP0M1.x;
    edges[ 2].y = pixelP0M1.y;
    edges[ 2].z = pixelP1M1.y;
    edges[ 2].w = pixelM1P1.x;
}

layout(early_fragment_tests) in;

void main()
{
    int2 screenPosI = int2(gl_FragCoord.xy);

    // source : edge differences from previous pass
    uint packedVals[6][6];

    // center pixel (our output)
    uint4 packedQ4 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2(  0,   0 ) ).rgba;
    packedVals[2][2] = packedQ4.x;
    packedVals[3][2] = packedQ4.y;
    packedVals[2][3] = packedQ4.z;
    packedVals[3][3] = packedQ4.w;

    // unused
    // packedVals[0][0] = 0; //packedQ0.x;
    // packedVals[1][0] = 0; //packedQ0.y;
    // packedVals[0][1] = 0; //packedQ0.z;
    // packedVals[1][1] = 0; //packedQ0.w;

    // unused
    //packedVals[4][4] = 0; //packedQ8.x;
    //packedVals[5][4] = 0; //packedQ8.y;
    //packedVals[4][5] = 0; //packedQ8.z;
    //packedVals[5][5] = 0; //packedQ8.w;

    float4 edges[3];
    uint pe;

    if( packedVals[2][2]!=0 || packedVals[3][2]!=0 )
    {
        uint4 packedQ1 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2(  0,  -1 ) ).rgba;
        packedVals[2][0] = packedQ1.x;
        packedVals[3][0] = packedQ1.y;
        packedVals[2][1] = packedQ1.z;
        packedVals[3][1] = packedQ1.w;
    }

    if( packedVals[2][2]!=0 || packedVals[2][3]!=0 )
    {
        uint4 packedQ3 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2( -1,   0 ) ).rgba;
        packedVals[0][2] = packedQ3.x;
        packedVals[1][2] = packedQ3.y;
        packedVals[0][3] = packedQ3.z;
        packedVals[1][3] = packedQ3.w;
    }

    if( packedVals[2][2]!=0  )
    {
        CollectEdges( 2, 2, edges, packedVals );
        uint pe = PruneNonDominantEdges( edges );
        if( pe != 0 )
//            g_resultTexture[ int2( screenPosI.x*2+0, screenPosI.y*2+0 ) ] = (pe | 0x80) / 255.0;
            imageStore(g_resultTexture, int2( screenPosI.x*2+0, screenPosI.y*2+0 ), float4(pe | 0x80u) / 255.0);
    }

    if( packedVals[3][2]!=0  || packedVals[3][3]!=0  )
    {
        uint4 packedQ5 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2(  1,   0 ) ).rgba;
        packedVals[4][2] = packedQ5.x;
        packedVals[5][2] = packedQ5.y;
        packedVals[4][3] = packedQ5.z;
        packedVals[5][3] = packedQ5.w;
    }

    if( packedVals[3][2]!=0  )
    {
        uint4 packedQ2 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2(  1,  -1 ) ).rgba;
        packedVals[4][0] = packedQ2.x;
        packedVals[5][0] = packedQ2.y;
        packedVals[4][1] = packedQ2.z;
        packedVals[5][1] = packedQ2.w;

        CollectEdges( 3, 2, edges, packedVals );
        uint pe = PruneNonDominantEdges( edges );
        if( pe != 0 )
//            g_resultTexture[ int2( screenPosI.x*2+1, screenPosI.y*2+0 ) ] = (pe | 0x80u) / 255.0;
            imageStore(g_resultTexture, int2( screenPosI.x*2+1, screenPosI.y*2+0 ), float4(pe | 0x80u) / 255.0);
    }

    if( packedVals[2][3]!=0  || packedVals[3][3]!=0  )
    {
        uint4 packedQ7 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2(  0,   1 ) ).rgba;
        packedVals[2][4] = packedQ7.x;
        packedVals[3][4] = packedQ7.y;
        packedVals[2][5] = packedQ7.z;
        packedVals[3][5] = packedQ7.w;
    }

    if( packedVals[2][3]!=0  )
    {
        uint4 packedQ6 = texelFetchOffset(g_src0Texture4Uint, screenPosI.xy, 0, int2( -1,   1 ) ).rgba;
        packedVals[0][4] = packedQ6.x;
        packedVals[1][4] = packedQ6.y;
        packedVals[0][5] = packedQ6.z;
        packedVals[1][5] = packedQ6.w;

        CollectEdges( 2, 3, edges, packedVals );
        uint pe = PruneNonDominantEdges( edges );
        if( pe != 0 )
//            g_resultTexture[ int2( screenPosI.x*2+0, screenPosI.y*2+1 ) ] = (pe | 0x80u) / 255.0;
            imageStore(g_resultTexture, int2( screenPosI.x*2+0, screenPosI.y*2+1 ), float4(pe | 0x80u) / 255.0);
    }

    if( packedVals[3][3]!=0  )
    {
        CollectEdges( 3, 3, edges, packedVals );
        uint pe = PruneNonDominantEdges( edges );
        if( pe != 0 )
//            g_resultTexture[ int2( screenPosI.x*2+1, screenPosI.y*2+1 ) ] = (pe | 0x80u) / 255.0;
            imageStore(g_resultTexture, int2( screenPosI.x*2+1, screenPosI.y*2+1 ), float4(pe | 0x80u) / 255.0);
    }
}