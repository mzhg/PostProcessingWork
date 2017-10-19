
#extension GL_NV_geometry_shader_passthrough : enable
layout(triangles) in;
//layout(triangle_strip,max_vertices=3) out;

layout(passthrough) in gl_PerVertex {
    vec4 gl_Position;
  };
  layout(passthrough) in vec4 m_f4UVAndScreenPos[];

  void main()
  {
    gl_Layer = gl_PrimitiveIDIn;
    gl_PrimitiveID = gl_PrimitiveIDIn;
  }