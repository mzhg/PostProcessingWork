package jet.opengl.demos.intel.antialiasing;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by Administrator on 2017/9/24 0024.
 */
final class SAAPostProcessGPU {

    private int				m_ScreenWidth;
    private int				m_ScreenHeight;

    private int                m_ThreadGroupCountX;
    private int                m_ThreadGroupCountY;
    private int                m_DebugThreadGroupCountX;
    private int                m_DebugThreadGroupCountY;
    private GLSLProgram        m_pEdgeDetectionShader;
    private GLSLProgram        m_pEdgeDetectionDebugShader;
    private GLSLProgram        m_pHorizontalBlendingShader;
    private GLSLProgram        m_pVerticalBlendingShader;
    private BufferGL m_pConstantBuffer;
    private BufferGL				m_pEdgeDetectConstants;
    private BufferGL               m_pEdgeXBitArray;
    private BufferGL               m_pEdgeYBitArray;
    private BufferGL m_pEdgeXBitArrayUAV;
    private BufferGL  m_pEdgeYBitArrayUAV;
    private BufferGL   m_pEdgeXBitArraySRV;
    private BufferGL   m_pEdgeYBitArraySRV;
//    ID3D11SamplerState*         m_pSamplerState;
    private Texture2D            m_pWorkingColorTexture;
    private Texture2D   m_pWorkingColorTextureSRV;
    private int m_pSamplerState;
    private GLFuncProvider gl;
    private final CSConstants m_csconstants = new CSConstants();
    private final EdgeDetectConstants m_edge_constants = new EdgeDetectConstants();
    private boolean m_prinOnce = false;

    void OnCreate(/*ID3D11Device* pD3dDevice, ID3D11DeviceContext* pContext, IDXGISwapChain* pSwapChain*/){
        /*HRESULT hr = S_OK;
        CPUTResult result;
        cString FullPath, FinalPath;
        ID3DBlob *pPSBlob = NULL;
        CPUTAssetLibraryDX11 *pAssetLibrary = (CPUTAssetLibraryDX11*)CPUTAssetLibrary::GetAssetLibrary();
        cString OriginalAssetLibraryDirectory = pAssetLibrary->GetShaderDirectoryName();*/

        /*FullPath = OriginalAssetLibraryDirectory + L"SAAEdgeDetection.hlsl";
        CPUTFileSystem::ResolveAbsolutePathAndFilename(FullPath, &FinalPath);
        result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"CSMain", L"cs_5_0", &pPSBlob, NULL );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
        UNREFERENCED_PARAMETER(result);
        hr = pD3DDevice->CreateComputeShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_pEdgeDetectionShader);
        SAFE_RELEASE( pPSBlob );*/
        m_pEdgeDetectionShader = createProgram("EdgeDetection");
        // Compile & create compute shaders

        /*FullPath = OriginalAssetLibraryDirectory + L"SAAEdgeDetectionDebug.hlsl";
        CPUTFileSystem::ResolveAbsolutePathAndFilename(FullPath, &FinalPath);
        result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"CSMain", L"cs_5_0", &pPSBlob, NULL );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
        UNREFERENCED_PARAMETER(result);
        hr = pD3DDevice->CreateComputeShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_pEdgeDetectionDebugShader);
        SAFE_RELEASE( pPSBlob );*/
        m_pEdgeDetectionDebugShader = createProgram("EdgeDetectionDebug");


        /*FullPath = OriginalAssetLibraryDirectory + L"SAAHorizontalBlendingPass.hlsl";
        CPUTFileSystem::ResolveAbsolutePathAndFilename(FullPath, &FinalPath);
        result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"CSMain", L"cs_5_0", &pPSBlob, NULL );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
        UNREFERENCED_PARAMETER(result);
        hr = pD3DDevice->CreateComputeShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_pHorizontalBlendingShader);
        SAFE_RELEASE( pPSBlob );*/
        m_pHorizontalBlendingShader = createProgram("HorizontalBlendingPass");

        /*FullPath = OriginalAssetLibraryDirectory + L"SAAVerticalBlendingPass.hlsl";
        CPUTFileSystem::ResolveAbsolutePathAndFilename(FullPath, &FinalPath);
        result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"CSMain", L"cs_5_0", &pPSBlob, NULL );
        ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
        UNREFERENCED_PARAMETER(result);
        hr = pD3DDevice->CreateComputeShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_pVerticalBlendingShader);
        SAFE_RELEASE( pPSBlob );*/
        m_pVerticalBlendingShader = createProgram("VerticalBlendingPass");

        // Sampler state
        /*D3D11_SAMPLER_DESC SamplerDesc;
        ZeroMemory(&SamplerDesc, sizeof(SamplerDesc));
        SamplerDesc.Filter   = D3D11_FILTER_MIN_MAG_LINEAR_MIP_POINT;
        SamplerDesc.AddressU = D3D11_TEXTURE_ADDRESS_CLAMP;
        SamplerDesc.AddressV = D3D11_TEXTURE_ADDRESS_CLAMP;
        SamplerDesc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
        hr = (pD3DDevice->CreateSamplerState(&SamplerDesc, &m_pSamplerState));*/
        SamplerDesc samplerDesc = new SamplerDesc();
        samplerDesc.minFilter = GLenum.GL_LINEAR_MIPMAP_NEAREST;
        samplerDesc.magFilter = GLenum.GL_LINEAR;
        samplerDesc.wrapR = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.wrapS = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
        m_pSamplerState = SamplerUtils.createSampler(samplerDesc);

        // Create constant buffer
        /*D3D11_BUFFER_DESC BufferDesc;
        ZeroMemory(&BufferDesc, sizeof(BufferDesc));
        BufferDesc.Usage          = D3D11_USAGE_DYNAMIC;
        BufferDesc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
        BufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        BufferDesc.ByteWidth      = sizeof(CSConstants);
        hr = (pD3DDevice->CreateBuffer(&BufferDesc, NULL, &m_pConstantBuffer));*/
        m_pConstantBuffer = new BufferGL();
        m_pConstantBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, CSConstants.SIZE, null, GLenum.GL_DYNAMIC_COPY);
        m_pConstantBuffer.unbind();

        // Create edge detect constant buffer
        {
            /*D3D11_BUFFER_DESC BufferDesc;
            ZeroMemory(&BufferDesc, sizeof(BufferDesc));
            BufferDesc.Usage          = D3D11_USAGE_DYNAMIC;
            BufferDesc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
            BufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
            BufferDesc.ByteWidth      = sizeof(EdgeDetectConstants);
            hr = ( pD3DDevice->CreateBuffer( & BufferDesc, NULL, &m_pEdgeDetectConstants ) );*/
            m_pEdgeDetectConstants = new BufferGL();
            m_pEdgeDetectConstants.initlize(GLenum.GL_UNIFORM_BUFFER, EdgeDetectConstants.SIZE, null, GLenum.GL_DYNAMIC_COPY);
            m_pEdgeDetectConstants.unbind();
        }
    }

    void OnSize(/*ID3D11Device* pD3DDevice,*/ int width, int height){
        m_ScreenHeight = height;
        m_ScreenWidth = width;

        CommonUtil.safeRelease(m_pEdgeXBitArray);
        CommonUtil.safeRelease(m_pEdgeYBitArray);
        CommonUtil.safeRelease(m_pEdgeXBitArrayUAV);
        CommonUtil.safeRelease(m_pEdgeYBitArrayUAV);
        CommonUtil.safeRelease(m_pEdgeXBitArraySRV);
        CommonUtil.safeRelease(m_pEdgeYBitArraySRV);

        CommonUtil.safeRelease(m_pWorkingColorTexture);
        CommonUtil.safeRelease(m_pWorkingColorTextureSRV);

        /*D3D11_BUFFER_DESC DescBuffer;
        ZeroMemory(&DescBuffer, sizeof(D3D11_BUFFER_DESC));
        DescBuffer.BindFlags           = D3D11_BIND_UNORDERED_ACCESS | D3D11_BIND_SHADER_RESOURCE;
        DescBuffer.ByteWidth           = ((UINT) ceil(m_ScreenWidth/32.f)) * (UINT)m_ScreenHeight * sizeof(float);
        DescBuffer.MiscFlags           = D3D11_RESOURCE_MISC_BUFFER_ALLOW_RAW_VIEWS;
        DescBuffer.StructureByteStride = 0;
        DescBuffer.Usage               = D3D11_USAGE_DEFAULT;
        hr =(pD3DDevice->CreateBuffer(&DescBuffer, NULL, &m_pEdgeXBitArray));
        hr =(pD3DDevice->CreateBuffer(&DescBuffer, NULL, &m_pEdgeYBitArray));*/
        m_pEdgeXBitArray = new BufferGL();
        m_pEdgeXBitArray.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, ((int) Math.ceil(m_ScreenWidth/32.f)) * m_ScreenHeight * 4, null, GLenum.GL_DYNAMIC_COPY);
        m_pEdgeYBitArray = new BufferGL();
        m_pEdgeYBitArray.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, ((int) Math.ceil(m_ScreenWidth/32.f)) * m_ScreenHeight * 4, null, GLenum.GL_DYNAMIC_COPY);

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC DescUAV;
        ZeroMemory(&DescUAV, sizeof(D3D11_UNORDERED_ACCESS_VIEW_DESC));
        DescUAV.Format              = DXGI_FORMAT_R32_TYPELESS;
        DescUAV.ViewDimension       = D3D11_UAV_DIMENSION_BUFFER;
        DescUAV.Buffer.FirstElement = 0;
        DescUAV.Buffer.NumElements  = ((UINT) ceil(m_ScreenWidth/32.f)) * (UINT)m_ScreenHeight;
        DescUAV.Buffer.Flags        = D3D11_BUFFER_UAV_FLAG_RAW;
        hr =(pD3DDevice->CreateUnorderedAccessView(m_pEdgeXBitArray, &DescUAV, &m_pEdgeXBitArrayUAV));
        hr =(pD3DDevice->CreateUnorderedAccessView(m_pEdgeYBitArray, &DescUAV, &m_pEdgeYBitArrayUAV));*/
        m_pEdgeXBitArrayUAV = m_pEdgeXBitArray;
        m_pEdgeYBitArrayUAV = m_pEdgeYBitArray;

        /*D3D11_SHADER_RESOURCE_VIEW_DESC DescSRV;
        ZeroMemory(&DescSRV, sizeof(D3D11_SHADER_RESOURCE_VIEW_DESC));
        DescSRV.Format                = DXGI_FORMAT_R32_TYPELESS;
        DescSRV.ViewDimension         = D3D11_SRV_DIMENSION_BUFFEREX;
        DescSRV.BufferEx.FirstElement = 0;
        DescSRV.BufferEx.Flags        = D3D11_BUFFEREX_SRV_FLAG_RAW;
        DescSRV.BufferEx.NumElements  = ((UINT) ceil(m_ScreenWidth/32.f)) * (UINT)m_ScreenHeight;
        hr =(pD3DDevice->CreateShaderResourceView(m_pEdgeXBitArray, &DescSRV, &m_pEdgeXBitArraySRV));
        hr =(pD3DDevice->CreateShaderResourceView(m_pEdgeYBitArray, &DescSRV, &m_pEdgeYBitArraySRV));*/
        m_pEdgeXBitArraySRV = m_pEdgeXBitArray;
        m_pEdgeYBitArraySRV = m_pEdgeYBitArray;

        /*ZeroMemory(&DescSRV, sizeof(D3D11_SHADER_RESOURCE_VIEW_DESC));
        DescSRV.Format                    = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
        DescSRV.ViewDimension             = D3D11_SRV_DIMENSION_TEXTURE2D;
        DescSRV.Texture2D.MipLevels       = 1;
        DescSRV.Texture2D.MostDetailedMip = 0;*/

        // Update constant buffer with new dimensions
        m_ThreadGroupCountX = (int) Math.ceil(m_ScreenWidth  / 32.f);
        m_ThreadGroupCountY = (int) Math.ceil(m_ScreenHeight / 32.f);

        m_DebugThreadGroupCountX = m_ScreenWidth;
        m_DebugThreadGroupCountY = m_ScreenHeight;

        /*DXGI_SAMPLE_DESC SampleDesc;
        SampleDesc.Count = 1;
        SampleDesc.Quality = 0;
        // Create working texture
        D3D11_TEXTURE2D_DESC td;
        //    D3D11_RENDER_TARGET_VIEW_DESC rtvd;
        D3D11_SHADER_RESOURCE_VIEW_DESC srvd;
        memset(&td, 0, sizeof(td));
        td.ArraySize = 1;
        td.Format = DXGI_FORMAT_R8G8B8A8_TYPELESS;// m_backBufferSurfaceDesc.Format;
        td.Height = (UINT)m_ScreenHeight;
        td.Width = (UINT)m_ScreenWidth;
        td.CPUAccessFlags = 0;
        td.MipLevels = 1;
        td.MiscFlags = 0;
        td.SampleDesc = SampleDesc;
        td.Usage = D3D11_USAGE_DEFAULT;
        assert( td.SampleDesc.Count == 1 ); // other not supported yet
        td.BindFlags = D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE;
        hr = ( pD3DDevice->CreateTexture2D( &td, NULL, &m_pWorkingColorTexture ) );*/
        m_pWorkingColorTexture = TextureUtils.createTexture2D(new Texture2DDesc(m_ScreenWidth, m_ScreenHeight, GLenum.GL_RGBA8), null);

        /*memset(&srvd, 0, sizeof(srvd));
        srvd.Format = DXGI_FORMAT_R8G8B8A8_UNORM; //m_backBufferSurfaceDesc.Format;
        srvd.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
        srvd.Texture2D.MostDetailedMip = 0;
        srvd.Texture2D.MipLevels = 1;
        hr = ( pD3DDevice->CreateShaderResourceView( m_pWorkingColorTexture, &srvd, &m_pWorkingColorTextureSRV ) );*/
        m_pWorkingColorTextureSRV = m_pWorkingColorTexture;
    }

    void    OnShutdown(){
        CommonUtil.safeRelease(m_pEdgeDetectionShader);
        CommonUtil.safeRelease(m_pEdgeDetectionDebugShader);
        CommonUtil.safeRelease(m_pHorizontalBlendingShader);
        CommonUtil.safeRelease(m_pVerticalBlendingShader);
        CommonUtil.safeRelease(m_pConstantBuffer);
//        CommonUtil.safeRelease(m_pSamplerState);

        CommonUtil.safeRelease( m_pEdgeDetectConstants );

        CommonUtil.safeRelease(m_pEdgeXBitArray);
        CommonUtil.safeRelease(m_pEdgeYBitArray);
        CommonUtil.safeRelease(m_pEdgeXBitArrayUAV);
        CommonUtil.safeRelease(m_pEdgeYBitArrayUAV);
        CommonUtil.safeRelease(m_pEdgeXBitArraySRV);
        CommonUtil.safeRelease(m_pEdgeYBitArraySRV);

        CommonUtil.safeRelease(m_pWorkingColorTexture);
        CommonUtil.safeRelease(m_pWorkingColorTextureSRV);
    }

    void    Draw(/*ID3D11DeviceContext* pD3DImmediateContext,*/ float PPAADEMO_gEdgeDetectionThreshold,
                 Texture2D sourceColorSRV, Texture2D destColorUAV, boolean showEdges ){
        // Backup current render/stencil
        /*ID3D11RenderTargetView * oldRenderTargetViews[4] = { NULL };
        ID3D11DepthStencilView * oldDepthStencilView = NULL;
        pContext->OMGetRenderTargets( _countof(oldRenderTargetViews), oldRenderTargetViews, &oldDepthStencilView );*/
        if(gl == null){
            gl = GLFuncProviderFactory.getGLFuncProvider();
        }

        // Copy contents of the color buffer to the working texture
        /*ID3D11Resource*         pRT = NULL;
        //DXUTGetD3D11RenderTargetView()->GetResource( &sourceColorSRV );
        sourceColorSRV->GetResource( &pRT );
        pContext->CopyResource( m_pWorkingColorTexture, pRT );
        pRT->Release();*/
        gl.glCopyImageSubData(sourceColorSRV.getTexture(), sourceColorSRV.getTarget(), 0,0,0,0,
                m_pWorkingColorTexture.getTexture(), m_pWorkingColorTexture.getTarget(), 0,0,0,0,
                sourceColorSRV.getWidth(), sourceColorSRV.getHeight(), 1);

        // Need to clear this as we write it using InterlockedAdd...
        /*UINT Zeroes[4] = { 0, 0, 0, 0 };
        pContext->ClearUnorderedAccessViewUint(m_pEdgeXBitArrayUAV, Zeroes);*/
        m_pEdgeXBitArrayUAV.bind();
        gl.glClearBufferData(m_pEdgeXBitArrayUAV.getTarget(), GLenum.GL_R32F, GLenum.GL_RED, GLenum.GL_FLOAT, null);

        // Unbind RT from output so we can use it as input for the edge detection shader
//        ID3D11RenderTargetView* NullRT = NULL;
//        pContext->OMSetRenderTargets(1, &NullRT, CPUTRenderTargetDepth::GetActiveDepthStencilView());

        // update consts
        {
            /*HRESULT hr;
            D3D11_MAPPED_SUBRESOURCE MappedResource;
            hr =( pContext->Map(m_pConstantBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource) );
            CSConstants *pCB = (CSConstants *) MappedResource.pData;*/
            CSConstants pCB = m_csconstants;
            pCB.param[0] = m_ThreadGroupCountX;
            pCB.param[1] = m_ThreadGroupCountY;
            pCB.pXY[0]   = 1.f / m_ScreenWidth;
            pCB.pXY[1]   = 1.f / m_ScreenHeight;
            pCB.PPAADEMO_gEdgeDetectionThreshold = PPAADEMO_gEdgeDetectionThreshold;
//            pContext->Unmap(m_pConstantBuffer, 0);
            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(CSConstants.SIZE);
            pCB.store(bytes).flip();
            m_pConstantBuffer.update(0, bytes);
        }

        {
            /*HRESULT hr;
            D3D11_MAPPED_SUBRESOURCE MappedResource;
            hr =( pContext->Map( m_pEdgeDetectConstants, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource ) );
            EdgeDetectConstants* pConsts = ( EdgeDetectConstants* )MappedResource.pData;*/
            EdgeDetectConstants pConsts = m_edge_constants;
            pConsts.LumWeights[0]           = 0.2126f;
            pConsts.LumWeights[1]           = 0.7152f;
            pConsts.LumWeights[2]           = 0.0722f;
            pConsts.LumWeights[3]           = 0.0000f;

            pConsts.ColorThresholdSimple    = PPAADEMO_gEdgeDetectionThreshold;
            pConsts.ColorThresholdMin       = 0.0f;
            pConsts.ColorThresholdMax       = 0.0f;
            pConsts.DepthThreshold          = 0.07f;

//            pContext->Unmap( m_pEdgeDetectConstants, 0 );
            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(EdgeDetectConstants.SIZE);
            pConsts.store(bytes).flip();
            m_pEdgeDetectConstants.update(0, bytes);
//            pContext->CSSetConstantBuffers( 7, 1, &m_pEdgeDetectConstants );
            gl.glBindBufferBase(m_pEdgeDetectConstants.getTarget(), 1, m_pEdgeDetectConstants.getBuffer());
        }

        // First run edge detection shader
        /*ID3D11UnorderedAccessView* EdgeBitArrayUAVs[2] = { m_pEdgeXBitArrayUAV, m_pEdgeYBitArrayUAV };*/
        gl.glBindBufferBase(m_pEdgeXBitArrayUAV.getTarget(), 2, m_pEdgeXBitArrayUAV.getBuffer());
        gl.glBindBufferBase(m_pEdgeYBitArrayUAV.getTarget(), 3, m_pEdgeYBitArrayUAV.getBuffer());
        /*pContext->CSSetShader(m_pEdgeDetectionShader, NULL, 0);*/
        m_pEdgeDetectionShader.enable();
        /*pContext->CSSetShaderResources(0, 1, &m_pWorkingColorTextureSRV);*/
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_pWorkingColorTextureSRV.getTarget(), m_pWorkingColorTextureSRV.getTexture());
        /*pContext->CSSetConstantBuffers(0, 1, &m_pConstantBuffer);*/
        gl.glBindBufferBase(m_pConstantBuffer.getTarget(), 0, m_pConstantBuffer.getBuffer());
//        pContext->CSSetUnorderedAccessViews(0, 2, EdgeBitArrayUAVs, NULL);
        /*pContext->Dispatch(m_ThreadGroupCountX, m_ThreadGroupCountY, 1);*/
        gl.glDispatchCompute(m_ThreadGroupCountX, m_ThreadGroupCountY, 1);

        // Unbind the edge bit arrays from CS output so we can bind them as inputs of next passes
        /*ID3D11UnorderedAccessView* NullUAVs[2] = { NULL, NULL };
        pContext->CSSetUnorderedAccessViews(0, 2, NullUAVs, NULL);*/
        if(showEdges)
        {
            /*ID3D11ShaderResourceView* SRVs[2] = { m_pEdgeXBitArraySRV, m_pEdgeYBitArraySRV };
            pContext->CSSetShader(m_pEdgeDetectionDebugShader, NULL, 0);
            pContext->CSSetShaderResources(0, 2, SRVs);
            pContext->CSSetUnorderedAccessViews(0, 1, &destColorUAV, NULL);
            pContext->Dispatch(m_DebugThreadGroupCountX, m_DebugThreadGroupCountY, 1);*/
            m_pEdgeDetectionDebugShader.enable();
            gl.glBindImageTexture(0, destColorUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, destColorUAV.getFormat());
            gl.glDispatchCompute(m_DebugThreadGroupCountX, m_DebugThreadGroupCountY, 1);
            if(!m_prinOnce){
                m_pEdgeDetectionDebugShader.printPrograminfo();
            }
        }
        else
        {
            // Horizontal pass
            /*ID3D11ShaderResourceView* SRVs[2] = { m_pEdgeXBitArraySRV, m_pWorkingColorTextureSRV };
            pContext->CSSetShader(m_pHorizontalBlendingShader, NULL, 0);
            pContext->CSSetShaderResources(0, 2, SRVs);
            pContext->CSSetSamplers(0, 1, &m_pSamplerState);
            pContext->CSSetUnorderedAccessViews(0, 1, &destColorUAV, NULL);
            pContext->Dispatch(m_ThreadGroupCountX, m_ThreadGroupCountY, 1);*/
            m_pHorizontalBlendingShader.enable();
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_pWorkingColorTextureSRV.getTarget(), m_pWorkingColorTextureSRV.getTexture());
            gl.glBindImageTexture(0, destColorUAV.getTexture(), 0, false, 0, GLenum.GL_WRITE_ONLY, destColorUAV.getFormat());
            gl.glDispatchCompute(m_ThreadGroupCountX, m_ThreadGroupCountY, 1);
            if(!m_prinOnce){
                m_pHorizontalBlendingShader.printPrograminfo();
            }

            // Vertical pass
            /*SRVs[0] = m_pEdgeYBitArraySRV;
            pContext->CSSetShader(m_pVerticalBlendingShader, NULL, 0);
            pContext->CSSetShaderResources(0, 2, SRVs);
            pContext->Dispatch(m_ThreadGroupCountY, m_ThreadGroupCountX, 1);*/
            m_pVerticalBlendingShader.enable();
            gl.glDispatchCompute(m_ThreadGroupCountX, m_ThreadGroupCountY, 1);
            if(!m_prinOnce){
                m_pVerticalBlendingShader.printPrograminfo();
            }
        }

        /*pContext->CSSetUnorderedAccessViews(0, 2, NullUAVs, NULL);
        ID3D11ShaderResourceView* NullSRVs[2] = { NULL, NULL };
        pContext->CSSetShaderResources(0, 2, NullSRVs);
        ID3D11SamplerState* NullSamplerState = { NULL };
        pContext->CSSetSamplers(0, 1, &NullSamplerState);

        // Restore old stencil/depth
        pContext->OMSetRenderTargets( _countof(oldRenderTargetViews), oldRenderTargetViews, oldDepthStencilView );
        for( int i = 0; i < _countof(oldRenderTargetViews); i++ )
            SAFE_RELEASE( oldRenderTargetViews[i] );
        SAFE_RELEASE( oldDepthStencilView );*/
        gl.glBindImageTexture(0, 0, 0, false, 0, GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 1, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 2, 0);
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, 3, 0);
        m_prinOnce = true;
    }

    private static GLSLProgram createProgram(String computeShaderName, Macro... macros){
        GLSLProgram program = GLSLProgram.createProgram(String.format("shader_libs/AntiAliasing/PostProcessingSAA_%s.comp", computeShaderName), macros);
        program.setName(computeShaderName);
        return program;
    }

    static class CSConstants implements Readable
    {
        static final int SIZE = Vector4f.SIZE * 2;
        final int[]  param = new int[2];
        final float[] pXY = new float[2];

        float PPAADEMO_gEdgeDetectionThreshold;
        //        float dummy[3];

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            CacheBuffer.put(buf, param);
            CacheBuffer.put(buf, pXY);
            buf.putFloat(PPAADEMO_gEdgeDetectionThreshold);
            buf.putInt(0);
            buf.putInt(0);
            buf.putInt(0);
            return buf;
        }
    }

    static class EdgeDetectConstants implements Readable
    {
        static final int SIZE = 8 * 4;
        /** .rgb - luminance weight for each colour channel; .w unused for now (maybe will be used for gamma correction before edge detect) */
        final float[]    LumWeights = new float[4];
        /** one threshold only, when not using depth/normal info */
        float    ColorThresholdSimple;
        /** if above, edge if depth/normal is edge too */
        float    ColorThresholdMin;
        /** if above, always edge */
        float    ColorThresholdMax;
        /** if between ColorThresholdMin/ColorThresholdMax */
        float    DepthThreshold;

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            CacheBuffer.put(buf, LumWeights);
            buf.putFloat(ColorThresholdSimple);
            buf.putFloat(ColorThresholdMin);
            buf.putFloat(ColorThresholdMax);
            buf.putFloat(DepthThreshold);
            return buf;
        }
    }
}
