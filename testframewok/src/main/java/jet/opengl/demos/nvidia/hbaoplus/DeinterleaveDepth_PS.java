package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLenum;

class DeinterleaveDepth_PS extends BaseProgram{

	 private int m_DepthTexture = -1;
	 private int m_GlobalUniformBlock = -1;
	 private int m_PerPassUniformBlock = -1;
	 
	 void create(CharSequence FragmentShaderSource){
		create(/*GL,*/ getFullscreenTriangle_VS_GLSL(), FragmentShaderSource);

	    m_DepthTexture          = getUniformLocation(/*GL,*/ "g_t0");
	    m_GlobalUniformBlock    = getUniformBlockIndex(/*GL,*/ "GlobalConstantBuffer");
	    m_PerPassUniformBlock   = getUniformBlockIndex(/*GL,*/ "PerPassConstantBuffer");

	    gl.glUniformBlockBinding(getProgram(), m_GlobalUniformBlock, BaseConstantBuffer.BINDING_POINT_GLOBAL_UBO);
	    gl.glUniformBlockBinding(getProgram(), m_PerPassUniformBlock, BaseConstantBuffer.BINDING_POINT_PER_PASS_UBO);
//	    ASSERT_GL_ERROR(GL);
	 }
	 
	 void setDepthTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int textureId)
     {
        setTexture(/*GL, */GLenum.GL_TEXTURE_2D, m_DepthTexture, textureId, 0);
     }
}
