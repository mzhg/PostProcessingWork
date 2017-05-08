#include "../../../shader_libs/PostProcessingCommon.glsl"

#if ENABLE_IN_OUT_FEATURE
in vec4 gxl3d_Position;
in vec4 gxl3d_TexCoord0;
in vec4 gxl3d_Color;

out vec4 Vertex_C;
out vec4 Vertex_UV;
#else
attribute vec4 gxl3d_Position;
attribute vec4 gxl3d_TexCoord0;
attribute vec4 gxl3d_Color;

varying vec4 Vertex_C;
varying vec4 Vertex_UV;
#endif

uniform mat4 gxl3d_ModelViewProjectionMatrix;

void main()
{
  gl_Position = gxl3d_ModelViewProjectionMatrix * gxl3d_Position;
  Vertex_C = gxl3d_Color;
  Vertex_UV = gxl3d_TexCoord0;
}