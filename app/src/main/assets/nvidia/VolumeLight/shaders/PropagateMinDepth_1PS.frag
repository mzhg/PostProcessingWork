in vec4 m_f4UVAndScreenPos;
out float Out_MinMax;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define int2 ivec2

void main()
{
    float depth0 = texture( s0, m_f4UVAndScreenPos.xy).x;
    float threshold = depth0 - 100.0;

    float depth1 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,0) ).x;
    float depth2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,-1) ).x;
    float depth3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,0) ).x;
    float depth4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,1) ).x;

    float depthMin = min( depth1, depth2 );
    depthMin = min( depthMin, depth3 );
    depthMin = min( depthMin, depth4 );

    depth1 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1, 1) ).x;
    depth2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1,-1) ).x;
    depth3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,-1) ).x;
    depth4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1, 1) ).x;

    depthMin = min( depthMin, depth1 );
    depthMin = min( depthMin, depth2 );
    depthMin = min( depthMin, depth3 );
    depthMin = min( depthMin, depth4 );
    
    float propogate = threshold > depthMin ? 1.0 : 0.0;
    
    Out_MinMax = mix( depth0, depthMin, propogate );
}