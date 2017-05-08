#include "PostProcessingCommon.glsl"

#if ENABLE_VERTEX_ID
in vec3 a_Position;
#else
attribute vec3 a_Position;
#endif

uniform mat4 u_ViewProjMatrix;

void main() 
{
    gl_Position = u_ViewProjMatrix * vec4(a_Position, 1.0);
}
