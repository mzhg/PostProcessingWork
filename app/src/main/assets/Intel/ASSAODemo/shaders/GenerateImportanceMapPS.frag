#include "ASSAOCommon.frag"

layout(location = 0) out float Out_fColor; 
in vec4 m_f4UVAndScreenPos;

void main()
{
	uint2 basePos = uint2(gl_FragCoord.xy) * 2;

    float2 baseUV = (float2(basePos) + float2( 0.5, 0.5 ) ) * g_ASSAOConsts.HalfViewportPixelSize;
    float2 gatherUV = (float2(basePos) + float2( 1.0, 1.0 ) ) * g_ASSAOConsts.HalfViewportPixelSize;

    float avg = 0.0;
    float minV = 1.0;
    float maxV = 0.0;
    for( int i = 0; i < 4; i++ )
    {
        float4 vals = textureGather( g_FinalSSAO, float3( gatherUV, i ) ); // g_PointClampSampler

        // apply the same modifications that would have been applied in the main shader
        vals = g_ASSAOConsts.EffectShadowStrength * vals;

        vals = 1.0-vals;

        vals = pow( saturate( vals ), float4(g_ASSAOConsts.EffectShadowPow) );

        avg += dot( float4( vals.x, vals.y, vals.z, vals.w ), float4( 1.0 / 16.0, 1.0 / 16.0, 1.0 / 16.0, 1.0 / 16.0 ) );

        maxV = max( maxV, max( max( vals.x, vals.y ), max( vals.z, vals.w ) ) );
        minV = min( minV, min( min( vals.x, vals.y ), min( vals.z, vals.w ) ) );
    }

    float minMaxDiff = maxV - minV;

    //return pow( saturate( minMaxDiff * 1.2 + (1.0-avg) * 0.3 ), 0.8 );
    Out_fColor = pow( saturate( minMaxDiff * 2.0 ), 0.8 );
}