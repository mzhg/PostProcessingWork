package jet.opengl.demos.nvidia.hbaoplus;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

class BaseConstantBuffer {

	static final int BINDING_POINT_GLOBAL_UBO = 0;
	static final int BINDING_POINT_NORMAL_UBO = 1;
	static final int BINDING_POINT_PER_PASS_UBO = 2;
	
	int m_ByteWidth;
    int m_BufferId;
    int m_BindingPoint;

    private GLFuncProvider gl;
    
    public BaseConstantBuffer(int byteWidth, int bindingPoint) {
    	m_ByteWidth = byteWidth;
    	m_BindingPoint = bindingPoint;
    	m_BufferId = 0;
    }
	
    void create(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
//        ASSERT(!m_BufferId);
        m_BufferId = gl.glGenBuffer();

        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, m_BufferId);
        gl.glBufferData(GLenum.GL_UNIFORM_BUFFER, m_ByteWidth, GLenum.GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GLenum.GL_UNIFORM_BUFFER, 0);

        System.out.println(getClass().getSimpleName() + " Create!");
    }

    void release(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        gl.glDeleteBuffer(m_BufferId);
        m_BufferId = 0;
        
        System.out.println(getClass().getSimpleName() + " Released!");
    }

    void unbind(/*const GFSDK_SSAO_GLFunctions& GL*/)
    {
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, m_BindingPoint, 0);
//        ASSERT_GL_ERROR(GL);
    }

    void updateCB(/*const GFSDK_SSAO_GLFunctions& GL,*/ ByteBuffer pData)
    {
//        ASSERT(m_BufferId);
    	
    	if(m_BufferId == 0){
    		create();
    	}

        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, m_BindingPoint, m_BufferId);

        // Do not use glMapBuffer for updating constant buffers (slow path on GL).
        // glBufferSubData has a fast path for UBO.
        gl.glBufferSubData(GLenum.GL_UNIFORM_BUFFER, 0, pData);
        GLCheck.checkError();
    }

    int getBindingPoint()
    {
        return m_BindingPoint;
    }

    int getBufferId()
    {
        return m_BufferId;
    }
}
