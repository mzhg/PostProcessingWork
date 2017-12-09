package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;

class Blur_PS extends BaseProgram{

	private int m_AODepthTextureNearest;
	private int m_AODepthTextureLinear;
	private int m_GlobalUniformBlock;
	
	void setAODepthTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int TextureId)
    {
        setTexture(/*GL,*/ GLenum.GL_TEXTURE_2D, m_AODepthTextureNearest, TextureId, 0/*, GL11.GL_NEAREST*/);

        if (m_AODepthTextureLinear != -1)
        {
            setTexture(/*GL,*/ GLenum.GL_TEXTURE_2D, m_AODepthTextureLinear,  TextureId, 1, GLenum.GL_LINEAR, GLenum.GL_CLAMP_TO_EDGE);
        }
    }
	
	void create(CharSequence FragmentShaderSource){
		create(/*GL,*/ getFullscreenTriangle_VS_GLSL(), FragmentShaderSource);

	    m_AODepthTextureNearest = getUniformLocation(/*GL,*/ "g_t0");
	    m_AODepthTextureLinear  = getUniformLocation(/*GL,*/ "g_t1");
	    m_GlobalUniformBlock    = getUniformBlockIndex(/*GL,*/ "GlobalConstantBuffer");

	    gl.glUniformBlockBinding(getProgram(), m_GlobalUniformBlock, BaseConstantBuffer.BINDING_POINT_GLOBAL_UBO);
//	    ASSERT_GL_ERROR(GL);
	}
}
