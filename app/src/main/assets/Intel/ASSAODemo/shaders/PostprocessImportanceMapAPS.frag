#include "ASSAOCommon.frag"

layout(location = 0) out float Out_fColor; 
in vec4 m_f4UVAndScreenPos;

void main()
{
	const float cSmoothenImportance = 1.0;
	uint2 pos = uint2(gl_FragCoord.xy);
	float2 inUV      = m_f4UVAndScreenPos.xy;

    float centre = textureLod( g_ImportanceMap, inUV, 0.0 ).x;  // g_LinearClampSampler
    //return centre;

    float2 halfPixel = g_ASSAOConsts.QuarterResPixelSize * 0.5f;

    float4 vals;
    vals.x = textureLod( g_ImportanceMap, inUV + float2( -halfPixel.x * 3, -halfPixel.y ), 0.0 ).x;  // g_LinearClampSampler
    vals.y = textureLod( g_ImportanceMap, inUV + float2( +halfPixel.x, -halfPixel.y * 3 ), 0.0 ).x;
    vals.z = textureLod( g_ImportanceMap, inUV + float2( +halfPixel.x * 3, +halfPixel.y ), 0.0 ).x;
    vals.w = textureLod( g_ImportanceMap, inUV + float2( -halfPixel.x, +halfPixel.y * 3 ), 0.0 ).x;

    float avgVal = dot( vals, float4( 0.25, 0.25, 0.25, 0.25 ) );
    vals.xy = max( vals.xy, vals.zw );
    float maxVal = max( centre, max( vals.x, vals.y ) );

    Out_fColor = lerp( maxVal, avgVal, cSmoothenImportance );
}