#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Quadtree.glsl"
#include "../../../shader_libs/WaveWork/GFSDK_WaveWorks_Attributes.glsl"

layout(location = 0) in float4 In_f4Position;

uniform mat4 u_ViewProjMatrix;
out GFSDK_WAVEWORKS_VERTEX_OUTPUT VSOutput;

void main()
{
    VSOutput = GFSDK_WaveWorks_GetDisplacedVertex(In_f4Position);
    vec3 dwpos = VSOutput.pos_world;
    gl_Position = u_ViewProjMatrix*vec4(dwpos,1.f);
}