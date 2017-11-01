#include "ASSAOCommon.frag"

layout(location = 0) out float2 Out_f2Color; 

in vec4 m_f4UVAndScreenPos;

// edge-sensitive blur (wider kernel)
void main()
{
	Out_f2Color = SampleBlurredWide( gl_FragCoord, m_f4UVAndScreenPos.xy );
}