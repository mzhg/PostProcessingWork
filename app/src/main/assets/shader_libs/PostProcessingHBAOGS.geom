
  in Inputs {
    vec4 m_f4UVAndScreenPos;
  } IN[];


  in gl_PerVertex {
    vec4 gl_Position;
  }gl_in[];

  out gl_PerVertex{
  	vec4 gl_Position;
  };

  out vec4 m_f4UVAndScreenPos;

  void main()
  {
    for (int i = 0; i < 3; i++){
      m_f4UVAndScreenPos = IN[i].m_f4UVAndScreenPos;
      gl_Layer = gl_PrimitiveIDIn;
      gl_PrimitiveID = gl_PrimitiveIDIn;
      gl_Position = gl_in[i].gl_Position;
      EmitVertex();
    }
  }