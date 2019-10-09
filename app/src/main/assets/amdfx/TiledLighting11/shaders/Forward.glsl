#include "CommonHeader.glsl"
#include "LightingCommonHeader.glsl"


//-----------------------------------------------------------------------------------------
// Textures and Buffers
//-----------------------------------------------------------------------------------------
/*Texture2D              g_TxDiffuse     : register( t0 );
Texture2D              g_TxNormal      : register( t1 );*/

layout(binding = 0) uniform sampler2D  g_TxDiffuse     /*: register( t0 )*/;
layout(binding = 1) uniform sampler2D  g_TxNormal      /*: register( t1 )*/;

#include "Transparency.glsl"
