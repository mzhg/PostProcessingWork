#include "PostProcessingCMAA_Common.glsl"

layout(location = 0) out float4 OutColor;
layout(early_fragment_tests) in;

bool IsInRect( float2 pt, float4 rect )
{
    return ( (pt.x >= rect.x) && (pt.x <= rect.z) && (pt.y >= rect.y) && (pt.y <= rect.w) );
}

void DistToClosestRectEdge( float2 pt, float4 rect, out float dist, out int edge )
{
    edge = 0;
    dist = 1e20;

    float distTmp;
    distTmp = abs( pt.x - rect.x );
    if( distTmp <= dist ) { dist = distTmp; edge = 2; }  // left

    distTmp = abs( pt.y - rect.y );
    if( distTmp <= dist ) { dist = distTmp; edge = 3; }  // top

    distTmp = abs( pt.x - rect.z );
    if( distTmp <= dist ) { dist = distTmp; edge = 0; }  // right

    distTmp = abs( pt.y - rect.w );
    if( distTmp <= dist ) { dist = distTmp; edge = 1; }  // bottom
}

void DistToClosestRectEdge( float2 pt, float4 rect, out float dist, out int edge, int ignoreEdge )
{
    edge = 0;
    dist = 1e20;

    float distTmp;
    if( ignoreEdge != 2 )
    {
        distTmp = abs( pt.x - rect.x );
        if( distTmp <= dist ) { dist = distTmp; edge = 2; }  // left
    }

    if( ignoreEdge != 3 )
    {
        distTmp = abs( pt.y - rect.y );
        if( distTmp <= dist ) { dist = distTmp; edge = 3; }  // top
    }

    if( ignoreEdge != 0 )
    {
        distTmp = abs( pt.x - rect.z );
        if( distTmp <= dist ) { dist = distTmp; edge = 0; }  // right
    }

    if( ignoreEdge != 1 )
    {
        distTmp = abs( pt.y - rect.w );
        if( distTmp <= dist ) { dist = distTmp; edge = 1; }  // bottom
    }
}

float2 RectToRect( float2 pt, float2 srcRCentre, float2 srcRSize, float2 dstRCentre, float2 dstRSize )
{
    pt -= srcRCentre;
    pt /= srcRSize;

    pt *= dstRSize;
    pt += dstRCentre;

    return pt;
}

void main()
{
    float2 screenPos = gl_FragCoord.xy;
    float4 srcRect = g_CMAA.DebugZoomTool;
    bool showEdges = false;
    if( srcRect.x > srcRect.z )
    {
        srcRect = -srcRect;
        showEdges = true;
    }

    if( IsInRect(screenPos.xy, srcRect ) )
    {
        // draw source box frame
        float dist; int edge;
        DistToClosestRectEdge( screenPos.xy, srcRect, dist, edge );

        OutColor = float4( 0.8, 1, 0.8, dist < 1.1 );
        return;
    }

    float zoomFactor = 10;

    const float2 screenSize = float2(g_CMAA.ScreenWidth, g_CMAA.ScreenHeight);
    const float2 screenCenter = float2(g_CMAA.ScreenWidth, g_CMAA.ScreenHeight) * 0.5;

    float2 srcRectSize = float2( srcRect.z - srcRect.x, srcRect.w - srcRect.y );
    float2 srcRectCenter = srcRect.xy + srcRectSize.xy * 0.5;

    float2 displayRectSize = srcRectSize * zoomFactor.xx;
    float2 displayRectCenter;
    displayRectCenter.x = (srcRectCenter.x > screenCenter.x)?(srcRectCenter.x - srcRectSize.x * 0.5 - displayRectSize.x * 0.5 - 350):(srcRectCenter.x + srcRectSize.x * 0.5 + displayRectSize.x * 0.5 + 350);

    //displayRectCenter.y = (srcRectCenter.y > screenCenter.y)?(srcRectCenter.y - srcRectSize.y * 0.5 - displayRectSize.y * 0.5 - 50):(srcRectCenter.y + srcRectSize.y * 0.5 + displayRectSize.y * 0.5 + 50);
    displayRectCenter.y = lerp( displayRectSize.y/2, screenSize.y - displayRectSize.y/2, srcRectCenter.y / screenSize.y );

    float4 displayRect = float4( displayRectCenter.xy - displayRectSize.xy * 0.5, displayRectCenter.xy + displayRectSize.xy * 0.5 );

    bool chessPattern = /*(((uint)screenPos.x + (uint)screenPos.y) % 2) == 0*/
            int(screenPos.x + screenPos.y)%2==0;

    if( IsInRect(screenPos.xy, displayRect ) )
    {
        {
            // draw destination box frame
            float dist; int edge;
            DistToClosestRectEdge( screenPos.xy, displayRect, dist, edge );

            if( dist < 1.1 )
                return float4( 1.0, 0.8, 0.8, dist < 1.1 );
        }

        float2 texCoord = RectToRect( screenPos.xy, displayRectCenter, displayRectSize, srcRectCenter, srcRectSize );

        float3 colour = texelFetch(g_screenTexture, texCoord, 0).rgb;

        if( showEdges )
        {
            float4 thisPixRect, thisPixRectBig4;
            thisPixRect.xy = RectToRect( screenPos.xy, displayRectCenter, displayRectSize, srcRectCenter, srcRectSize );
            thisPixRect.zw = RectToRect( screenPos.xy, displayRectCenter, displayRectSize, srcRectCenter, srcRectSize );
            thisPixRect.xy = RectToRect( floor( thisPixRect.xy ), srcRectCenter, srcRectSize, displayRectCenter, displayRectSize );
            thisPixRect.zw = RectToRect( ceil(  thisPixRect.zw ), srcRectCenter, srcRectSize, displayRectCenter, displayRectSize );

            float dist; int edge;
            DistToClosestRectEdge( screenPos.xy, thisPixRect, dist, edge );
            if( dist < 1.1 ) //&& chessPattern )
            {
                uint packedEdges, shapeType;
                UnpackBlurAAInfo(texelFetch(g_src0TextureFlt, texCoord.xy, 0).r, packedEdges, shapeType );
                float4 edges = float4(UnpackEdge( packedEdges ));

                float4 edgeColour = c_edgeDebugColours[shapeType];

                if( shapeType == 0 )
                    edgeColour = float4( lerp( colour.rgb, edgeColour.rgb, 0.8), 1 );

                //edgeColour = float4( 0, 1, 1, 1 );

                if( edges[edge] != 0.0 )
                {
                    OutColor= edgeColour;
                    return;
                }

                // fill in a small corner gap
                DistToClosestRectEdge( screenPos.xy, thisPixRect, dist, edge, edge );
                if( ( dist < 1.1 ) && (edges[edge] != 0.0) )
                {
                    OutColor = edgeColour;
                    return;
                }
            }
        }

        OutColor = float4( colour, 1 );
        return;
    }


    OutColor = float4( 0.0, 0.5, 0, 0.0 );
}