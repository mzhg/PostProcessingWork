
in vec4 m_f4UVAndScreenPos;
out vec2 Out_MinMax;

uniform float g_BufferWidthInv;
uniform float g_BufferHeightInv;

layout(binding = 0) uniform sampler2D s0;

#define float2 vec2

//-----------------------------------------------------------------------------
// Name: EdgeDetection
// Type: Pixel Shader
// Desc: Detect edges for final image bluring
//-----------------------------------------------------------------------------
void main()
{
    float isEdge = 0;
    
    float offsetX = g_BufferWidthInv * 0.5;
    float offsetY = g_BufferHeightInv * 0.5;
        
    float c0 = texture( s0, m_f4UVAndScreenPos.xy ).x;  // samplerLinearClamp

    float c1 = texture( s0, m_f4UVAndScreenPos.xy + float2( offsetX, 0) ).x;
    float c2 = texture( s0, m_f4UVAndScreenPos.xy + float2( 0,-offsetY) ).x;
    float c3 = texture( s0, m_f4UVAndScreenPos.xy + float2(-offsetX, 0) ).x;
    float c4 = texture( s0, m_f4UVAndScreenPos.xy + float2( 0, offsetY) ).x;

    float c5 = texture( s0, m_f4UVAndScreenPos.xy + float2( offsetX, offsetY) ).x;
    float c6 = texture( s0, m_f4UVAndScreenPos.xy + float2( offsetX,-offsetY) ).x;
    float c7 = texture( s0, m_f4UVAndScreenPos.xy + float2(-offsetX,-offsetY) ).x;
    float c8 = texture( s0, m_f4UVAndScreenPos.xy + float2(-offsetX, offsetY) ).x;

    float c9 =  texture( s0, m_f4UVAndScreenPos.xy + float2( -2.0 * offsetX, -2.0 * offsetY) ).x;
    float c10 = texture( s0, m_f4UVAndScreenPos.xy + float2( -1.0 * offsetX, -2.0 * offsetY) ).x;
    float c11 = texture( s0, m_f4UVAndScreenPos.xy + float2(  0.0 * offsetX, -2.0 * offsetY) ).x;
    float c12 = texture( s0, m_f4UVAndScreenPos.xy + float2(  1.0 * offsetX, -2.0 * offsetY) ).x;
    float c13 = texture( s0, m_f4UVAndScreenPos.xy + float2(  2.0 * offsetX, -2.0 * offsetY) ).x;

    float c14 = texture( s0, m_f4UVAndScreenPos.xy + float2( -2.0 * offsetX, 2.0 * offsetY) ).x;
    float c15 = texture( s0, m_f4UVAndScreenPos.xy + float2( -1.0 * offsetX, 2.0 * offsetY) ).x;
    float c16 = texture( s0, m_f4UVAndScreenPos.xy + float2(  0.0 * offsetX, 2.0 * offsetY) ).x;
    float c17 = texture( s0, m_f4UVAndScreenPos.xy + float2(  1.0 * offsetX, 2.0 * offsetY) ).x;
    float c18 = texture( s0, m_f4UVAndScreenPos.xy + float2(  2.0 * offsetX, 2.0 * offsetY) ).x;

    float c19 = texture( s0, m_f4UVAndScreenPos.xy + float2( -2.0 * offsetX, -1.0 * offsetY) ).x;
    float c20 = texture( s0, m_f4UVAndScreenPos.xy + float2(  2.0 * offsetX, -1.0 * offsetY) ).x;
    float c21 = texture( s0, m_f4UVAndScreenPos.xy + float2( -2.0 * offsetX,  0.0 * offsetY) ).x;
    float c22 = texture( s0, m_f4UVAndScreenPos.xy + float2(  2.0 * offsetX,  0.0 * offsetY) ).x;
    float c23 = texture( s0, m_f4UVAndScreenPos.xy + float2( -2.0 * offsetX,  1.0 * offsetY) ).x;
    float c24 = texture( s0, m_f4UVAndScreenPos.xy + float2(  2.0 * offsetX,  1.0 * offsetY) ).x;

    // Apply Sobel 5x5 edge detection filter
    float Gx = 1.0 * ( -c9 -c14 + c13 + c18 ) + 2.0 * ( -c19 -c23 - c10 - c15 + c12 + c17 + c20 + c24 ) + 3.0 * ( -c21 -c7 -c8 + c6 + c5 + c22 ) + 5.0 * ( -c3 + c1 );
    float Gy = 1.0 * ( -c14 -c18 + c9 + c13 ) + 2.0 * ( -c15 -c17 - c23 - c24 + c19 + c20 + c10 + c12 ) + 3.0 * ( -c16 -c8 -c5 + c6 + c7 + c11 ) + 5.0 * ( -c4 + c2 );
    float scale = 25.0; // Blur scale, can be depth dependent

    Out_MinMax = float2( Gx * scale, Gy * scale );
}