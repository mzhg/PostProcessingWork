package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;

class ReconstructNormal_PS extends BaseProgram{
	 private int m_FullResDepthTexture = -1;
	 private int m_FullResNormalTexture = -1;
	 private int m_GlobalUniformBlock = -1;
	 private int m_NormalMatrixUniformBlock = -1;
	 
	 void setDepthTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int TextureId, int WrapMode)
	    {
	        if (m_FullResDepthTexture != -1)
	        {
	            setTexture(/*GL,*/ GLenum.GL_TEXTURE_2D, m_FullResDepthTexture, TextureId, 0, GLenum.GL_NEAREST, WrapMode);
	        }
	    }

	    void setNormalTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int target, int textureID)
	    {
	        if (m_FullResNormalTexture != -1)
	        {
	            setTexture(/*GL,*/ target, m_FullResNormalTexture, textureID, 1);
	        }
	    }
	    
	void create(CharSequence FragmentShaderSource){
		create(/*GL,*/ getFullscreenTriangle_VS_GLSL(), FragmentShaderSource);

	    m_FullResDepthTexture       = getUniformLocation(/*GL,*/ "g_t0");
	    m_FullResNormalTexture      = getUniformLocation(/*GL,*/ "g_t1");
	    m_GlobalUniformBlock        = getUniformBlockIndex(/*GL,*/ "GlobalConstantBuffer");

	    if (m_GlobalUniformBlock != GLenum.GL_INVALID_INDEX)
	    {
	        gl.glUniformBlockBinding(getProgram(), m_GlobalUniformBlock, BaseConstantBuffer.BINDING_POINT_GLOBAL_UBO);
	    }

//	    ASSERT_GL_ERROR(GL);
	}
}
