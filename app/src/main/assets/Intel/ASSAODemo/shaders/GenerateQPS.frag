#include "ASSAOCommon.frag"

// #define QUALITY_LEVEL 0,1,2,3
// #define ADPATIVE_BASE 0,1
layout(location = 0) out float2 Out_fColor; 

void main()
{
	float   outShadowTerm;
    float   outWeight;
    float4  outEdges;
    GenerateSSAOShadowsInternal( outShadowTerm, outEdges, outWeight, gl_FragCoord.xy/*, inUV*/, QUALITY_LEVEL, bool(ADPATIVE_BASE) );
    Out_fColor.x = outShadowTerm;
#if QUALITY_LEVEL == 0
    Out_fColor.y = PackEdges( float4( 1, 1, 1, 1 ) ); // no edges in low quality
#else
	#if ADPATIVE_BASE
		Out_fColor.y = outWeight / (float(SSAO_ADAPTIVE_TAP_BASE_COUNT) * 4.0); //0.0; //frac(outWeight / 6.0);// / (float)(SSAO_MAX_TAPS * 4.0);
	#else
		Out_fColor.y = PackEdges( outEdges );
	#endif
#endif
}