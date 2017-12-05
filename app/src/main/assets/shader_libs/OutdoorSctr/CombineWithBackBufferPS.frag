#include "CloudsCommon.glsl"

layout(binding = 0) uniform sampler2D g_tex2DColorBuffer;

layout(location = 0) out float4 Out_f4Color;
// This shader combines cloud buffers with the back buffer
void main()
{
    int2 i2Pos = int2(gl_FragCoord);
    vec3 f3BackgroundColor = texelFetch( g_tex2DColorBuffer, i2Pos,0 ).rgb;
#if DEBUG_STATIC_SCENE
//    int2 size =textureSize(g_tex2DColorBuffer, 0);
//    i2Pos.y = size.y - i2Pos.y;
#endif
    vec3 f3CloudColor = texelFetch(g_tex2DScrSpaceCloudColor,  i2Pos,0 ).rgb;
    float fTransparency = texelFetch(g_tex2DScrSpaceCloudTransparency, i2Pos,0 ).r;
    Out_f4Color = float4(f3BackgroundColor * fTransparency + f3CloudColor, 1);
//    Out_f4Color = float4(f3BackgroundColor, 1);
}