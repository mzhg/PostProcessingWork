#include "VolumeRenderer.glsl"

layout(location = 0) out vec4 OutColor;

void main()
{
    OutColor = texture(rayDataTex, float2(gl_FragCoord.x/RTWidth,gl_FragCoord.y/RTHeight));  //samPointClamp
}