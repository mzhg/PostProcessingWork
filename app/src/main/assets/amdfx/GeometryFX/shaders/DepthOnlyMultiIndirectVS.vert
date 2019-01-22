#include "AMD_GeometryFX_Filtering.glsl"

layout(location = 0) in vec4 In_Position;

void main()
{
    gl_Position = mul (projection, mul (drawConstants [gl_InstanceID].worldView, pos));
}