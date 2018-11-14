in vec4 m_f4UVAndScreenPos;
out float Out_Max;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define int2 ivec2

void main()
{
    float depth0 = texture( s0, m_f4UVAndScreenPos.xy).x;
	float threshold = depth0 + 100.0; 

	float depth1 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,0) ).x;
	float depth2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,-1) ).x;
	float depth3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,0) ).x;
	float depth4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,1) ).x;

	float depth5 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,1) ).x;
	float depth6 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,-1) ).x;
	float depth7 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,-1) ).x;
	float depth8 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,1) ).x;

	float depthMax = max( depth1, depth2 );
	depthMax = max( depthMax, depth3 );
	depthMax = max( depthMax, depth4 );
	depthMax = max( depthMax, depth5 );
	depthMax = max( depthMax, depth6 );
	depthMax = max( depthMax, depth7 );
	depthMax = max( depthMax, depth8 );

	float propogate = threshold < depthMax ? 1.0 : 0.0;
	propogate = clamp( propogate,0.0, 1.0 );
	
	Out_Max = mix( depth0, depthMax, propogate );
}