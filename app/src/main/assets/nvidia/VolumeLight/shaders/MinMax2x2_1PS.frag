
in vec4 m_f4UVAndScreenPos;
out vec2 Out_MinMax;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

// Build min max texture for 2x2 kernel from original map
void main()
{
    vec2 textureOffset;
	textureOffset.x = g_BufferWidthInv * 0.25;
	textureOffset.y = g_BufferHeightInv * 0.25;

	float depth1 = texture( s0, m_f4UVAndScreenPos.xy + vec2(textureOffset.x, textureOffset.y) ).x;  // samplerPointClamp
	float depth2 = texture( s0, m_f4UVAndScreenPos.xy + vec2(textureOffset.x, -textureOffset.y) ).x;
	float depth3 = texture( s0, m_f4UVAndScreenPos.xy + vec2(-textureOffset.x, textureOffset.y) ).x;
	float depth4 = texture( s0, m_f4UVAndScreenPos.xy + vec2(-textureOffset.x, -textureOffset.y) ).x;

	float minDepth = min( depth1, depth2 );
	minDepth = min( minDepth, depth3 );
	minDepth = min( minDepth, depth4 );

	float maxDepth = max( depth1, depth2 );
	maxDepth = max( maxDepth, depth3 );
	maxDepth = max( maxDepth, depth4 );

	minDepth -= 25.0;
	maxDepth += 25.0;

	Out_MinMax = vec2( minDepth, maxDepth );
}