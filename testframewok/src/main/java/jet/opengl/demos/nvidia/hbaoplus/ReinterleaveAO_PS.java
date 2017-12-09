package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;

class ReinterleaveAO_PS extends BaseProgram{

	private int m_AOTexture = -1;
	private int m_DepthTexture = -1;
	private int m_GlobalUniformBlock = -1;
	
	void setAOTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int TextureId)
    {
        setTexture(/*GL,*/ GLenum.GL_TEXTURE_2D_ARRAY, m_AOTexture, TextureId, 0);
    }

    void setDepthTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int TextureId)
    {
        setTexture(/*GL,*/ GLenum.GL_TEXTURE_2D, m_DepthTexture, TextureId, 1);
    }
    
    void create(CharSequence FragmentShaderSource){
    	create(/*GL,*/ getFullscreenTriangle_VS_GLSL(), FragmentShaderSource);

        m_AOTexture             = getUniformLocation(/*GL,*/ "g_t0");
        m_DepthTexture          = getUniformLocation(/*GL,*/ "g_t1");
        m_GlobalUniformBlock    = getUniformBlockIndex(/*GL,*/ "GlobalConstantBuffer");

        if (m_GlobalUniformBlock != -1)
        {
            gl.glUniformBlockBinding(getProgram(), m_GlobalUniformBlock, BaseConstantBuffer.BINDING_POINT_GLOBAL_UBO);
//            ASSERT_GL_ERROR(GL);
        }
    }
}
