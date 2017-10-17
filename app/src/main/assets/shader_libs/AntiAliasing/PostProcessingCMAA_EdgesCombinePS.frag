#include "PostProcessingCMAA_Common.glsl"

layout(location = 0) out uint4 outEdges;

void main()
{
//    outDepth = 0.0;

//    const int3 screenPosIBase = int3( ((int2)_screenPos) * 2, 0 );
    const int2 screenPosIBase = int2(gl_FragCoord.xy) * 2;

    uint packedEdgesArray[3][3];

//    uint4 sampA = (uint4)(g_src0TextureFlt.GatherRed( PointSampler, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 1, 0 ) ) * 255.0 - 127.5);
//        uint4 sampB = (uint4)(g_src0TextureFlt.GatherRed( PointSampler, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 0, 1 ) ) * 255.0 - 127.5);
//        uint  sampC = (uint)(g_src0TextureFlt.Load( screenPosIBase.xyz, int2( 1, 1 ) ) * 255.0 - 127.5);
    // use only if it has the 'prev frame' flag: do "sample * 255.0 - 127.5" -> if it has the last bit flag (128), it's going to stay above 0
    uint4 sampA = uint4(textureGatherOffset( g_src0TextureFlt, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 1, 0 ) ) * 255.0 - 127.5);   //PointSampler
    uint4 sampB = uint4(textureGatherOffset( g_src0TextureFlt, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 0, 1 ) ) * 255.0 - 127.5);   // PointSampler
    uint  sampC = uint(texelFetchOffset(g_src0TextureFlt, screenPosIBase.xy,0, int2( 1, 1 ) ).x * 255.0 - 127.5);
    packedEdgesArray[0][0] = 0;
    packedEdgesArray[1][0] = sampA.w;
    packedEdgesArray[2][0] = sampA.z;
    packedEdgesArray[1][1] = sampA.x;
    packedEdgesArray[2][1] = sampA.y;
    packedEdgesArray[0][1] = sampB.w;
    packedEdgesArray[0][2] = sampB.x;
    packedEdgesArray[1][2] = sampB.y;
    packedEdgesArray[2][2] = sampC;

    uint4 pixelsC = uint4( packedEdgesArray[1+0][1+0], packedEdgesArray[1+1][1+0], packedEdgesArray[1+0][1+1], packedEdgesArray[1+1][1+1] );
    uint4 pixelsL = uint4( packedEdgesArray[0+0][1+0], packedEdgesArray[0+1][1+0], packedEdgesArray[0+0][1+1], packedEdgesArray[0+1][1+1] );
    uint4 pixelsU = uint4( packedEdgesArray[1+0][0+0], packedEdgesArray[1+1][0+0], packedEdgesArray[1+0][0+1], packedEdgesArray[1+1][0+1] );

    uint4 outEdge4 = pixelsC | ((pixelsL & 0x01) << 2) | ((pixelsU & 0x02u) << 2);
    float4 outEdge4Flt = float4(outEdge4) / 255.0;

//    g_resultTextureSlot2[ screenPosIBase.xy + int2( 0, 0 ) ] = outEdge4Flt.x;
//    g_resultTextureSlot2[ screenPosIBase.xy + int2( 1, 0 ) ] = outEdge4Flt.y;
//    g_resultTextureSlot2[ screenPosIBase.xy + int2( 0, 1 ) ] = outEdge4Flt.z;
//    g_resultTextureSlot2[ screenPosIBase.xy + int2( 1, 1 ) ] = outEdge4Flt.w;
    imageStore(g_resultTextureSlot2, screenPosIBase.xy + int2( 0, 0 ), float4(outEdge4Flt.x,0,0,0));
    imageStore(g_resultTextureSlot2, screenPosIBase.xy + int2( 1, 0 ), float4(outEdge4Flt.y,0,0,0));
    imageStore(g_resultTextureSlot2, screenPosIBase.xy + int2( 0, 1 ), float4(outEdge4Flt.z,0,0,0));
    imageStore(g_resultTextureSlot2, screenPosIBase.xy + int2( 1, 1 ), float4(outEdge4Flt.w,0,0,0));

    int4 numberOfEdges4 = countbits( outEdge4 );

//    outDepth = any( numberOfEdges4 > 1 );
    gl_FragDepth = float(any(greaterThan(numberOfEdges4, int4(1))));

    // magic depth codepath
    //outDepth = dot( numberOfEdges4 > 1, float4( 1.0/2.0, 1.0/4.0, 1.0/8.0, 1.0/16.0 ) );
}