#include "SceneRenderCommon.glsl"

struct UnitCubeTransform
{
  float4x4              m_tr;
  float4x4              m_inverse;
  float4x4              m_forward;
  float4                m_color;
};

layout(binding=0) uniform CB_MODEL_DATA {
  UnitCubeTransform   g_UnitCubeTransform;
};


layout(location =0) out float4 Out_f4Color;

void main()
{
  Out_f4Color = g_UnitCubeTransform.m_color;
}