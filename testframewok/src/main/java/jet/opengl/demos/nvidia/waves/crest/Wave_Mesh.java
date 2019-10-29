package jet.opengl.demos.nvidia.waves.crest;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.CommonUtil;

final class Wave_Mesh implements Disposeable {

    private AttribDesc[] m_pLayout;
    private BufferGL m_pVB;
    private BufferGL m_pIB;
    private VertexArrayObject m_pVAO;
    private int m_VertexStride;
    private GLFuncProvider gl;

    private int m_PrimTopology;
    private int m_IndexCount;

    private final BoundingBox m_AABB = new BoundingBox();

    Wave_Mesh(	//ID3D11Device* pD3DDevice,
                              AttribDesc[] pLayout,
                              BufferGL pVertexBuffer,
                              BufferGL pIndexBuffer,
                              int VertexStride
    ){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_pLayout = pLayout;
        m_pVB = pVertexBuffer;
        m_pIB = pIndexBuffer;
        m_VertexStride = VertexStride;

        m_pVAO = new VertexArrayObject();
        BufferBinding vertexBinding = new BufferBinding(m_pVB, pLayout);
        m_pVAO.initlize(CommonUtil.toArray(vertexBinding), m_pIB);
        m_pVAO.unbind();
    }

    void setIndice(int primTopology, int indexCount){
        m_PrimTopology = primTopology;
        m_IndexCount = indexCount;
    }

    public void Draw() {
        if(m_PrimTopology != 0)
        {
            m_pVAO.bind();
            gl.glDrawElements/*BaseVertex*/(m_PrimTopology, m_IndexCount, GLenum.GL_UNSIGNED_INT, /*StartIndex * 4*/0);
            m_pVAO.unbind();
        }
    }

    public void setBoundingBox(BoundingBox box){
        m_AABB.set(box);
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_pVB);
        CommonUtil.safeRelease(m_pIB);
        CommonUtil.safeRelease(m_pVAO);
    }
}
