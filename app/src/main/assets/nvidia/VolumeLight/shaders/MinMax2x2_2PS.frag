in vec4 m_f4UVAndScreenPos;
out vec2 Out_MinMax;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

// Build min max texture mip
void main()
{
    vec2 textureOffset;
    textureOffset.x = g_BufferWidthInv * 0.25;
    textureOffset.y = g_BufferHeightInv * 0.25;

    vec2 depth1 = texture( s0, m_f4UVAndScreenPos.xy + vec2(textureOffset.x, textureOffset.y) ).xy;
    vec2 depth2 = texture( s0, m_f4UVAndScreenPos.xy + vec2(textureOffset.x, -textureOffset.y) ).xy;
    vec2 depth3 = texture( s0, m_f4UVAndScreenPos.xy + vec2(-textureOffset.x, textureOffset.y) ).xy;
    vec2 depth4 = texture( s0, m_f4UVAndScreenPos.xy + vec2(-textureOffset.x, -textureOffset.y) ).xy;

    float minDepth = min( depth1.x, depth2.x );
    minDepth = min( minDepth, depth3.x );
    minDepth = min( minDepth, depth4.x );

    float maxDepth = max( depth1.y, depth2.y );
    maxDepth = max( maxDepth, depth3.y );
    maxDepth = max( maxDepth, depth4.y );

    Out_MinMax = vec2( minDepth, maxDepth );
}