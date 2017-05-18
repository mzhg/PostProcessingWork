
#include "PostProcessingLightScatteringCommon.frag"

in float4 UVAndScreenPos;

layout(location = 0) out float4 OutColor;

void main()
{
	uint2 ui2InterpolationSources = // g_tex2DInterpolationSource.Load( uint3(In.m_f4Pos.xy,0) );
									   texelFetch(g_tex2DInterpolationSource, int2(gl_FragCoord.xy), 0).xy;
    // Ray marching samples are interpolated from themselves, so it is easy to detect them:
    if( ui2InterpolationSources.x != ui2InterpolationSources.y )
          discard;
    
    OutColor = float4(ui2InterpolationSources, 0,0);
}