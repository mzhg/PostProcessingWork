#include "PostProcessingCMAA_Common.glsl"

layout(location = 0) out uint4 outEdges;
layout(early_fragment_tests) in;

void main()
{
    int _i;

    const int3 screenPosIBase = int3( gl_FragCoord.xy * 2, 0 );

    //float magicDepth = g_depthTextureFlt.Load( int3( (int2)_screenPos.xy, 0 ) );

    uint forFollowUpCount = 0;
    int4 forFollowUpCoords[4];

    uint packedEdgesArray[4][4];

    uint4 sampA = textureGatherOffset( g_src0TextureFlt, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 0, 0 ) ) * 255.5;  // PointSampler
    uint4 sampB = textureGatherOffset( g_src0TextureFlt, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 2, 0 ) ) * 255.5;
    uint4 sampC = textureGatherOffset( g_src0TextureFlt, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 0, 2 ) ) * 255.5;
    uint4 sampD = textureGatherOffset( g_src0TextureFlt, screenPosIBase.xy * g_CMAA.OneOverScreenSize, int2( 2, 2 ) ) * 255.5;
    packedEdgesArray[0][0] = sampA.w;
    packedEdgesArray[1][0] = sampA.z;
    packedEdgesArray[0][1] = sampA.x;
    packedEdgesArray[1][1] = sampA.y;
    packedEdgesArray[2][0] = sampB.w;
    packedEdgesArray[3][0] = sampB.z;
    packedEdgesArray[2][1] = sampB.x;
    packedEdgesArray[3][1] = sampB.y;
    packedEdgesArray[0][2] = sampC.w;
    packedEdgesArray[1][2] = sampC.z;
    packedEdgesArray[0][3] = sampC.x;
    packedEdgesArray[1][3] = sampC.y;
    packedEdgesArray[2][2] = sampD.w;
    packedEdgesArray[3][2] = sampD.z;
    packedEdgesArray[2][3] = sampD.x;
    packedEdgesArray[3][3] = sampD.y;

//    [unroll]
    for( _i = 0; _i < 4; _i++ )
    {
        int _x = _i%2;
        int _y = _i/2;

        //magicDepth *= 2.0;
        //if( magicDepth < 0.99 )
        //    continue;
        //magicDepth -= 1.0;

        const int3 screenPosI = screenPosIBase + int3( _x, _y, 0 );

        //#ifdef IN_GAMMA_CORRECT_MODE
        //        g_resultTextureFlt4Slot1[ screenPosI.xy ] = float4( D3DX_FLOAT3_to_SRGB( float3( 1, 0, 0 ) ), 1 );
        //#else
        //        g_resultTextureFlt4Slot1[ screenPosI.xy ] = float4( float3( 0, 1, 0 ), 1 );
        //#endif
        //        continue;


        const uint packedEdgesC = packedEdgesArray[1+_x][1+_y]; // packedEdgesC4[_i]; // g_src0Texture.Load( screenPosI ).r;

        // int numberOfEdges = countbits( packedEdgesC );

        const uint4 edges       = UnpackEdge( packedEdgesC );
        const float4 edgesFlt   = float4(edges);

        float numberOfEdges = dot( edgesFlt, float4( 1, 1, 1, 1 ) );

        if( numberOfEdges < 2 )
            continue;

        const float fromRight   = edgesFlt.r;
        const float fromBelow   = edgesFlt.g;
        const float fromLeft    = edgesFlt.b;
        const float fromAbove   = edgesFlt.a;

        float4 xFroms = float4( fromBelow, fromAbove, fromRight, fromLeft );

        //this is number of edges - we already have this
        //float fSum = dot( xFroms, float4( 1, 1, 1, 1 ) );

        float blurCoeff = 0.0;

        // These are additional blurs that complement the main line-based blurring;
        // Unlike line-based, these do not necessarily preserve the total amount of screen colour as they will
        // take neighbouring pixel colours and apply them to the one currently processed.

#ifndef DEBUG_DISABLE_SIMPLE_SHAPES // enable/disable simple shapes
        // 1.) L-like shape.
        // For this shape, the total amount of screen colour will be preserved when this is a part
        // of a (zigzag) diagonal line as the corners from the other side will do the same and
        // take some of the current pixel's colour in return.
        // However, in the case when this is an actual corner, the pixel's colour will be partially
        // overwritten by it's 2 neighbours.
        // if( numberOfEdges > 1 )
        {

            // with value of 0.15, the pixel will retain approx 77% of its colour and the remaining 23% will
            // come from its 2 neighbours (which are likely to be blurred too in the opposite direction)
            blurCoeff = 0.08;

            // Only do blending if it's L shape - if we're between two parallel edges, don't do anything
            blurCoeff *= (1 - fromBelow * fromAbove) * (1 - fromRight * fromLeft);
        }

        // 2.) U-like shape (surrounded with edges from 3 sides)
//        [flatten]
        if( numberOfEdges > 2 )
        {
            // with value of 0.13, the pixel will retain approx 72% of its colour and the remaining 28% will
            // be picked from its 3 neighbours (which are unlikely to be blurred too but could be)
            blurCoeff = 0.11;
        }

        // 3.) Completely surrounded with edges from all 4 sides
//        [flatten]
        if( numberOfEdges > 3 )
        {
            // with value of 0.07, the pixel will retain 78% of its colour and the remaining 22% will
            // come from its 4 neighbours (which are unlikely to be blurred)
            blurCoeff = 0.05;
        }

        if( blurCoeff == 0 )
        {
            // this avoids Z search below as well but that's ok because a Z shape will also always have
            // some blurCoeff
            continue;
        }
#endif // DEBUG_DISABLE_SIMPLE_SHAPES

        float4 blurMap = xFroms * blurCoeff;

        float4 pixelC = texelFetch(g_screenTexture, screenPosI.xy,screenPosI.z /*int2( 0,  0 )*/ ).rgba;

        const float centerWeight = 1.0;
        const float fromBelowWeight = blurMap.x; // (1 / (1 - blurMap.x)) - 1; // this would be the proper math for blending if we were handling
        const float fromAboveWeight = blurMap.y; // (1 / (1 - blurMap.y)) - 1; // lines (Zs) and mini kernel smoothing here, but since we're doing
        const float fromRightWeight = blurMap.z; // (1 / (1 - blurMap.z)) - 1; // lines separately, no need to complicate, just tweak the settings.
        const float fromLeftWeight  = blurMap.w; // (1 / (1 - blurMap.w)) - 1;

        const float fourWeightSum   = dot( blurMap, float4( 1, 1, 1, 1 ) );
        const float allWeightSum    = centerWeight + fourWeightSum;

        float4 _output = float4( 0, 0, 0, 0 );
//        [flatten]
        if( fromLeftWeight > 0.0 )
        {
            float3 pixelL = texelFetchOffset(g_screenTexture, screenPosI.xy,screenPosI.z, int2( -1,  0 ) ).rgb;
            _output.rgb += fromLeftWeight * pixelL;
        }
//        [flatten]
        if( fromAboveWeight > 0.0 )
        {
            float3 pixelT = texelFetchOffset(g_screenTexture, screenPosI.xy,screenPosI.z, int2(  0, -1 ) ).rgb;
            _output.rgb += fromAboveWeight * pixelT;
        }
//        [flatten]
        if( fromRightWeight > 0.0 )
        {
            float3 pixelR = texelFetchOffset(g_screenTexture, screenPosI.xy,screenPosI.z, int2(  1,  0 ) ).rgb;
            _output.rgb += fromRightWeight * pixelR;
        }
//        [flatten]
        if( fromBelowWeight > 0.0 )
        {
            float3 pixelB = texelFetchOffset(g_screenTexture, screenPosI.xy,screenPosI.z, int2(  0,  1 ) ).rgb;
            _output.rgb += fromBelowWeight * pixelB;
        }

        _output /= fourWeightSum + 0.0001;
        _output.a = 1 - centerWeight / allWeightSum;

        _output.rgb = lerp( pixelC.rgb, _output.rgb, _output.a ).rgb;
#ifdef IN_GAMMA_CORRECT_MODE
        _output.rgb = D3DX_FLOAT3_to_SRGB( _output.rgb );
#endif

#ifdef DEBUG_OUTPUT_AAINFO
    #ifndef DEBUG_DISABLE_SIMPLE_SHAPES // enable/disable simple shapes
//        g_resultTextureSlot2[ screenPosI.xy ] = PackBlurAAInfo( screenPosI.xy, numberOfEdges );
          imageStore(g_resultTextureSlot2, screenPosI.xy, float4(PackBlurAAInfo( screenPosI.xy, numberOfEdges )));
    #endif
#endif

//        g_resultTextureFlt4Slot1[ screenPosI.xy ] = float4( _output.rgb, pixelC.a );
        imageStore(g_resultTextureFlt4Slot1, screenPosI.xy, float4( _output.rgb, pixelC.a ));

        if( numberOfEdges == 2 )
        {

            uint packedEdgesL    = packedEdgesArray[0+_x][1+_y];
            uint packedEdgesT    = packedEdgesArray[1+_x][0+_y];
            uint packedEdgesR    = packedEdgesArray[2+_x][1+_y];
            uint packedEdgesB    = packedEdgesArray[1+_x][2+_y];

            //bool isNotHorizontal =
            bool isHorizontalA = ( ( packedEdgesC ) == (0x01 | 0x02) ) && ( (packedEdgesR & (0x01 | 0x08) ) == (0x08) );
            bool isHorizontalB = ( ( packedEdgesC ) == (0x01 | 0x08) ) && ( (packedEdgesR & (0x01 | 0x02) ) == (0x02) );

            bool isHCandidate = isHorizontalA || isHorizontalB;

            bool isVerticalA = ( ( packedEdgesC ) == (0x08 | 0x01) ) && ( (packedEdgesT & (0x08 | 0x04) ) == (0x04) );
            bool isVerticalB = ( ( packedEdgesC ) == (0x08 | 0x04) ) && ( (packedEdgesT & (0x08 | 0x01) ) == (0x01) );
            bool isVCandidate = isVerticalA || isVerticalB;

            bool isCandidate = isHCandidate || isVCandidate;

            if( !isCandidate )
                continue;

            bool horizontal = isHCandidate;

            // what if both are candidates? do additional pruning (still not 100% but gets rid of worst case errors)
            if( isHCandidate && isVCandidate )
                horizontal = ( isHorizontalA && ( ( packedEdgesL & 0x02 ) == 0x02 ) ) || ( isHorizontalB && ( ( packedEdgesL & 0x08 ) == 0x08 ) );

            int2 offsetC;
            uint packedEdgesM1P0;
            uint packedEdgesP1P0;
            if( horizontal )
            {
                packedEdgesM1P0 = packedEdgesL;
                packedEdgesP1P0 = packedEdgesR;
                offsetC = int2(  2,  0 );
            }
            else
            {
                packedEdgesM1P0 = packedEdgesB;
                packedEdgesP1P0 = packedEdgesT;
                offsetC = int2(  0, -2 );
            }

            //uint4 edges        = UnpackEdge( packedEdgesC );
            uint4 edgesM1P0    = UnpackEdge( packedEdgesM1P0 );
            uint4 edgesP1P0    = UnpackEdge( packedEdgesP1P0 );
            uint4 edgesP2P0    = UnpackEdge( texelFetch(g_src0TextureFlt, screenPosI.xy + offsetC, 0).r * 255.5 );

            uint4 arg0;
            uint4 arg1;
            uint4 arg2;
            uint4 arg3;
            bool arg4;

            if( horizontal )
            {
                arg0 = edges;
                arg1 = edgesM1P0;
                arg2 = edgesP1P0;
                arg3 = edgesP2P0;
                arg4 = true;
            }
            else
            {
                // Reuse the same code for vertical (used for horizontal above), but rotate input data 90?counter-clockwise, so that:
                // left     becomes     bottom
                // top      becomes     left
                // right    becomes     top
                // bottom   becomes     right

                // we also have to rotate edges, thus .argb
                arg0 = edges.argb;
                arg1 = edgesM1P0.argb;
                arg2 = edgesP1P0.argb;
                arg3 = edgesP2P0.argb;
                arg4 = false;
            }

            //DetectZsHorizontal( screenPosI.xy, arg0, arg1, arg2, arg3, arg4 );
            {
                const int2 screenPos = screenPosI.xy;
                const uint4 _edges = arg0;
                const uint4 _edgesM1P0 = arg1;
                const uint4 _edgesP1P0 = arg2;
                const uint4 _edgesP2P0 = arg3;
                bool horizontal = arg4;
                // Inverted Z case:
                //   __
                //  X|
                //
                bool isInvertedZ = false;
                bool isNormalZ = false;
                {
#ifndef SETTINGS_ALLOW_SHORT_Zs
                    uint isZShape		= _edges.r * _edges.g * _edgesM1P0.g * _edgesP1P0.a * _edgesP2P0.a * (1-_edges.b) * (1-_edgesP1P0.r) * (1-_edges.a) * (1-_edgesP1P0.g);   // (1-_edges.a) constraint can be removed; it was added for some rare cases
#else
                    uint isZShape		= _edges.r * _edges.g *                _edgesP1P0.a *                (1-_edges.b) * (1-_edgesP1P0.r) * (1-_edges.a) * (1-_edgesP1P0.g);   // (1-_edges.a) constraint can be removed; it was added for some rare cases
                    isZShape           *= ( _edgesM1P0.g + _edgesP2P0.a ); // and at least one of these need to be there
#endif

                    if( isZShape > 0.0 )
                    {
                        isInvertedZ = true;
                    }
                }

                // Normal Z case:
                // __
                //  X|
                //
                {
#ifndef SETTINGS_ALLOW_SHORT_Zs
                    uint isZShape   = _edges.r * _edges.a * _edgesM1P0.a * _edgesP1P0.g * _edgesP2P0.g * (1-_edges.b) * (1-_edgesP1P0.r) * (1-_edges.g) * (1-_edgesP1P0.a);   // (1-_edges.g) constraint can be removed; it was added for some rare cases
#else
                    uint isZShape     = _edges.r * _edges.a *                _edgesP1P0.g                * (1-_edges.b) * (1-_edgesP1P0.r) * (1-_edges.g) * (1-_edgesP1P0.a);   // (1-_edges.g) constraint can be removed; it was added for some rare cases
                    isZShape         *= ( _edgesM1P0.a + _edgesP2P0.g ); // and at least one of these need to be there
#endif

                    if( isZShape > 0.0 )
                    {
                        isNormalZ = true;
                    }
                }
                bool isZ = isInvertedZ || isNormalZ;
//                [branch]
                if( isZ )
                {
                    forFollowUpCoords[forFollowUpCount++] = int4( screenPosI.xy, horizontal, isInvertedZ );
                }
            }
        }
    }

    // This code below is the only potential bug with this algorithm : it HAS to be executed after the simple shapes above. It used to be executed as a separate compute
    // shader (by storing the packed 'forFollowUpCoords' in an append buffer and consuming it later) but the whole thing (append/consume buffers, using CS) appears to
    // be too inefficient on most hardware.
    // However, it seems to execute fairly efficiently here and without any issues, although there is no 100% guarantee that this code below will execute across all pixels
    // (it has a c_maxLineLength wide kernel) after other shaders processing same pixels have done solving simple shapes. It appears to work regardless, across all
    // hardware; pixels with 1-edge or two opposing edges are ignored by simple shapes anyway and other shapes stop the long line algorithm from executing; the only danger
    // appears to be simple shape L's colliding with Z shapes from neighbouring pixels but I couldn't reproduce any problems on any hardware.
//    [loop]
    for( _i = 0; _i < forFollowUpCount; _i++ )
    {
        int4 data = forFollowUpCoords[_i];
        ProcessDetectedZ( data.xy, data.z, data.w );
    }
}