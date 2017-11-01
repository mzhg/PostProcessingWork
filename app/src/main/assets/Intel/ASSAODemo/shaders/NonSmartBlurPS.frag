#include "ASSAOCommon.frag"

layout(location = 0) out float2 Out_f2Color; 
in vec4 m_f4UVAndScreenPos;

// edge-ignorant blur in x and y directions, 9 pixels touched (for the lowest quality level 0)
void main()
{
	float2 halfPixel = g_ASSAOConsts.HalfViewportPixelSize * 0.5f;
	float2 inUV      = m_f4UVAndScreenPos.xy;
	
    float2 centre = textureLod( g_BlurInput, inUV, 0.0 ).xy;  // g_LinearClampSampler

    float4 vals;
    vals.x = textureLod( g_BlurInput, inUV + float2( -halfPixel.x * 3, -halfPixel.y ), 0.0 ).x; // g_LinearClampSampler
    vals.y = textureLod( g_BlurInput, inUV + float2( +halfPixel.x, -halfPixel.y * 3 ), 0.0 ).x;
    vals.z = textureLod( g_BlurInput, inUV + float2( -halfPixel.x, +halfPixel.y * 3 ), 0.0 ).x;
    vals.w = textureLod( g_BlurInput, inUV + float2( +halfPixel.x * 3, +halfPixel.y ), 0.0 ).x;

    Out_f2Color = float2(dot( vals, 0.2.xxxx ) + centre.x * 0.2, centre.y);
}