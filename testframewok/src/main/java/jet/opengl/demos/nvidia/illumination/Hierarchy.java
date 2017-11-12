package jet.opengl.demos.nvidia.illumination;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by Administrator on 2017/11/12 0012.
 */
class Hierarchy implements Disposeable, Defines{
    private GLSLProgram m_pCSDownsampleMax;
    private GLSLProgram m_pCSDownsampleMin;
    private GLSLProgram m_pCSDownsampleAverage;
    private GLSLProgram m_pCSDownsampleAverageInplace;

    private GLSLProgram m_pCSUpsample;
    private GLSLProgram m_pCSUpsampleBilinear;
    private GLSLProgram m_pCSUpsampleBilinearAccumulate;
    private GLSLProgram m_pCSUpsampleBilinearAccumulateInplace;

    private BufferGL m_pcb;
    GLFuncProvider gl;
    final CB_HIERARCHY m_hierarchy = new CB_HIERARCHY();

    Hierarchy(/*ID3D11Device* pd3dDevice*/)
    {
        gl = GLFuncProviderFactory.getGLFuncProvider();
        /*HRESULT hr;
        ID3DBlob* pBlob = NULL;*/

        /*m_pCSDownsampleMax = NULL;  TODO
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "DownsampleMax", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSDownsampleMax ) );
        SAFE_RELEASE( pBlob );

        m_pCSDownsampleMin = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "DownsampleMin", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSDownsampleMin ) );
        SAFE_RELEASE( pBlob );

        m_pCSDownsampleAverage = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "DownsampleAverage", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSDownsampleAverage ) );
        SAFE_RELEASE( pBlob );

        m_pCSDownsampleAverageInplace = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "DownsampleAverageInplace", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSDownsampleAverageInplace ) );
        SAFE_RELEASE( pBlob );

        m_pCSUpsample = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "Upsample", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSUpsample ) );
        SAFE_RELEASE( pBlob );

        m_pCSUpsampleBilinear = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "UpsampleBilinear", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSUpsampleBilinear ) );
        SAFE_RELEASE( pBlob );

        m_pCSUpsampleBilinearAccumulate = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "UpsampleBilinearAccumulate", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSUpsampleBilinearAccumulate ) );
        SAFE_RELEASE( pBlob );


        m_pCSUpsampleBilinearAccumulateInplace = NULL;
        V( CompileShaderFromFile( L"Hierarchy.hlsl", "UpsampleBilinearAccumulateInplace", "cs_5_0", &pBlob ) );
        V( pd3dDevice->CreateComputeShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(), NULL, &m_pCSUpsampleBilinearAccumulateInplace ) );
        SAFE_RELEASE( pBlob );*/



        // setup constant buffer
        /*D3D11_BUFFER_DESC Desc;
        Desc.Usage = D3D11_USAGE_DYNAMIC;
        Desc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        Desc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        Desc.MiscFlags = 0;
        Desc.ByteWidth = sizeof( CB_HIERARCHY );
        V( pd3dDevice->CreateBuffer( &Desc, NULL, &m_pcb ) );*/
        m_pcb = new BufferGL();
        m_pcb.initlize(GLenum.GL_UNIFORM_BUFFER, CB_HIERARCHY.SIZE, null, GLenum.GL_STREAM_DRAW);
        m_pcb.unbind();
    }

    //go from a finer (higherRes) to a coarser (lowerRes) grid by taking the max of a 2x2x2 (in 3D) or 2x2 (in 2D) region.
    void Downsample(/*ID3D11DeviceContext* pd3dContext,*/ SimpleRT finer, SimpleRT coarser, int op, int RTindex0, int RTindex1 /*= -1*/, int RTindex2 /*= -1*/, int RTindex3 /*= -1*/)
    {
//        float ClearColor[4] = {0.0f, 0.0f, 0.0f, 0.0f};
        for(int i=0;i<coarser.getNumRTs();i++) {
//            pd3dContext.ClearRenderTargetView(coarser -> get_pRTV(i), ClearColor);
            TextureGL rtv = coarser.get_pRTV(i);
            gl.glClearTexImage(rtv.getTexture(), 0, TextureUtils.measureFormat(rtv.getFormat()), TextureUtils.measureDataType(rtv.getFormat()), null);
        }

        assert( (RTindex1 == -1) || op==Defines.DOWNSAMPLE_AVERAGE ); //only have mutiple RT code written for the downsample average option. If you need more oprions you have to include them here

        if(op==Defines.DOWNSAMPLE_MAX)
//            pd3dContext->CSSetShader( m_pCSDownsampleMax, NULL, 0 );
            m_pCSDownsampleMax.enable();
        else if(op==Defines.DOWNSAMPLE_MIN)
//            pd3dContext->CSSetShader( m_pCSDownsampleMin, NULL, 0 );
            m_pCSDownsampleMin.enable();
        else if(op==Defines.DOWNSAMPLE_AVERAGE)
        {
            if(RTindex1 == -1)
//                pd3dContext->CSSetShader( m_pCSDownsampleAverage, NULL, 0 );
                m_pCSDownsampleAverage.enable();
            else
//                pd3dContext->CSSetShader( m_pCSDownsampleAverageInplace, NULL, 0 );
                m_pCSDownsampleAverageInplace.enable();
        }

        //set the unordered views that we are going to be writing to
        //set the finer RT as a texture to read from
        int initCounts = 0;

        if(RTindex1 == -1)
        {
            /*ID3D11UnorderedAccessView* ppUAV[1] = { coarser.get_pUAV(RTindex0) };  TODO
            pd3dContext->CSSetUnorderedAccessViews( 0, 1, ppUAV, &initCounts );
            ID3D11ShaderResourceView* ppSRV[1] = { finer.get_pSRV(RTindex0) };
            pd3dContext->CSSetShaderResources( 0, 1, ppSRV);*/
        }
        else
        {
            // TODO resources binding
            /*ID3D11UnorderedAccessView* ppUAV[4] = { coarser.get_pUAV(RTindex0), coarser.get_pUAV(RTindex1), coarser.get_pUAV(RTindex2), coarser.get_pUAV(RTindex3) };
            pd3dContext->CSSetUnorderedAccessViews( 0, 4, ppUAV, &initCounts );
            ID3D11ShaderResourceView* ppSRV[4] = { finer.get_pSRV(RTindex0), finer.get_pSRV(RTindex1), finer.get_pSRV(RTindex2), finer.get_pSRV(RTindex3) };
            pd3dContext->CSSetShaderResources( 0, 4, ppSRV);*/
        }

        //set the constant buffer
        /*D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( m_pcb, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_HIERARCHY* pcb = ( CB_HIERARCHY* )MappedResource.pData;*/
        CB_HIERARCHY pcb = m_hierarchy;
        pcb.finerLevelWidth = finer.getWidth3D();
        pcb.finerLevelHeight = finer.getHeight3D();
        pcb.finerLevelDepth = finer.getDepth3D();
        pcb.corserLevelWidth = coarser.getWidth3D();
        pcb.corserLevelHeight = coarser.getHeight3D();
        pcb.corserLevelDepth = coarser.getDepth3D();
        pcb.g_numColsFiner = finer.getNumCols();
        pcb.g_numRowsFiner = finer.getNumRows();
        pcb.g_numColsCoarser = coarser.getNumCols();
        pcb.g_numRowsCoarser = coarser.getNumRows();

        /*pd3dContext->Unmap( m_pcb, 0 );
        pd3dContext->CSSetConstantBuffers( 0, 1, &m_pcb );*/
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(CB_HIERARCHY.SIZE);
        pcb.store(bytes).flip();
        m_pcb.update(0, bytes);
        // TODO binding m_pcb

//        pd3dContext->Dispatch( g_LPVWIDTH/X_BLOCK_SIZE, g_LPVHEIGHT/Y_BLOCK_SIZE, g_LPVDEPTH/Z_BLOCK_SIZE );
        gl.glDispatchCompute( g_LPVWIDTH/X_BLOCK_SIZE, g_LPVHEIGHT/Y_BLOCK_SIZE, g_LPVDEPTH/Z_BLOCK_SIZE);


        if(RTindex1 == -1)
        {
            /*ID3D11UnorderedAccessView* ppUAVssNULL1[1] = { NULL };  TODO
            pd3dContext->CSSetUnorderedAccessViews( 0, 1, ppUAVssNULL1, &initCounts );
            ID3D11ShaderResourceView* ppSRVsNULL1[1] = { NULL };
            pd3dContext->CSSetShaderResources( 0, 1, ppSRVsNULL1);*/
        }
        else
        {
            /*ID3D11UnorderedAccessView* ppUAVssNULL4[4] = { NULL, NULL, NULL, NULL };  TODO
            pd3dContext->CSSetUnorderedAccessViews( 0, 4, ppUAVssNULL4, &initCounts );
            ID3D11ShaderResourceView* ppSRVsNULL4[4] = { NULL, NULL, NULL, NULL };
            pd3dContext->CSSetShaderResources( 0, 4, ppSRVsNULL4);*/
        }

    }

    //go from a coarser (lowerRes) to a finer (higherRes) grid by copying data from one cell to a 2x2x2 (in 3D) or 2x2 (in 2D) region.
    void Upsample(/*ID3D11DeviceContext* pd3dContext,*/ SimpleRT finer, SimpleRT coarser, int op, int sampleType,
                  TextureGL srvAccumulate, int RTindex)
    {
//        float ClearColor[4] = {0.0f, 0.0f, 0.0f, 0.0f};
        for(int i=0;i<finer.getNumRTs();i++) {
//            pd3dContext->ClearRenderTargetView( finer.get_pRTV(i), ClearColor );
            TextureGL rtv = finer.get_pRTV(i);
            gl.glClearTexImage(rtv.getTexture(), 0, TextureUtils.measureFormat(rtv.getFormat()), TextureUtils.measureDataType(rtv.getFormat()), null);
        }

        if(op==UPSAMPLE_DUPLICATE && sampleType == SAMPLE_REPLACE)
//            pd3dContext->CSSetShader( m_pCSUpsample, NULL, 0 );
            m_pCSUpsample.enable();
        else if(op==UPSAMPLE_BILINEAR && sampleType == SAMPLE_REPLACE)
//            pd3dContext->CSSetShader( m_pCSUpsampleBilinear, NULL, 0 );
            m_pCSUpsampleBilinear.enable();
        else if((op==UPSAMPLE_BILINEAR) && sampleType == SAMPLE_ACCUMULATE && srvAccumulate !=null)
//            pd3dContext->CSSetShader( m_pCSUpsampleBilinearAccumulate, NULL, 0 );
            m_pCSUpsampleBilinearAccumulate.enable();

        //set the unordered views that we are going to be writing to
        int initCounts = 0;
        /*ID3D11UnorderedAccessView* ppUAV[1] = { finer.get_pUAV(RTindex) };  TODO
        pd3dContext->CSSetUnorderedAccessViews( 0, 1, ppUAV, &initCounts );*/

        //set the coarser RT as a texture to read from
        if( sampleType == SAMPLE_REPLACE)
        {
            /*ID3D11ShaderResourceView* ppSRV[1] = { coarser.get_pSRV(RTindex) }; TODO
            pd3dContext->CSSetShaderResources( 0, 1, ppSRV);*/
        }
        else if(sampleType == SAMPLE_ACCUMULATE)
        {
            /*ID3D11ShaderResourceView* ppSRV[2] = {coarser.get_pSRV(RTindex), srvAccumulate }; TODO
            pd3dContext->CSSetShaderResources( 0, 2, ppSRV);*/
        }

        //set the constant buffer
        /*D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( m_pcb, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_HIERARCHY* pcb = ( CB_HIERARCHY* )MappedResource.pData;*/
        CB_HIERARCHY pcb = m_hierarchy;
        pcb.finerLevelWidth = finer.getWidth3D();
        pcb.finerLevelHeight = finer.getHeight3D();
        pcb.finerLevelDepth = finer.getDepth3D();
        pcb.corserLevelWidth = coarser.getWidth3D();
        pcb.corserLevelHeight = coarser.getHeight3D();
        pcb.corserLevelDepth = coarser.getDepth3D();
        pcb.g_numColsFiner = finer.getNumCols();
        pcb.g_numRowsFiner = finer.getNumRows();
        pcb.g_numColsCoarser = coarser.getNumCols();
        pcb.g_numRowsCoarser = coarser.getNumRows();

        /*pd3dContext->Unmap( m_pcb, 0 );
        pd3dContext->CSSetConstantBuffers( 0, 1, &m_pcb );*/
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(CB_HIERARCHY.SIZE);
        pcb.store(bytes).flip();
        m_pcb.update(0, bytes);
        // TODO binding m_pcb

//        pd3dContext->Dispatch( g_LPVWIDTH/X_BLOCK_SIZE, g_LPVHEIGHT/Y_BLOCK_SIZE, g_LPVDEPTH/Z_BLOCK_SIZE );
        gl.glDispatchCompute(g_LPVWIDTH/X_BLOCK_SIZE, g_LPVHEIGHT/Y_BLOCK_SIZE, g_LPVDEPTH/Z_BLOCK_SIZE );

        /*ID3D11UnorderedAccessView* ppUAVssNULL1[1] = { NULL };
        pd3dContext->CSSetUnorderedAccessViews( 0, 1, ppUAVssNULL1, &initCounts );
        ID3D11ShaderResourceView* ppSRVsNULL2[2] = { NULL, NULL };
        pd3dContext->CSSetShaderResources( 0, 2, ppSRVsNULL2);*/

    }

    //go from a coarser (lowerRes) to a finer (higherRes) grid by accumulating data from one cell to a 2x2x2 (in 3D) or 2x2 (in 2D) region.
    //This version is specialized for 4 float textures, if you want more you have to change this function and also UpsampleBilinearAccumulateInplace in Hierarchy.hlsl
    void UpsampleAccumulateInplace4(//ID3D11DeviceContext* pd3dContext,
                                            SimpleRT finer, int finerIndex0, int finerIndex1, int finerIndex2, int finerIndex3,
                                            SimpleRT coarser, int coarserIndex0, int coarserIndex1, int coarserIndex2, int coarserIndex3,
                                            int op, int sampleType)
    {
        assert(  sampleType == SAMPLE_ACCUMULATE && op==UPSAMPLE_BILINEAR );

        /*pd3dContext->CSSetShader( m_pCSUpsampleBilinearAccumulateInplace, NULL, 0 );*/
        m_pCSUpsampleBilinearAccumulateInplace.enable();

        //set the unordered views that we are going to be writing to
        /*UINT initCounts = 0;  TODO
        ID3D11UnorderedAccessView* ppUAV[4] = { finer.get_pUAV(finerIndex0), finer.get_pUAV(finerIndex1), finer.get_pUAV(finerIndex2), finer.get_pUAV(finerIndex3) };
        pd3dContext->CSSetUnorderedAccessViews( 1, 4, ppUAV, &initCounts );*/

        //set the coarser RT as a texture to read from  TODO
        /*ID3D11ShaderResourceView* ppSRV[4] = { coarser.get_pSRV(coarserIndex0), coarser.get_pSRV(coarserIndex1), coarser.get_pSRV(coarserIndex2), coarser.get_pSRV(coarserIndex3) };
        pd3dContext->CSSetShaderResources( 2, 4, ppSRV);*/

        //set the constant buffer
        /*D3D11_MAPPED_SUBRESOURCE MappedResource;
        pd3dContext->Map( m_pcb, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
        CB_HIERARCHY* pcb = ( CB_HIERARCHY* )MappedResource.pData;*/
        CB_HIERARCHY pcb = m_hierarchy;
        pcb.finerLevelWidth = finer.getWidth3D();
        pcb.finerLevelHeight = finer.getHeight3D();
        pcb.finerLevelDepth = finer.getDepth3D();
        pcb.corserLevelWidth = coarser.getWidth3D();
        pcb.corserLevelHeight = coarser.getHeight3D();
        pcb.corserLevelDepth = coarser.getDepth3D();
        pcb.g_numColsFiner = finer.getNumCols();
        pcb.g_numRowsFiner = finer.getNumRows();
        pcb.g_numColsCoarser = coarser.getNumCols();
        pcb.g_numRowsCoarser = coarser.getNumRows();

        /*pd3dContext->Unmap( m_pcb, 0 );
        pd3dContext->CSSetConstantBuffers( 0, 1, &m_pcb );*/
        ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(CB_HIERARCHY.SIZE);
        pcb.store(bytes).flip();
        m_pcb.update(0, bytes);
        // TODO binding m_pcb

        /*pd3dContext->Dispatch( g_LPVWIDTH/X_BLOCK_SIZE, g_LPVHEIGHT/Y_BLOCK_SIZE, g_LPVDEPTH/Z_BLOCK_SIZE );*/
        gl.glDispatchCompute(g_LPVWIDTH/X_BLOCK_SIZE, g_LPVHEIGHT/Y_BLOCK_SIZE, g_LPVDEPTH/Z_BLOCK_SIZE);

        /*ID3D11UnorderedAccessView* ppUAVssNULL4[4] = { NULL, NULL, NULL, NULL };
        pd3dContext->CSSetUnorderedAccessViews( 0, 4, ppUAVssNULL4, &initCounts );
        ID3D11ShaderResourceView* ppSRVsNULL4[4] = { NULL, NULL, NULL, NULL };
        pd3dContext->CSSetShaderResources( 0, 4, ppSRVsNULL4);*/
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(m_pCSDownsampleMax);
        SAFE_RELEASE(m_pCSDownsampleMin);
        SAFE_RELEASE(m_pCSDownsampleAverage);
        SAFE_RELEASE(m_pCSUpsample);
        SAFE_RELEASE(m_pCSDownsampleAverageInplace);
        SAFE_RELEASE(m_pCSUpsampleBilinear);
        SAFE_RELEASE(m_pCSUpsampleBilinearAccumulate);
        SAFE_RELEASE(m_pCSUpsampleBilinearAccumulateInplace);
        SAFE_RELEASE(m_pcb);
    }
}
