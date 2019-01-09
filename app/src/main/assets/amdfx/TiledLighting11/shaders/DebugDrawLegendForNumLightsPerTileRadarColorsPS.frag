#include "DebugDraw.glsl"

out float4 Out_Color;
in vec2 m_TextureUV;

void main()
{
    uint nBandIdx = floor(16.999f*m_TextureUV.y);

    // black for no lights
    if( nBandIdx == 0 ) Out_Color = float4(0,0,0,1);
    // light purple for reaching the max
    else if( nBandIdx == 15 ) Out_Color = float4(0.847,0.745,0.921,1);
    // white for going over the max
    else if ( nBandIdx == 16 ) Out_Color = float4(1,1,1,1);
    // else use weather radar colors
    else
    {
        // nBandIdx should be in the range [1,14]
        uint nColorIndex = nBandIdx-1;
        Out_Color = kRadarColors[nColorIndex];
    }
}