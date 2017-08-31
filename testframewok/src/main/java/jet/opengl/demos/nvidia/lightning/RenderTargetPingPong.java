package jet.opengl.demos.nvidia.lightning;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

final class RenderTargetPingPong {
    private Texture2D	m_source;
    private Texture2D	m_target;
    private Texture2D	m_last_target;

    private Texture2D m_depth_stencil_view;
    private RenderTargets m_render_target;

    public RenderTargetPingPong(Texture2D	source,	Texture2D	target, Texture2D depth_stencil_view, RenderTargets render_target ){
        m_depth_stencil_view=depth_stencil_view;
        m_source = source;
        m_target = target;
        m_last_target =source;

        m_render_target = render_target;
    }

    void Apply(GLSLProgram technique){
//        ID3D10Buffer* zero = 0;
//        UINT nought = 0;
//        device->IASetVertexBuffers(0,1,&zero,&nought,&nought);
//        device->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);
//        device->IASetInputLayout(0);
//
//        {
//            ID3D10RenderTargetView* view[] = { m_target->RenderTargetView() };
//            device->OMSetRenderTargets(1, const_cast<ID3D10RenderTargetView**> (view), m_depth_stencil_view);
//        }
        m_render_target.bind();
        if(m_depth_stencil_view != null){
            m_render_target.setRenderTextures(CommonUtil.toArray(m_target, m_depth_stencil_view), null);
        }else{
            m_render_target.setRenderTexture(m_target, null);
        }
//        m_shader_resource_variable->SetResource(m_source->ShaderResourceView());
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_source.getTarget(), m_source.getTexture());
        gl.glViewport(0,0,m_target.getWidth(), m_target.getHeight());

//        for(UINT n = 0; n < Effect::NumPasses(technique); ++n)
        {
//            technique->GetPassByIndex(n)->Apply(0);
            technique.enable();
//            device->Draw(4,0);
            gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        }

//        m_shader_resource_variable->SetResource(0);
        m_last_target = m_target;
//        std::swap(m_source,m_target);\
        Texture2D tmp = m_source;
        m_source = m_target;
        m_target = tmp;

    }
    Texture2D	LastTarget(){
        return m_last_target;
    }
}
