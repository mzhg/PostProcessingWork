#include "DebugDraw.glsl"

out float4 Out_Color;
in vec2 m_TextureUV;

//--------------------------------------------------------------------------------------
// This shader creates a procedural texture for a grayscale gradient, for use as
// a legend for the grayscale lights-per-tile visualization.
//--------------------------------------------------------------------------------------
void main()
{
    float fGradVal = m_TextureUV.y;
    return float4(fGradVal, fGradVal, fGradVal, 1.0f);
}