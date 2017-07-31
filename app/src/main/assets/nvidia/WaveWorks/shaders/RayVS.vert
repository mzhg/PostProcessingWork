#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Quadtree.glsl"

layout(location = 0) in float4 In_f4Position;

void main()
{
    gl_Position = mul(float4(g_OriginPosition.xzy + In_f4Position.y*g_RayDirection.xzy,1.0), g_ModelViewProjectionMatrix);
}