#include "Forward.glsl"

in vec2 TextureUV;
out float4 Out_Color;

//--------------------------------------------------------------------------------------
// This shader does alpha testing.
//--------------------------------------------------------------------------------------
void main()
{
    float4 DiffuseTex = texture( g_TxDiffuse, TextureUV );
    float fAlpha = DiffuseTex.a;
    if( fAlpha < g_fAlphaTest ) discard;
    Out_Color = DiffuseTex;
}