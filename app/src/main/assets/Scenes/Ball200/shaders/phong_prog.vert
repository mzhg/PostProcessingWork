#include "../../../shader_libs/PostProcessingCommon.glsl"

#if ENABLE_IN_OUT_FEATURE
in vec4 gxl3d_Position;
in vec4 gxl3d_Normal;
in vec4 gxl3d_TexCoord0;

out vec4 Vertex_UV;
out vec4 Vertex_Normal;
out vec4 Vertex_LightDir;
out vec4 Vertex_EyeVec;
#else
attribute vec4 gxl3d_Position;
attribute vec4 gxl3d_Normal;
attribute vec4 gxl3d_TexCoord0;

varying vec4 Vertex_UV;
varying vec4 Vertex_Normal;
varying vec4 Vertex_LightDir;
varying vec4 Vertex_EyeVec;
#endif

uniform mat4 gxl3d_ModelViewProjectionMatrix; // Automatically passed by GLSL Hacker
uniform mat4 gxl3d_ModelViewMatrix; // Automatically passed by GLSL Hacker
uniform vec4 light_position;
uniform vec4 uv_tiling;

void main()
{
  gl_Position = gxl3d_ModelViewProjectionMatrix * gxl3d_Position;
  Vertex_UV = gxl3d_TexCoord0 * uv_tiling;
  Vertex_Normal = gxl3d_ModelViewMatrix  * gxl3d_Normal;
  vec4 view_vertex = gxl3d_ModelViewMatrix * gxl3d_Position;
  Vertex_LightDir = light_position - view_vertex;
  Vertex_EyeVec = -view_vertex;
}