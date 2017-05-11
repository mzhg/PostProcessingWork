
#extension GL_NV_geometry_shader_passthrough : enable
layout(passthrough) in gl_PerVertex {
    vec4 gl_Position;
  };
  layout(passthrough) in Inputs {
    vec4 m_f4UVAndScreenPos;
  };

  void main()
  {
    gl_Layer = gl_PrimitiveIDIn;
    gl_PrimitiveID = gl_PrimitiveIDIn;
  }