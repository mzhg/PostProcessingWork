#include "ASSAOCommon.frag"

layout(location = 0) out float Out_fColor0; 
layout(location = 1) out float Out_fColor1;

void main()
{
	int3 baseCoord = int3( int2(gl_FragCoord.xy) * 2, 0 );
//    float a = g_DepthSource.Load( baseCoord, int2( 0, 0 ) ).x;
//    float d = g_DepthSource.Load( baseCoord, int2( 1, 1 ) ).x;
    
    float a = texelFetch(g_DepthSource, baseCoord.xy, baseCoord.z).x;
	float b = texelFetchOffset(g_DepthSource, baseCoord.xy, baseCoord.z, int2( 1, 1 )).x;

    Out_fColor0 = ScreenSpaceToViewSpaceDepth( a );
    Out_fColor1 = ScreenSpaceToViewSpaceDepth( b );
}