package jet.opengl.postprocessing.shader;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.core.OpenGLProgram;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public class ShaderProgram implements OpenGLProgram {
    protected int m_programId;
    protected int m_target;

    private String m_name = getClass().getName();

    @Override
    public int getProgram() {
        return m_programId;
    }

    public int getTarget() {return m_target;}

    @Override
    public void setName(String name) {
        m_name = name;
    }

    @Override
    public String getName() {
        return m_name;
    }

    @Override
    public void dispose() {
        if(m_programId != 0) {
            GLFuncProviderFactory.getGLFuncProvider().glDeleteProgram(m_programId);
            m_programId = 0;
        }
    }
}
