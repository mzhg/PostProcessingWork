#include "../../../shader_libs/PostProcessingCommonPS.frag"

uniform vec4 color;
#if ENABLE_IN_OUT_FEATURE
in vec4 Vertex_C;
in vec4 Vertex_UV;
#else
varying vec4 Vertex_C;
varying vec4 Vertex_UV;
#endif

void main()
{
  Out_f4Color = Vertex_C * color;
}