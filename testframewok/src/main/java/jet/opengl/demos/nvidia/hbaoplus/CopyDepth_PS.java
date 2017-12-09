package jet.opengl.demos.nvidia.hbaoplus;

class CopyDepth_PS extends BaseProgram{

	int m_DepthTexture = -1;
	
	void create(CharSequence FragmentShaderSource){
		super.create(getFullscreenTriangle_VS_GLSL(), FragmentShaderSource);
		
		m_DepthTexture = getUniformLocation("g_t0");
	}
	
	void setDepthTexture(/*const GFSDK_SSAO_GLFunctions& GL,*/ int target, int textureID)
    {
        setTexture(/*GL, */target, m_DepthTexture, textureID, 0);
    } 
}
