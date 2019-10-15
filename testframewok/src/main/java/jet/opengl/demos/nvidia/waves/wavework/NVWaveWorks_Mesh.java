package jet.opengl.demos.nvidia.waves.wavework;

import java.nio.Buffer;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/7/24.
 */

interface NVWaveWorks_Mesh extends Disposeable{
    enum PrimitiveType
    {
        PT_TriangleStrip,
        PT_TriangleList,
        PT_PatchList_3
    }

    static HRESULT CreateD3D11(    //ID3D11Device* pD3DDev,
                                   AttribDesc[] pInputElementDescs,
                                   int NumAttributeDescs,
                                   int VertexStride,
                                   Buffer pVertData,
                                   int NumVerts,
                                   int[] pIndexData,
                                   int NumIndices,
                                   NVWaveWorks_Mesh[] ppMesh
    ){
        HRESULT hr;

//        ID3D11InputLayout* pLayout = NULL;
//        V_RETURN(pD3DDev->CreateInputLayout(pInputElementDescs, NumElements, pShaderBytecodeWithInputSignature, BytecodeLength, &pLayout));

//        ID3D11Buffer* pVB = NULL;
//        D3D11_BUFFER_DESC vbDesc;
//        vbDesc.ByteWidth = NumVerts * VertexStride;
//        vbDesc.Usage = D3D11_USAGE_IMMUTABLE;
//        vbDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
//        vbDesc.CPUAccessFlags = 0;
//        vbDesc.MiscFlags = 0;
//        vbDesc.StructureByteStride = 0;
//
//        D3D11_SUBRESOURCE_DATA vSrd;
//        vSrd.pSysMem = pVertData;
//        vSrd.SysMemPitch = 0;
//        vSrd.SysMemSlicePitch = 0;
//
//        V_RETURN(pD3DDev->CreateBuffer(&vbDesc, &vSrd, &pVB));
        BufferGL pVB = new BufferGL();
        pVB.initlize(GLenum.GL_ARRAY_BUFFER, NumVerts * VertexStride, pVertData, GLenum.GL_STATIC_DRAW);


//        ID3D11Buffer* pIB = NULL;
//        D3D11_BUFFER_DESC ibDesc;
//        ibDesc.ByteWidth = NumIndices * sizeof(DWORD);
//        ibDesc.Usage = D3D11_USAGE_IMMUTABLE;
//        ibDesc.BindFlags = D3D11_BIND_INDEX_BUFFER;
//        ibDesc.CPUAccessFlags = 0;
//        ibDesc.MiscFlags = 0;
//        ibDesc.StructureByteStride = 0;
//
//        D3D11_SUBRESOURCE_DATA iSrd;
//        iSrd.pSysMem = pIndexData;
//        iSrd.SysMemPitch = 0;
//        iSrd.SysMemSlicePitch = 0;
//
//        V_RETURN(pD3DDev->CreateBuffer(&ibDesc, &iSrd, &pIB));
        BufferGL pIB = new BufferGL();
        pIB.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, NumIndices * 4, CacheBuffer.wrap(pIndexData), GLenum.GL_STATIC_DRAW);

        ppMesh[0] = new NVWaveWorks_MeshD3D11(/*pD3DDev,*/ pInputElementDescs, pVB, pIB, VertexStride);

//        pLayout->Release();
//        pVB->Release();
//        pIB->Release();
        pVB.unbind();
        pIB.unbind();
        return HRESULT.S_OK;
    }

//    static HRESULT CreateGnm(	int VertexStride,
//                                 const void* pVertData,
//                                 int NumVerts,
//                                 const DWORD* pIndexData,
//                                 int NumIndices,
//                                 NVWaveWorks_Mesh** ppMesh
//    );

    static HRESULT CreateGL2(   AttribDesc[] pInputElementDescs,
                                int NumAttributeDescs,
                                int VertexStride,
                                Buffer pVertData,
                                int NumVerts,
                                int[] pIndexData,
                                int NumIndices,
                                NVWaveWorks_Mesh[] ppMesh
    ){
        HRESULT hr;

//        GLuint VB;
//        GLuint IB;

        // creating VB/IB and filling with the data
//        NVSDK_GLFunctions.glGenBuffers(1,&VB); CHECK_GL_ERRORS;
//        NVSDK_GLFunctions.glBindBuffer(GL_ARRAY_BUFFER, VB); CHECK_GL_ERRORS;
//        NVSDK_GLFunctions.glBufferData(GL_ARRAY_BUFFER, NumVerts*VertexStride, pVertData, GL_STATIC_DRAW); CHECK_GL_ERRORS;
//        NVSDK_GLFunctions.glBindBuffer(GL_ARRAY_BUFFER, 0); CHECK_GL_ERRORS;

        BufferGL pVB = new BufferGL();
        pVB.initlize(GLenum.GL_ARRAY_BUFFER, NumVerts * VertexStride, pVertData, GLenum.GL_STATIC_DRAW);

//        NVSDK_GLFunctions.glGenBuffers(1, &IB);  CHECK_GL_ERRORS;
//        NVSDK_GLFunctions.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, IB); CHECK_GL_ERRORS;
//        NVSDK_GLFunctions.glBufferData(GL_ELEMENT_ARRAY_BUFFER, NumIndices * sizeof(DWORD), pIndexData, GL_STATIC_DRAW); CHECK_GL_ERRORS;
//        NVSDK_GLFunctions.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0); CHECK_GL_ERRORS;

        BufferGL pIB = new BufferGL();
        pIB.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, NumIndices * 4, CacheBuffer.wrap(pIndexData), GLenum.GL_STATIC_DRAW);

        ppMesh[0] = new NVWaveWorks_MeshGL2(/*pD3DDev,*/ pInputElementDescs, pInputElementDescs.length, pVB, pIB);

        return HRESULT.S_OK;
    }

    HRESULT Draw(//	Graphics_Context* pGC,
                             PrimitiveType PrimType,
                             int BaseVertexIndex,
                             int MinIndex,
                             int NumVertices,
                             int StartIndex,
                             int PrimitiveCount,
                             int[] pShaderInputMappings
    );

    HRESULT PreserveState(/*Graphics_Context* pGC,*/ GFSDK_WaveWorks_Savestate pSavestateImpl);
}
