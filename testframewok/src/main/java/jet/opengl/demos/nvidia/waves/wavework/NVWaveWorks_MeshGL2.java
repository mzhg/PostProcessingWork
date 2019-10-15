package jet.opengl.demos.nvidia.waves.wavework;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferBinding;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/7/24.
 */

final class NVWaveWorks_MeshGL2 implements NVWaveWorks_Mesh, Disposeable{

    private BufferGL m_VB;
    private BufferGL m_IB;
    private VertexArrayObject m_VAO;

    private AttribDesc[] m_pVertexAttribDescs;
    private int	m_NumVertexAttribs;
    private GLFuncProvider gl;

    NVWaveWorks_MeshGL2(	AttribDesc[] AttributeDescs,
                            int NumAttributeDescs,
                            BufferGL vb,
                            BufferGL ib
    ){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        m_pVertexAttribDescs = AttributeDescs;
        m_NumVertexAttribs = NumAttributeDescs;
        m_VB = vb;
        m_IB = ib;

        m_VAO = new VertexArrayObject(false);
        BufferBinding vertexBinding = new BufferBinding(m_VB, AttributeDescs);
        m_VAO.initlize(CommonUtil.toArray(vertexBinding), m_IB);
    }

    @Override
    public HRESULT Draw(PrimitiveType PrimType, int BaseVertexIndex, int MinIndex, int NumVertices, int StartIndex, int PrimitiveCount,
                        int[] pShaderInputMappings) {
        // Must supply input mappings if we have attributes to hook up
        if(m_NumVertexAttribs > 0 && null == pShaderInputMappings)
        {
            return HRESULT.E_FAIL;
        }

        int IndexCount = 0;
        int GLPrimTopology = GLenum.GL_TRIANGLES;
        switch(PrimType)
        {
            case PT_TriangleStrip:
                GLPrimTopology = GLenum.GL_TRIANGLE_STRIP;
                IndexCount = 2 + PrimitiveCount;
                break;
            case PT_TriangleList:
                GLPrimTopology = GLenum.GL_TRIANGLES;
                IndexCount = 3 * PrimitiveCount;
                break;
            case PT_PatchList_3:
                GLPrimTopology = GLenum.GL_PATCHES;
                gl.glPatchParameteri(GLenum.GL_PATCH_VERTICES, 3);
                IndexCount = 3 * PrimitiveCount;
                break;
        }

        /*
        NVSDK_GLFunctions.glBindBuffer(GL_ARRAY_BUFFER, m_VB); CHECK_GL_ERRORS;
        NVSDK_GLFunctions.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_IB); CHECK_GL_ERRORS;

        for(GLuint i = 0; i < m_NumVertexAttribs; i++)
        {
            NVSDK_GLFunctions.glEnableVertexAttribArray(pShaderInputMappings[i]); CHECK_GL_ERRORS;
            NVSDK_GLFunctions.glVertexAttribPointer(pShaderInputMappings[i], m_pVertexAttribDescs[i].Size, m_pVertexAttribDescs[i].Type, m_pVertexAttribDescs[i].Normalized,m_pVertexAttribDescs[i].Stride,(const GLvoid *)(m_pVertexAttribDescs[i].Offset + BaseVertexIndex*m_pVertexAttribDescs[i].Stride)); CHECK_GL_ERRORS;
        }*/
        m_VAO.bind();

        gl.glDrawElements(GLPrimTopology, IndexCount, GLenum.GL_UNSIGNED_INT, (StartIndex * /*sizeof(GLuint)*/ 4));  //CHECK_GL_ERRORS;

        m_VAO.unbind();
        /*for(GLuint i = 0; i < m_NumVertexAttribs; i++)
        {
            NVSDK_GLFunctions.glDisableVertexAttribArray(pShaderInputMappings[i]); CHECK_GL_ERRORS;
        }

        NVSDK_GLFunctions.glBindBuffer(GL_ARRAY_BUFFER, 0); CHECK_GL_ERRORS;
        NVSDK_GLFunctions.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0); CHECK_GL_ERRORS;*/

        return HRESULT.S_OK;
    }

    @Override
    public HRESULT PreserveState(GFSDK_WaveWorks_Savestate pSavestateImpl) {
        return HRESULT.S_OK;
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_VB);
        CommonUtil.safeRelease(m_IB);
    }
}
