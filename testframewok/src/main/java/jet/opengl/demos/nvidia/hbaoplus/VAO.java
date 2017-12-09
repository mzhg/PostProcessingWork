package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

final class VAO {

	private int m_VertexArrayObjectId = 0;
	private GLFuncProvider gl;
	
	void create()
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
		m_VertexArrayObjectId = gl.glGenVertexArray();
    }

    void release()
    {
        gl.glDeleteVertexArray(m_VertexArrayObjectId);
        m_VertexArrayObjectId = 0;
    }

    void bind()
    {
        gl.glBindVertexArray(m_VertexArrayObjectId);
    }
}
