in vec4 m_f4UVAndScreenPos;
out vec2 Out_MinMax;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define int2 ivec2

// Get min and max values for 3x3 sample grid for coarse tracing.
// Use many steps ("heavy" shader), but the texture is relatively small.
void main()
{
    vec2 depth1 = texture( s0, m_f4UVAndScreenPos.xy).xy;
    vec2 depth2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,1) ).xy;
    vec2 depth3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,1) ).xy;
    vec2 depth4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,0) ).xy;
    vec2 depth5 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,-1) ).xy;
    vec2 depth6 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,-1) ).xy;
    vec2 depth7 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,-1) ).xy;
    vec2 depth8 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,0) ).xy;
    vec2 depth9 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,1) ).xy;

    float minDepth = min( depth1.x, depth2.x );
    minDepth = min( minDepth, depth3.x );
    minDepth = min( minDepth, depth4.x );
    minDepth = min( minDepth, depth5.x );
    minDepth = min( minDepth, depth6.x );
    minDepth = min( minDepth, depth7.x );
    minDepth = min( minDepth, depth8.x );
    minDepth = min( minDepth, depth9.x );
    
    float maxDepth = max( depth1.y, depth2.y );
    maxDepth = max( maxDepth, depth3.y );
    maxDepth = max( maxDepth, depth4.y );
    maxDepth = max( maxDepth, depth5.y );
    maxDepth = max( maxDepth, depth6.y );
    maxDepth = max( maxDepth, depth7.y );
    maxDepth = max( maxDepth, depth8.y );
    maxDepth = max( maxDepth, depth9.y );

    Out_MinMax = vec2( minDepth, maxDepth );
}