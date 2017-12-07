#include "ASSAOCommon.frag"

layout(location = 0) out float Out_fColor; 
in vec4 m_f4UVAndScreenPos;

layout(r32ui, binding = 0) uniform uimage1D g_LoadCounterOutputUAV;  // read_write

void main()
{
	const float cSmoothenImportance = 1.0;
	uint2 pos = uint2(gl_FragCoord.xy);
	float2 inUV = m_f4UVAndScreenPos.xy;

    float centre = textureLod( g_ImportanceMap, inUV, 0.0 ).x;  // g_LinearClampSampler
    //return centre;

    float2 halfPixel = g_ASSAOConsts.QuarterResPixelSize * 0.5f;

    float4 vals;
    vals.x = textureLod( g_ImportanceMap, inUV + float2( -halfPixel.x, -halfPixel.y * 3 ), 0.0 ).x;
    vals.y = textureLod( g_ImportanceMap, inUV + float2( +halfPixel.x * 3, -halfPixel.y ), 0.0 ).x;
    vals.z = textureLod( g_ImportanceMap, inUV + float2( +halfPixel.x, +halfPixel.y * 3 ), 0.0 ).x;
    vals.w = textureLod( g_ImportanceMap, inUV + float2( -halfPixel.x * 3, +halfPixel.y ), 0.0 ).x;

    float avgVal = dot( vals, float4( 0.25, 0.25, 0.25, 0.25 ) );
    vals.xy = max( vals.xy, vals.zw );
    float maxVal = max( centre, max( vals.x, vals.y ) );

    float retVal = lerp( maxVal, avgVal, cSmoothenImportance );

    // sum the average; to avoid overflowing we assume max AO resolution is not bigger than 16384x16384; so quarter res (used here) will be 4096x4096, which leaves us with 8 bits per pixel 
    uint sum = uint(saturate(retVal) * 255.0 + 0.5);
    
    // save every 9th to avoid InterlockedAdd congestion - since we're blurring, this is good enough; compensated by multiplying LoadCounterAvgDiv by 9
    if( ((pos.x % 3) + (pos.y % 3)) == 0  )
        imageAtomicAdd( g_LoadCounterOutputUAV, 0, sum );

    Out_fColor = retVal;
}