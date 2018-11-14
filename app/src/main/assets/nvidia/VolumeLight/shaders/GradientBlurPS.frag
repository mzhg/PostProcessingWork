
in vec4 m_f4UVAndScreenPos;
out float Out_Color;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define float2 vec2

void main()
{
    float2 gradient = texture( s0, m_f4UVAndScreenPos.xy ).xy;   // samplerLinearClamp
    float2 offset;

    offset.x = g_BufferWidthInv * gradient.y;
    offset.y = g_BufferHeightInv * gradient.x;
    
    float vColor = 0.0f;
    
    for( int iSample = -7; iSample < 8; iSample++ )
    {
        vColor += texture( s0, m_f4UVAndScreenPos.xy + offset * iSample ).x;
    }
    
    vColor *= ( 1.0 / 15.0 );
    Out_Color = vColor;
}