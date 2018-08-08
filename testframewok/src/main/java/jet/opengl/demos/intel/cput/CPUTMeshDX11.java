package jet.opengl.demos.intel.cput;

import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

public class CPUTMeshDX11 extends CPUTMesh {
    int    mD3DMeshTopology;
    D3D11_INPUT_ELEMENT_DESC []mpLayoutDescription;
    int                       mNumberOfInputLayoutElements;
    ID3D11InputLayout        mpInputLayout;
    ID3D11InputLayout        mpShadowInputLayout;
    int                      mVertexStride;

    final D3D11_BUFFER_DESC         mVertexBufferDesc = new D3D11_BUFFER_DESC();
    int                      mVertexBufferOffset;
    int                      mVertexCount;
    BufferGL             mpVertexBuffer;
    BufferGL             mpStagingVertexBuffer;
    CPUTMapType              mVertexBufferMappedType = CPUTMapType.CPUT_MAP_UNDEFINED;
    BufferGL             mpVertexBufferForSRVDX; // Need SRV, but _real_ DX won't allow for _real_ VB
    BufferGL             mpVertexView;
    CPUTBuffer             mpVertexBufferForSRV;


    int                      mIndexCount;
    int               mIndexBufferFormat;
    BufferGL             mpIndexBuffer;
    final D3D11_BUFFER_DESC         mIndexBufferDesc = new D3D11_BUFFER_DESC();
    BufferGL             mpStagingIndexBuffer;
    CPUTMapType              mIndexBufferMappedType = CPUTMapType.CPUT_MAP_UNDEFINED;
    private GLFuncProvider gl;

    @Override
    public ByteBuffer MapVertices(CPUTRenderParameters params, int type, boolean wait) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer MapIndices(CPUTRenderParameters params, int type, boolean wait) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void UnmapVertices(CPUTRenderParameters params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void UnmapIndices(CPUTRenderParameters params) {
        throw new UnsupportedOperationException();
    }

    public  BufferGL             GetIndexBuffer()  { return mpIndexBuffer; }
    public  BufferGL             GetVertexBuffer() { return mpVertexBuffer; }
    public  void                      SetMeshTopology(int meshTopology){
        assert ( meshTopology > 0 && meshTopology <= 5);
        super.SetMeshTopology(meshTopology);
        // The CPUT enum has the same values as the D3D enum.  Will likely need an xlation on OpenGL.
        /*mD3DMeshTopology = (D3D_PRIMITIVE_TOPOLOGY)meshTopology;*/
        mD3DMeshTopology = SDKmesh.convertDXDrawCMDToGL(meshTopology);
    }
    @Override
    public  void  CreateNativeResources( CPUTModel pModel, int meshIdx, int vertexDataInfoArraySize,
                                                       CPUTBufferInfo[] pVertexDataInfo, byte[] pVertexData, CPUTBufferInfo pIndexDataInfo, int[] pIndexData ){
        /*CPUTResult result = CPUT_SUCCESS;
        HRESULT hr;
        ID3D11Device *pD3dDevice = CPUT_DX11::GetDevice();*/
        gl = GLFuncProviderFactory.getGLFuncProvider();

        // Release the layout, offset, stride, and vertex buffer structure objects
        ClearAllObjects();

        // allocate the layout, offset, stride, and vertex buffer structure objects
        mpLayoutDescription = new D3D11_INPUT_ELEMENT_DESC[vertexDataInfoArraySize];

        // Create the index buffer
        /*D3D11_SUBRESOURCE_DATA resourceData;*/
        if(null!=pIndexData)
        {
            mIndexCount = pIndexDataInfo.mElementCount;

            // set the data format info
            /*ZeroMemory( &mIndexBufferDesc, sizeof(mIndexBufferDesc) );*/
            mIndexBufferDesc.zeros();
            mIndexBufferDesc.Usage = /*D3D11_USAGE_DEFAULT*/ GLenum.GL_STATIC_DRAW;
            mIndexBufferDesc.ByteWidth = mIndexCount * pIndexDataInfo.mElementSizeInBytes;
            mIndexBufferDesc.BindFlags = /*D3D11_BIND_INDEX_BUFFER*/0;
            mIndexBufferDesc.CPUAccessFlags = 0;  // default to no cpu access for speed

            // create the buffer
            /*ZeroMemory( &resourceData, sizeof(resourceData) );
            resourceData.pSysMem = pIndexData;
            hr = pD3dDevice->CreateBuffer( &mIndexBufferDesc, &resourceData, &mpIndexBuffer );
            ASSERT(!FAILED(hr), _L("Failed creating index buffer") );
            CPUTSetDebugName( mpIndexBuffer, _L("Index buffer") );*/
            mpIndexBuffer = new BufferGL();
            mpIndexBuffer.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, mIndexBufferDesc.ByteWidth, CacheBuffer.wrap(pIndexData), GLenum.GL_STATIC_DRAW);
            mpIndexBuffer.unbind();

            // set the DX index buffer format
            if(pIndexDataInfo.mElementComponentCount != 1){
                throw new IllegalArgumentException();
            }

            if(pIndexDataInfo.mElementType == CPUT_U32){
                mIndexBufferFormat = GLenum.GL_UNSIGNED_INT;
            }else if(pIndexDataInfo.mElementType == CPUT_U16){
                mIndexBufferFormat = GLenum.GL_UNSIGNED_SHORT;
            }else if(pIndexDataInfo.mElementType == CPUT_U8){
                mIndexBufferFormat = GLenum.GL_UNSIGNED_BYTE;
            }
        }

        // set up data format info
        mVertexCount = pVertexDataInfo[0].mElementCount;

        /*ZeroMemory( &mVertexBufferDesc, sizeof(mVertexBufferDesc) );*/
        mVertexBufferDesc.zeros();
        mVertexBufferDesc.Usage = /*D3D11_USAGE_DEFAULT*/0;
        // set the stride for one 'element' block of verts
        mVertexStride = pVertexDataInfo[vertexDataInfoArraySize-1].mOffset + pVertexDataInfo[vertexDataInfoArraySize-1].mElementSizeInBytes; // size in bytes of a single vertex block
        mVertexBufferDesc.ByteWidth = mVertexCount * mVertexStride; // size in bytes of entire buffer
        mVertexBufferDesc.BindFlags = /*D3D11_BIND_VERTEX_BUFFER*/0;
        mVertexBufferDesc.CPUAccessFlags = 0;  // default to no cpu access for speed
        // create the buffer
        /*ZeroMemory( &resourceData, sizeof(resourceData) );
        resourceData.pSysMem = pVertexData;
        hr = pD3dDevice->CreateBuffer( &mVertexBufferDesc, &resourceData, &mpVertexBuffer );
        ASSERT( !FAILED(hr), _L("Failed creating vertex buffer") );
        CPUTSetDebugName( mpVertexBuffer, _L("Vertex buffer") );*/
        mpVertexBuffer = new BufferGL();
        mpVertexBuffer.setName("Vertex buffer");
        mpVertexBuffer.initlize(GLenum.GL_ARRAY_BUFFER, mVertexBufferDesc.ByteWidth, CacheBuffer.wrap(pVertexData), GLenum.GL_STATIC_DRAW);
        mpVertexBuffer.unbind();


        mpVertexView = mpVertexBufferForSRVDX = mpVertexBuffer;
        String name = "@VertexBuffer" + pModel.toString() + meshIdx;
        mpVertexBufferForSRV = new CPUTBufferDX11( name, mpVertexBufferForSRVDX/*, mpVertexView*/ );
        CPUTAssetLibrary.GetAssetLibrary().AddBuffer( name, mpVertexBufferForSRV );

        // build the layout object
        int currentByteOffset=0;
        mNumberOfInputLayoutElements = vertexDataInfoArraySize;
        for(int ii=0; ii<vertexDataInfoArraySize; ii++)
        {
            mpLayoutDescription[ii] = new D3D11_INPUT_ELEMENT_DESC();
            mpLayoutDescription[ii].SemanticName  = pVertexDataInfo[ii].mpSemanticName; // string name that matches
            mpLayoutDescription[ii].SemanticIndex = pVertexDataInfo[ii].mSemanticIndex; // if we have more than one
            mpLayoutDescription[ii].Format = ConvertToDirectXFormat(pVertexDataInfo[ii].mElementType, pVertexDataInfo[ii].mElementComponentCount);
            mpLayoutDescription[ii].InputSlot = 0; // TODO: We support only a single stream now.  Support multiple streams.
            mpLayoutDescription[ii].InputSlotClass = /*D3D11_INPUT_PER_VERTEX_DATA*/0;
            mpLayoutDescription[ii].InstanceDataStepRate = 0;
            mpLayoutDescription[ii].AlignedByteOffset = currentByteOffset;
            currentByteOffset += pVertexDataInfo[ii].mElementSizeInBytes;
        }
    }

    @Override
    public  void BindVertexShaderLayout(CPUTMaterial pMaterial, CPUTMaterial pShadowCastMaterial){
        /*ID3D11Device *pDevice = CPUT_DX11::GetDevice();*/

        if( pMaterial != null)
        {
            // Get the vertex layout for this shader/format comb
            // If already exists, then GetLayout() returns the existing layout for reuse.
            ShaderProgram pVertexShader = ((CPUTMaterialDX11)pMaterial).GetVertexShader();
            /*SAFE_RELEASE(mpInputLayout);*/
            mpInputLayout = CPUTInputLayoutCacheDX11.GetInputLayoutCache().GetLayout(/*pDevice,*/ mpLayoutDescription, pVertexShader.getProgram());
        }

        if( pShadowCastMaterial != null)
        {
            ShaderProgram pVertexShader = ((CPUTMaterialDX11)pShadowCastMaterial).GetVertexShader();
            /*SAFE_RELEASE(mpShadowInputLayout);*/
            mpShadowInputLayout = CPUTInputLayoutCacheDX11.GetInputLayoutCache().GetLayout(/*pDevice,*/ mpLayoutDescription, pVertexShader.getProgram());
        }
    }

    public D3D11_INPUT_ELEMENT_DESC[] GetLayoutDescription() { return mpLayoutDescription; }

    //-----------------------------------------------------------------------------
    public void Draw(CPUTRenderParameters renderParams, ID3D11InputLayout pInputLayout )
    {
        Draw(renderParams, null, pInputLayout);
    }

    public void  Draw(CPUTRenderParameters renderParams, CPUTModel pModel)       { Draw(renderParams, pModel, mpInputLayout);}
    public void  DrawShadow(CPUTRenderParameters renderParams, CPUTModel pModel) { Draw(renderParams, pModel, mpShadowInputLayout);}
    public void  DrawAVSMShadowed(CPUTRenderParameters renderParams, CPUTModel pModel) // { Draw(renderParams, pModel, mpShadowInputLayout);}
    {
        Draw(renderParams, pModel, mpShadowInputLayout);
    }

    public void Draw(CPUTRenderParameters renderParams, CPUTModel pModel, ID3D11InputLayout pLayout){
        // Skip empty meshes.
        if( mIndexCount==0 ) { return; }

// TODO: Modify CPUTPerfTaskMarker so that calls compile out, instead of explicitly wrapping every call with ifdef CPUT_GPA_INSTRUMENTATION
/*#ifdef CPUT_GPA_INSTRUMENTATION
        CPUTPerfTaskMarker marker = CPUTPerfTaskMarker(D3DCOLOR(0xff0000), _L("CPUT Draw Mesh"));
#endif
        ID3D11DeviceContext *pContext = ((CPUTRenderParametersDX*)&renderParams)->mpContext;*/

        /*pContext->IASetPrimitiveTopology( mD3DMeshTopology );
        pContext->IASetVertexBuffers(0, 1, &mpVertexBuffer, &mVertexStride, &mVertexBufferOffset);
        pContext->IASetIndexBuffer(mpIndexBuffer, mIndexBufferFormat, 0);
        pContext->IASetInputLayout( pInputLayout );
        pContext->DrawIndexed( mIndexCount, 0, 0 );*/

        GLCheck.checkError();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, mpVertexBuffer.getBuffer());
        pLayout.bind();
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, mpIndexBuffer.getBuffer());

        gl.glDrawElements(mD3DMeshTopology, mIndexCount, mIndexBufferFormat, 0);

        pLayout.unbind();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLCheck.checkError();

//        saveData(-1,-1);
    }

    /*private static int gDrawIndex;
    private static void saveData(int vertexSize, int indexCount){
        gDrawIndex++;
        System.out.println("DrawIndex = " + gDrawIndex);
    }*/

    public int                      GetTriangleCount() { return mIndexCount/3; }
    public int                      GetVertexCount() { return mVertexCount; }
    public int                      GetIndexCount()  { return mIndexCount; }

    // Mapping vertex and index buffers is very similar.  This internal function does both
    /*private ByteBuffer Map(
            UINT                   count,
            ID3D11Buffer          *pBuffer,
            D3D11_BUFFER_DESC     &bufferDesc,
            ID3D11Buffer         **pStagingBuffer,
            eCPUTMapType          *pMappedType,
            CPUTRenderParameters  &params,
            eCPUTMapType           type,
            bool                   wait = true
    );
    private void  Unmap(
            ID3D11Buffer         *pBuffer,
            ID3D11Buffer         *pStagingBuffer,
            eCPUTMapType         *pMappedType,
            CPUTRenderParameters &params
    );*/
    private void ClearAllObjects() // delete all allocations held by this object
    {
        SAFE_RELEASE(mpStagingIndexBuffer);
        SAFE_RELEASE(mpIndexBuffer);
        SAFE_RELEASE(mpStagingVertexBuffer);
        SAFE_RELEASE(mpVertexBuffer);
        SAFE_RELEASE(mpVertexBufferForSRVDX);
        SAFE_RELEASE(mpVertexBufferForSRV);
        SAFE_RELEASE(mpVertexView);
        /*SAFE_RELEASE(mpInputLayout);
        SAFE_RELEASE(mpShadowInputLayout);*/

        /*SAFE_DELETE_ARRAY(mpLayoutDescription);*/
    }

    // Translate an internal CPUT data type into it's equivalent DirectX(OpenGL) type
    static int ConvertToDirectXFormat(int dataFormatType, int componentCount){
        assert ( componentCount>0 && componentCount<=4) : "Invalid vertex element count.";
        switch( dataFormatType )
        {
            case CPUT_F32:
            {
                /*const DXGI_FORMAT componentCountToFormat[4] = {
                        DXGI_FORMAT_R32_FLOAT,
                        DXGI_FORMAT_R32G32_FLOAT,
                        DXGI_FORMAT_R32G32B32_FLOAT,
                        DXGI_FORMAT_R32G32B32A32_FLOAT
                };*/
                final int[] componentCountToFormat = {
                    GLenum.GL_R32F,
                    GLenum.GL_RG32F,
                    GLenum.GL_RGB32F,
                    GLenum.GL_RGBA32F,
                };

                return componentCountToFormat[componentCount-1];
            }
            case CPUT_U32:
            {
                /*const DXGI_FORMAT componentCountToFormat[4] = {
                        DXGI_FORMAT_R32_UINT,
                        DXGI_FORMAT_R32G32_UINT,
                        DXGI_FORMAT_R32G32B32_UINT,
                        DXGI_FORMAT_R32G32B32A32_UINT
                };*/
                final int[] componentCountToFormat = {
                        GLenum.GL_R32UI,
                        GLenum.GL_RG32UI,
                        GLenum.GL_RGB32UI,
                        GLenum.GL_RGBA32UI,
                };
                return componentCountToFormat[componentCount-1];
            }
            case CPUT_U16:
            {
//                assert ( 3 != componentCount) : "Invalid vertex element count.";
                /*const DXGI_FORMAT componentCountToFormat[4] = {
                    DXGI_FORMAT_R16_UINT,
                        DXGI_FORMAT_R16G16_UINT,
                        DXGI_FORMAT_UNKNOWN, // Count of 3 is invalid for 16-bit type
                        DXGI_FORMAT_R16G16B16A16_UINT
                };*/
                final int[] componentCountToFormat = {
                        GLenum.GL_R16UI,
                        GLenum.GL_RG16UI,
                        GLenum.GL_RGB16UI,
                        GLenum.GL_RGBA16UI,
                };
                return componentCountToFormat[componentCount-1];
            }
            default:
            {
                // todo: add all the other data types you want to support
                throw new IllegalArgumentException("Unsupported vertex element type");
            }
//            return DXGI_FORMAT_UNKNOWN;
        }
    }

    @Override
    public void dispose() {
        ClearAllObjects();
    }
}
