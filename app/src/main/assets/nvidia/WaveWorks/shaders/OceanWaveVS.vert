#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Quadtree.glsl"

layout(location = 0) in float4 In_f4Position;

out VS_OUTPUT
{
    float4 worldspace_position;
}_output;

void main()
{
    _output.worldspace_position = float4(GFSDK_WaveWorks_GetUndisplacedVertexWorldPosition(In_f4Position),0.0);
}