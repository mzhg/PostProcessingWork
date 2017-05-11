#include "../../../shader_libs/PostProcessingCommon.glsl"
#ifndef GL_ES
//#extension GL_ARB_draw_instanced: enable
#endif

#if ENABLE_IN_OUT_FEATURE
in vec4 gxl3d_Position;
in vec4 gxl3d_Instance_Position;
in vec4 gxl3d_Instance_Rotation;

out vec4 Vertex_C;
#else
attribute vec4 gxl3d_Position;
attribute vec4 gxl3d_Instance_Position;
attribute vec4 gxl3d_Instance_Rotation;

varying vec4 Vertex_C;
#endif

uniform mat4 gxl3d_ProjectionMatrix;
uniform mat4 gxl3d_ViewMatrix;

#define PI_OVER_180 0.01745329251994329576923690768489

mat4 makeInstanceTransform(vec4 pos, vec4 rot)
{
  float pitch=rot.x, yaw=rot.y, roll=rot.z;
	float cosX = cos(pitch*PI_OVER_180);
	float sinX = sin(pitch*PI_OVER_180);
	float cosY = cos(yaw*PI_OVER_180);
	float sinY = sin(yaw*PI_OVER_180);
	float cosZ = cos(roll*PI_OVER_180);
	float sinZ = sin(roll*PI_OVER_180);
  
  mat4 result;
  
  result[0][0]  = cosY * cosZ + sinX * sinY * sinZ;
	result[0][1]  = -cosX * sinZ;
	result[0][2]  = sinX * cosY * sinZ - sinY * cosZ;
	result[0][3]  = 0.0;

	result[1][0]  = cosY * sinZ - sinX * sinY * cosZ;
	result[1][1]  = cosX * cosZ;
	result[1][2]  = -sinY * sinZ - sinX * cosY * cosZ;
	result[1][3]  = 0.0;

  result[2][0]  = cosX * sinY;
  result[2][1]  = sinX;
  result[2][2] = cosX * cosY;
  result[2][3] = 0.0;

  result[3][0] = pos.x;
  result[3][1] = pos.y;
  result[3][2] = pos.z;
  result[3][3] = 1.0;
  
  return result;
}

void main()
{
  mat4 modelMatrix = makeInstanceTransform(gxl3d_Instance_Position, gxl3d_Instance_Rotation);
  vec4 modelSpacePos = modelMatrix * gxl3d_Position;
  vec4 viewSpacePos = gxl3d_ViewMatrix * modelSpacePos;  
  gl_Position = gxl3d_ProjectionMatrix * viewSpacePos;
  Vertex_C = vec4(1);
}