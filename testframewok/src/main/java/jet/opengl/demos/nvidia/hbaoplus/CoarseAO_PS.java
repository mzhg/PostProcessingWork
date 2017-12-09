package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;

class CoarseAO_PS extends BaseProgram{

	private int m_QuarterResDepthTexture = -1;
	private int m_FullResNormalTexture = -1;
	private int m_GlobalUniformBlock = -1;
	private int m_PerPassUniformBlock = -1;
	private int m_NormalMatrixUniformBlock = -1;
	
	void setDepthTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int TextureId, int WrapMode)
    {
        setTexture(/*GL,*/ GLenum.GL_TEXTURE_2D_ARRAY, m_QuarterResDepthTexture, TextureId, 0, GLenum.GL_NEAREST, WrapMode);
    }

    void setNormalTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int target, int textureID)
    {
        setTexture(/*GL,*/ target, m_FullResNormalTexture, textureID, 1);
    }
    
    void create(CharSequence FragmentShaderSource){
    	final String VertexShaderSource = 
    	            "void main()" +
    	            "{" +
    	            "    gl_Position = vec4(0); "+
    	            "}"
    	        ;

    	final String GeometryShaderSource = 
    	        // Must match the uniform declaration from shaders/out/GL/CoarseAO_PS.cpp
//    	        STRINGIFY(
    	            "layout(std140) uniform;" +
    	            "struct PerPassConstantBuffer_0_Type" +
    	            "{"+
    	                "vec4 f4Jitter;"+
    	                "vec2 f2Offset;"+
    	                "float fSliceIndex;"+
    	                "uint uSliceIndex;"+
    	            "};"+
    	            "uniform PerPassConstantBuffer"+
    	            "{"+
    	                "PerPassConstantBuffer_0_Type PerPassConstantBuffer_0;"+
    	            "};\n" +
//    	        )
//    	        STRINGIFY(
    	            "layout(points) in;"+
    	            "layout(triangle_strip, max_vertices = 3) out;"+

    	            "void main()" +
    	            "{"+
    	                "gl_Layer = int(PerPassConstantBuffer_0.uSliceIndex);"+

    	                "for (int VertexID = 0; VertexID < 3; VertexID++)"+
    	                "{"+
    	                    "vec2 texCoords = vec2( (VertexID << 1) & 2, VertexID & 2 );"+
    	                    "gl_Position = vec4( texCoords * vec2( 2.0, 2.0 ) + vec2( -1.0, -1.0) , 0.0, 1.0 );"+
    	                    "EmitVertex();"+
    	                "}"+
    	                "EndPrimitive();"+
    	            "}";
//    	        );

    	    create(/*GL,*/ VertexShaderSource, GeometryShaderSource, FragmentShaderSource);

    	    m_QuarterResDepthTexture    = getUniformLocation(/*GL,*/ "g_t0");
    	    m_FullResNormalTexture      = getUniformLocation(/*GL,*/ "g_t1");
    	    m_GlobalUniformBlock        = getUniformBlockIndex(/*GL,*/ "GlobalConstantBuffer");
    	    m_PerPassUniformBlock       = getUniformBlockIndex(/*GL,*/ "PerPassConstantBuffer");

    	    gl.glUniformBlockBinding(getProgram(), m_GlobalUniformBlock, BaseConstantBuffer.BINDING_POINT_GLOBAL_UBO);
			gl.glUniformBlockBinding(getProgram(), m_PerPassUniformBlock, BaseConstantBuffer.BINDING_POINT_PER_PASS_UBO);
//    	    ASSERT_GL_ERROR(GL);
    }
}
