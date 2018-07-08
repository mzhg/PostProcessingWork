#include "Voxelizer.glsl"

layout(location = 0) in vec3 In_Position;

void main()
{
    gl_Position = mul( float4(In_Position,1), WorldViewProjection );
}