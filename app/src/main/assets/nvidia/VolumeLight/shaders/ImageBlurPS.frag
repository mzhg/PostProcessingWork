
in vec4 m_f4UVAndScreenPos;
out float Out_Color;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define int2 ivec2

void main()
{
    float sample0 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1, -1) ).x;
    float sample1 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 0, -1) ).x;
    float sample2 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1, -1) ).x;
    
    float sample3 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1, 0) ).x;
    float sample4 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 0, 0) ).x;
    float sample5 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1, 0) ).x;

    float sample6 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2(-1, 1) ).x;
    float sample7 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 0, 1) ).x;
    float sample8 = textureOffset( s0, m_f4UVAndScreenPos.xy, int2( 1, 1) ).x;
    
    float vColor = sample0 + sample1 + sample2;
    vColor += sample3 + sample4 + sample5;
    vColor += sample6 + sample7 + sample8;
    
    vColor *= ( 1.0 / 9.0 );
    Out_Color = vColor;
}