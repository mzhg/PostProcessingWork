#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Quadtree.glsl"

layout(location = 0) in float4 In_f4Position;

uniform float4 g_ContactPosition;
uniform mat4 g_ModelViewProjectionMatrix;

void main()
{
    gl_Position = mul(float4(In_f4Position.xzy*0.5 + g_ContactPosition.xyz, 1.0), g_ModelViewProjectionMatrix);
}