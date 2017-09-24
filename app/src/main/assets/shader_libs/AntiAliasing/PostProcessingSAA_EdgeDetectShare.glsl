#include "../PostProcessingHLSLCompatiable.glsl"
#ifndef EDGEDETECTSHARE_HLSL_INCLUDED
#define EDGEDETECTSHARE_HLSL_INCLUDED

struct EdgeDetectConstants
{
   float4   LumWeights;             // .rgb - luminance weight for each colour channel; .w unused for now (maybe will be used for gamma correction before edge detect)

   float    ColorThresholdSimple;   // one threshold only, when not using depth/normal info
   float    ColorThresholdMin;      // if above, edge if depth/normal is edge too
   float    ColorThresholdMax;      // if above, always edge
   float    DepthThreshold;         // if between ColorThresholdMin/ColorThresholdMax
};

//cbuffer EdgeDetectGlobals : register(b7)
layout(binding = 1) uniform EdgeDetectGlobals
{
   EdgeDetectConstants g_EdgeDetectGlobals;
};

bool EdgeDetectColor( float4 colorA, float4 colorB )
{
// CONSIDER THIS:
//   // Weighted Euclidean distance
//   // (Copyright ?2010, Thiadmer Riemersma, ITB CompuPhase, see http://www.compuphase.com/cmetric.htm for details)
//   float rmean = ( colour1.r + colour2.r ) / 2.0;
//   float3 delta = colour1 - colour2;
//   return sqrt( ( (2.0+rmean)*delta.r*delta.r ) + 4*delta.g*delta.g + ( (3.0-rmean)*delta.b*delta.b ) );

	float3 LumWeights   = g_EdgeDetectGlobals.LumWeights.rgb;

	float diff = abs(
		        (colorA.r - colorB.r) * LumWeights.r +
				(colorA.g - colorB.g) * LumWeights.g +
				(colorA.b - colorB.b) * LumWeights.b
				);

    return diff > g_EdgeDetectGlobals.ColorThresholdSimple;
}


#endif // EDGEDETECTSHARE_HLSL_INCLUDED