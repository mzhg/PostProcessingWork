in vec4 m_f4UVAndScreenPos;
out float Out_Min;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define int2 ivec2

void main()
{
    float depth0 = texture( s0, m_f4UVAndScreenPos.xy ).y;
    float threshold = depth0 - 100.0;

    float depth1 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(1,0) ).y;
    float depth2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,-1) ).y;
    float depth3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,0) ).y;
    float depth4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(0,1) ).y;

    float depthMin = min( depth1, depth2 );
    depthMin = min( depthMin, depth3 );
    depthMin = min( depthMin, depth4 );

    depth1 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1, 1) ).y;
    depth2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1,-1) ).y;
    depth3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1,-1) ).y;
    depth4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1, 1) ).y;

    depthMin = min( depthMin, depth1 );
    depthMin = min( depthMin, depth2 );
    depthMin = min( depthMin, depth3 );
    depthMin = min( depthMin, depth4 );

    float propogate = float(threshold > depthMin);

    Out_Min = mix( depth0, depthMin, propogate );
}