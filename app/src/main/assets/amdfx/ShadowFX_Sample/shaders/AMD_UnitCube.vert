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


out gl_PerVertex
{
	vec4 gl_Position;
};

void main()
{
  const float4 vertex[] =
  {
    { -1.0000, -1.0000,  1.0000, 1 },
    { -1.0000, -1.0000, -0.0000, 1 },
    {  1.0000, -1.0000, -0.0000, 1 },
    {  1.0000, -1.0000,  1.0000, 1 },
    { -1.0000,  1.0000,  1.0000, 1 },
    {  1.0000,  1.0000,  1.0000, 1 },
    {  1.0000,  1.0000, -0.0000, 1 },
    { -1.0000,  1.0000, -0.0000, 1 }
  };

  const int index[]=
  {
     1, 2, 3,
     3, 4, 1,
     5, 6, 7,
     7, 8, 5,
     1, 4, 6,
     6, 5, 1,
     4, 3, 7,
     7, 6, 4,
     3, 2, 8,
     8, 7, 3,
     2, 1, 5,
     5, 8, 2
  };
  
  int vertex_id = gl_VertexID;
  gl_Position = mul (vertex[index[vertex_id] - 1], g_UnitCubeTransform.m_tr );
}