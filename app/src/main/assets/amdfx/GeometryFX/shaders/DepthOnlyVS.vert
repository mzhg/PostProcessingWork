#include "AMD_GeometryFX_Filtering.glsl"

layout(location = 0) in vec4 In_Position;

void main()
{
    gl_Position = mul (projection, mul (worldView, In_Position));
}