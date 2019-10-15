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

import static jet.opengl.demos.nvidia.waves.wavework.HRESULT.E_FAIL;
import static jet.opengl.demos.nvidia.waves.wavework.HRESULT.S_OK;
import static jet.opengl.postprocessing.common.GLenum.GL_PATCH_VERTICES;

/**
 * Created by mazhen'gui on 2017/7/24.
 */

final class NVWaveWorks_MeshD3D11 implements NVWaveWorks_Mesh, Disposeable {

//    ID3D11Device* m_pd3dDevice;
    private AttribDesc[] m_pLayout;
    private BufferGL m_pVB;
    private BufferGL m_pIB;
    private VertexArrayObject m_pVAO;
    private int m_VertexStride;
    private GLFuncProvider gl;

    NVWaveWorks_MeshD3D11(	//ID3D11Device* pD3DDevice,
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

    @Override
    public HRESULT Draw(PrimitiveType PrimType, int BaseVertexIndex, int MinIndex, int NumVertices, int StartIndex, int PrimitiveCount, int[] pShaderInputMappings) {
        HRESULT hr;

//        ID3D11DeviceContext* pDC_d3d11 = pGC->d3d11();

//        const UINT VBOffset = 0;
//        pDC_d3d11->IASetVertexBuffers(0, 1, &m_pVB, &m_VertexStride, &VBOffset);
//        pDC_d3d11->IASetIndexBuffer(m_pIB, DXGI_FORMAT_R32_UINT, 0);
//        pDC_d3d11->IASetInputLayout(m_pLayout);

        int d3dPrimTopology = 0;
        int IndexCount = 0;
        switch(PrimType)
        {
            case PT_TriangleStrip:
                d3dPrimTopology = GLenum.GL_TRIANGLE_STRIP;
                IndexCount = 2 + PrimitiveCount;
                break;
            case PT_TriangleList:
                d3dPrimTopology = GLenum.GL_TRIANGLES;
                IndexCount = 3 * PrimitiveCount;
                break;
            case PT_PatchList_3:
                d3dPrimTopology = GLenum.GL_PATCHES;
                IndexCount = 3 * PrimitiveCount;
                gl.glPatchParameteri(GL_PATCH_VERTICES, 3);
                break;
        }

        if(d3dPrimTopology != 0)
        {
//            pDC_d3d11->IASetPrimitiveTopology(d3dPrimTopology);
//            pDC_d3d11->DrawIndexed(IndexCount, StartIndex, BaseVertexIndex);
            m_pVAO.bind();
            gl.glDrawElements/*BaseVertex*/(d3dPrimTopology, IndexCount, GLenum.GL_UNSIGNED_INT, StartIndex * 4);
            m_pVAO.unbind();
            hr = S_OK;
        }
        else
        {
            hr = E_FAIL;
        }

        return hr;
    }

    @Override
    public HRESULT PreserveState(GFSDK_WaveWorks_Savestate pSavestateImpl) {
        // TODO
        return HRESULT.S_OK;
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_pVB);
        CommonUtil.safeRelease(m_pIB);
        CommonUtil.safeRelease(m_pVAO);
    }
}
