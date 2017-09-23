#include "PostProcessingCMAA_Common.glsl"

layout(location = 0) out float4 OutColor;
layout(early_fragment_tests) in;

void main()
{
    uint packedEdges, shapeType;
    UnpackBlurAAInfo( textureLod(g_src0TextureFlt,  int2(gl_FragCoord.xy), 0).r, packedEdges, shapeType );
    float4 edges = float4(UnpackEdge( packedEdges ));

    bool showShapes = true;

    if( !showShapes )
    {
        float alpha = saturate( dot( edges, float4( 1, 1, 1, 1 ) ) * 255.5f );

        edges.rgb *= 0.8;
        edges.rgb += edges.aaa * float3( 15 / 255.0, 31 / 255.0, 63 / 255.0 );

        OutColor = float4( edges.rgb, alpha );
    }
    else
    {
        if( any( /*edges.xyzw > 0*/ greaterThan(edges, float4(0)) ) )
        {
            OutColor = c_edgeDebugColours[shapeType];
            OutColor.a = 0.8;
            if( shapeType == 0 )
                OutColor.a = 0.4;
        }
    }
}