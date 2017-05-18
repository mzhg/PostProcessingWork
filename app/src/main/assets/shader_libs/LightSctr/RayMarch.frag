
#include "CalculateInscattering.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float3 OutColor;

void main()
{
	int2 ui2SamplePosSliceInd = int2(gl_FragCoord.xy);
    float2 f2SampleLocation = // g_tex2DCoordinates.Load( uint3(ui2SamplePosSliceInd, 0) );
    							texelFetch(g_tex2DCoordinates, ui2SamplePosSliceInd, 0).xy;

 //   [branch]
    if( any(greaterThan(abs(f2SampleLocation), float2(1+1e-3))) )
    {
    	OutColor = float3(0);
        return;
    }

    OutColor = CalculateInscattering(f2SampleLocation, 
                                 false, // Do not apply phase function
                                 false, // Do not use min/max optimization
                                 0 // Ignored
                                 );
}