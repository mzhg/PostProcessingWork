package jet.opengl.postprocessing.buffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;

/**
 * Created by mazhen'gui on 2017/4/15.
 */

public class VertexArrayObject implements Disposeable {

    private static final int UNKOWN = 0;
    private static final int ENABLE = 1;
    private static final int DISABLE = 2;

    private static int g_VAOState = UNKOWN;
    private BufferBinding[] m_bindings;
    private BufferGL m_indices;

    private int m_vao;

    public void initlize(BufferBinding[] bindings, BufferGL indices){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        if(g_VAOState == UNKOWN){
            // Query the state
            GLAPIVersion version = gl.getGLAPIVersion();
            g_VAOState = (version.major >= 3 ? ENABLE : DISABLE);
        }

        if(m_vao == 0 && g_VAOState == ENABLE){
            m_vao = gl.glGenVertexArray();
        }

        if((bindings == null && indices == null) || g_VAOState != ENABLE){
            return;
        }

        m_bindings = bindings;
        m_indices = indices;
        GLStateTracker.getInstance().bindVAO(m_vao);
        _bind();
    }

    private void _bind(){
        if(m_bindings != null){
            for(BufferBinding binding : m_bindings){
                binding.bind();
            }
        }

        if(m_indices != null){
            m_indices.bind();
        }
    }

    private void _unbind(){
        if(m_bindings != null){
            for(BufferBinding binding : m_bindings){
                binding.unbind();
            }
        }

        if(m_indices != null){
            m_indices.unbind();
        }
    }

    public void bind() {
        if(g_VAOState == ENABLE){
            GLStateTracker.getInstance().bindVAO(m_vao);
        }else{
            _bind();
        }
    }
    public void unbind() {
        if(g_VAOState ==ENABLE){
            GLStateTracker.getInstance().bindVAO(0);
        }else{
            _unbind();
        }
    }

    @Override
    public void dispose() {
        if(g_VAOState ==ENABLE) {
            GLFuncProviderFactory.getGLFuncProvider().glDeleteVertexArray(m_vao);
            m_vao = 0;
        }
    }
}
