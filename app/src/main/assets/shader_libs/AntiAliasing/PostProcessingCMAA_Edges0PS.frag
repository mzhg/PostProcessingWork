#include "PostProcessingCMAA_Common.glsl"

layout(location = 0) out uint4 outEdges;

void main()
{
    int3 screenPosI = int3( gl_FragCoord.xy, 0 ) * int3( 2, 2, 0 );

    // .rgb contains colour, .a contains flag whether to output it to working colour texture
    float4 pixel00   = float4( texelFetch(g_screenTexture, screenPosI.xy, screenPosI.z ).rgba);
    float4 pixel10   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2(  1, 0 ) ).rgba );
    float4 pixel20   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2(  2, 0 ) ).rgba );
    float4 pixel01   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2( 0,  1 ) ).rgba );
    float4 pixel11   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2( 1,  1 ) ).rgba );
    float4 pixel21   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2( 2,  1 ) ).rgba );
    float4 pixel02   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2( 0,  2 ) ).rgba );
    float4 pixel12   = float4( texelFetchOffset(g_screenTexture, screenPosI.xy, screenPosI.z, uint2( 1,  2 ) ).rgba );

    float storeFlagPixel00 = 0;
    float storeFlagPixel10 = 0;
    float storeFlagPixel20 = 0;
    float storeFlagPixel01 = 0;
    float storeFlagPixel11 = 0;
    float storeFlagPixel21 = 0;
    float storeFlagPixel02 = 0;
    float storeFlagPixel12 = 0;

    float2 et;

    {
        et.x = EdgeDetectColorCalcDiff( pixel00.rgb, pixel10.rgb );
        et.y = EdgeDetectColorCalcDiff( pixel00.rgb, pixel01.rgb );
        et = saturate( et - g_CMAA.ColorThreshold.xx );
        int2 eti = int2( et * 15.0 + 0.99 );
        outEdges.x = eti.x | (eti.y << 4);

        storeFlagPixel00 += et.x;
        storeFlagPixel00 += et.y;
        storeFlagPixel10 += et.x;
        storeFlagPixel01 += et.y;
    }

    {
        et.x = EdgeDetectColorCalcDiff( pixel10.rgb, pixel20.rgb );
        et.y = EdgeDetectColorCalcDiff( pixel10.rgb, pixel11.rgb );
        et = saturate( et - g_CMAA.ColorThreshold.xx );
        int2 eti = int2( et * 15.0 + 0.99 );
        outEdges.y = eti.x | (eti.y << 4);

        storeFlagPixel10 += et.x;
        storeFlagPixel10 += et.y;
        storeFlagPixel20 += et.x;
        storeFlagPixel11 += et.y;
    }

    {
        et.x = EdgeDetectColorCalcDiff( pixel01.rgb, pixel11.rgb );
        et.y = EdgeDetectColorCalcDiff( pixel01.rgb, pixel02.rgb );
        et = saturate( et - g_CMAA.ColorThreshold.xx );
        int2 eti = int2( et * 15.0 + 0.99 );
        outEdges.z = eti.x | (eti.y << 4);

        storeFlagPixel01 += et.x;
        storeFlagPixel01 += et.y;
        storeFlagPixel11 += et.x;
        storeFlagPixel02 += et.y;
    }

    {
        et.x = EdgeDetectColorCalcDiff( pixel11.rgb, pixel21.rgb );
        et.y = EdgeDetectColorCalcDiff( pixel11.rgb, pixel12.rgb );
        et = saturate( et - g_CMAA.ColorThreshold.xx );
        int2 eti = int2( et * 15.0 + 0.99 );
        outEdges.w = eti.x | (eti.y << 4);

        storeFlagPixel11 += et.x;
        storeFlagPixel11 += et.y;
        storeFlagPixel21 += et.x;
        storeFlagPixel12 += et.y;
    }

    gl_FragDepth = (any(outEdges) != 0)?(1.0):(0.0);

    if( outDepth != 0 )
    {
        if( storeFlagPixel00 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 0, 0 ) ] = pixel00.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 0, 0 ), pixel00.rgba);
        if( storeFlagPixel10 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 1, 0 ) ] = pixel10.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 1, 0 ), pixel10.rgba);
        if( storeFlagPixel20 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 2, 0 ) ] = pixel20.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 2, 0 ), pixel20.rgba);
        if( storeFlagPixel01 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 0, 1 ) ] = pixel01.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 0, 1 ), pixel01.rgba);
        if( storeFlagPixel02 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 0, 2 ) ] = pixel02.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 0, 2 ), pixel02.rgba);
        if( storeFlagPixel11 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 1, 1 ) ] = pixel11.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 1, 1 ), pixel11.rgba);
        if( storeFlagPixel21 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 2, 1 ) ] = pixel21.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 2, 1 ), pixel21.rgba);
        if( storeFlagPixel12 != 0 )
//            g_resultTextureFlt4Slot1[ screenPosI.xy + int2( 1, 2 ) ] = pixel12.rgba;
            imageStore(g_resultTextureFlt4Slot1, screenPosI.xy + int2( 1, 2 ), pixel12.rgba);
    }
}