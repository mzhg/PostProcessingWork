package jet.opengl.demos.intel.oit;

import java.io.IOException;
import java.nio.IntBuffer;

import jet.opengl.demos.intel.cput.CPUTAssetLibrary;
import jet.opengl.demos.intel.cput.CPUTAssetLibraryDX11;
import jet.opengl.demos.intel.cput.CPUTBufferDX11;
import jet.opengl.demos.intel.cput.CPUTTextureDX11;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class AOITTechnique implements Disposeable{
    private static final int NUM_BLUR_LEVELS = 4;
    private static final int NUM_BLURRED_RESOURCES = 4;
    private static final int MAX_SHADER_VARIATIONS = 6;

    GLSLProgram[] m_pAOITSPResolvePS = new GLSLProgram[MAX_SHADER_VARIATIONS];
//    ID3D11ShaderReflection* m_pAOITSPResolvePSReflection[MAX_SHADER_VARIATIONS];
    GLSLProgram[] m_pAOITSPClearPS = new GLSLProgram[MAX_SHADER_VARIATIONS];

    GLSLProgram m_pDXResolvePS;
//    ID3D11ShaderReflection* m_pDXResolvePSReflection;

    BufferGL[]          mAOITSPColorData = new BufferGL[MAX_SHADER_VARIATIONS];
    BufferGL[]			mAOITSPDepthData = new BufferGL[MAX_SHADER_VARIATIONS];
    BufferGL			mFragmentListNodes;
    Texture2D           mFragmentListFirstNodeOffset;
    boolean             mATSPClearMaskInitialized;

    Texture2D           mATSPClearMask;

    Runnable mAOITCompositeBlendState;
    Runnable mAOITCompositeDepthStencilState;

    CPUTBufferDX11 mpFragmentListFirstNodeOffseBuffer;
    CPUTBufferDX11 mFragmentListNodesBuffer;
    CPUTBufferDX11 mpAOITSPDepthDataUAVBuffer;
    CPUTBufferDX11 mpAOITSPColorDataUAVBuffer;
    CPUTBufferDX11 mp8AOITSPDepthDataUAVBuffer;
    CPUTBufferDX11 mp8AOITSPColorDataUAVBuffer;
    CPUTBufferDX11 mpIntelExt;
    CPUTBufferDX11 mFragmentListConstants;
    CPUTBufferDX11 mpATSPClearMaskBuffer;
    BufferGL       m_pConstBuffer;		// Buffer constants (dimensions and miplevels) for compute shaders

    CPUTTextureDX11 mpClearMaskRT;

    int m_pPointSampler;

    Texture2D m_pSwapChainRTV;	// render target view retrieved at InitFrameRender
    Texture2D m_pDSView;			// depth stencil view retried at InitFrameRender
//    DXGI_SURFACE_DESC* m_pBackBufferSurfaceDesc;// back buffer surface desc of current render target

//    BufferGL m_pQuadVB;
//    ID3D11VertexShader* m_pFullScreenQuadVS;
    ID3D11InputLayout m_pFullScreenQuadLayout;

    int mLisTexNodeCount = 1 << 22;

    private GLFuncProvider gl;
    private RenderTargets fbo;

    private final TextureAttachDesc colorDesc = new TextureAttachDesc();
    private final TextureAttachDesc depthDesc = new TextureAttachDesc();

    private final Texture2DDesc m_pBackBufferSurfaceDesc = new Texture2DDesc();

    AOITTechnique(){

    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mpClearMaskRT);
    }

    /*
            InitFrameRender
            This method should be called prior to any rendering (or clearing). This implementation discards anything
            prior to the InitFrameRender invocation. Stores a pointer to the current backbuffer that is used in FinalizeFrameRender
        */
    void InitFrameRender( RenderType selectedItem, int NodeIndex, boolean debugViewActive){
//        HRESULT hr = S_OK;
//        pD3DImmediateContext->OMGetRenderTargets(1, &(m_pSwapChainRTV), &m_pDSView);

        if((RenderType.ROV_OIT == selectedItem ) || (RenderType.ROV_HDR_OIT == selectedItem))
        {
            // Clearing the whole AOIT data structure can incur in performance degradation.
            // We clear a small control surface/clear mask instead, which will let us know if a given
            // pixels needs to be initialized at transparent fragments insertion time.
            // We only clear this ClearMask whole once, and then clear it only on modified pixels (masked by stencil)
            if( !mATSPClearMaskInitialized || debugViewActive )
            {
                Clear( /*pD3DImmediateContext,*/ null, NodeIndex );
                mATSPClearMaskInitialized = true;
            }
        }

        if ((RenderType.ROV_OIT == selectedItem) || (RenderType.ROV_HDR_OIT == selectedItem))
        {
//            D3D11_RENDER_TARGET_VIEW_DESC desc;
//            m_pSwapChainRTV->GetDesc( &desc );

            // Set output UAVs
            Texture2D[] pUAVs = new Texture2D[3];
            switch(NodeIndex)
            {
                case 0:
                    pUAVs[0] = (Texture2D) mpClearMaskRT.GetColorUAV();
                    pUAVs[1] = null;
                    pUAVs[2] = mpAOITSPColorDataUAVBuffer.GetUnorderedAccessView();
                    break;
                case 1:
                    pUAVs[0] = (Texture2D) mpClearMaskRT.GetColorUAV();
                    pUAVs[1] = mpAOITSPDepthDataUAVBuffer.GetUnorderedAccessView();
                    pUAVs[2] = mpAOITSPColorDataUAVBuffer.GetUnorderedAccessView();
                    break;
                case 2:
                    pUAVs[0] = (Texture2D) mpClearMaskRT.GetColorUAV();
                    pUAVs[1] = mp8AOITSPDepthDataUAVBuffer.GetUnorderedAccessView();
                    pUAVs[2] = mp8AOITSPColorDataUAVBuffer.GetUnorderedAccessView();
                    break;
            }

//            pD3DImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews( 0, NULL, m_pDSView, 1, 3, pUAVs, NULL ); // <- this disables MSAA  TODO

        }
        else if(RenderType.DX11_AOIT == selectedItem )
        {
            boolean resetUAVCounter = true;

            Texture2D fragmentListFirstNodeOffsetUAV =  mFragmentListFirstNodeOffset/*.m_ppUAV[0]*/;

            // Initialize the first node offset RW UAV with a NULL offset (end of the list)
            /*UINT clearValuesFirstNode[4] = {
            0x0UL,
                    0x0UL,
                    0x0UL,
                    0x0UL
            };*/

//            pD3DImmediateContext->ClearUnorderedAccessViewUint(fragmentListFirstNodeOffsetUAV, clearValuesFirstNode);
            gl.glClearTexImage(fragmentListFirstNodeOffsetUAV.getTexture(), 0, TextureUtils.measureFormat(fragmentListFirstNodeOffsetUAV.getFormat()),
                    TextureUtils.measureDataType(fragmentListFirstNodeOffsetUAV.getFormat()), null);
            FillFragmentListConstants( mLisTexNodeCount * 2);

            /*ID3D11UnorderedAccessView* pUAVs[] = { mFragmentListFirstNodeOffset.m_ppUAV[0], mFragmentListNodes.m_pUAV  };  TODO
            UINT pUAVInitialCounts[2] = {1, 1};
            pD3DImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews(0, NULL, m_pDSView,0, 2, pUAVs, resetUAVCounter ? pUAVInitialCounts : NULL);*/
        }
    }

    /*
        FinalizeFrameRender
        Finalize the the frame, performs the tonemapping and bloom calculations and copies the results into
        the original backbuffer.
    */
    void FinalizeFrameRender(RenderType selectedItem, boolean doResolve,int NodeIndex, boolean debugViewActive){
//        HRESULT hr = S_OK;
//        D3D11_VIEWPORT viewport;
//        UINT numViewPorts = 1;
//        pD3DImmediateContext->RSGetViewports(&numViewPorts, &viewport);

        IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);
        int viewportWidth = viewport.get(2);
        int viewportHeight = viewport.get(3);


        int width = 512;
        int height = 512;

        /*D3D11_VIEWPORT quadViewPort;
        quadViewPort.Height = (float) height;
        quadViewPort.Width = (float)width;
        quadViewPort.MaxDepth = 1.0f;
        quadViewPort.MinDepth = 0.0f;
        quadViewPort.TopLeftX = 0;
        quadViewPort.TopLeftY = 0;*/

        if(doResolve)
        {
//            pD3DImmediateContext->PSSetSamplers(0, 1, &m_pPointSampler);  TODO
//            pD3DImmediateContext->RSSetViewports(1, &quadViewPort);
            gl.glViewport(0,0, width, height);
            UpdateConstantBuffer(0, 0, m_pBackBufferSurfaceDesc.width, m_pBackBufferSurfaceDesc.height);
//            pD3DImmediateContext->RSSetViewports(1, &viewport);
            gl.glViewport(0,0, viewportWidth, viewportHeight);

            if (RenderType.ROV_OIT == selectedItem)
            {
                Resolve( m_pSwapChainRTV, m_pDSView, NodeIndex);

                // Clear only on touched pixels (using stencil mask) so that it's clear for the next frame.
                if( !debugViewActive )
                    Clear( m_pDSView, NodeIndex );
            }
            else if (RenderType.ROV_HDR_OIT == selectedItem)
            {
                Resolve( m_pSwapChainRTV, m_pDSView, NodeIndex+3);

                // Clear only on touched pixels (using stencil mask) so that it's clear for the next frame.
                if (!debugViewActive)
                    Clear( m_pDSView, NodeIndex);

            }
            else if(RenderType.DX11_AOIT== selectedItem )
            {
                ResolveDX(  m_pSwapChainRTV);
            }
        }
//        pD3DImmediateContext->OMSetRenderTargets(1, &m_pSwapChainRTV, m_pDSView);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        SAFE_RELEASE(m_pSwapChainRTV);
        SAFE_RELEASE(m_pDSView);

        /*pD3DImmediateContext->PSSetShader(NULL, NULL, NULL);
        pD3DImmediateContext->VSSetShader(NULL, NULL, NULL);
        pD3DImmediateContext->GSSetShader(NULL, 0, 0);


        ID3D11ShaderResourceView* nullViews[16] = {0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0};
        pD3DImmediateContext->VSSetShaderResources(0, 16, nullViews);
        pD3DImmediateContext->GSSetShaderResources(0, 16, nullViews);
        pD3DImmediateContext->PSSetShaderResources(0, 16, nullViews);
        pD3DImmediateContext->CSSetShaderResources(0, 16, nullViews);

        ID3D11UnorderedAccessView* nullUAViews[8] = {0, 0, 0, 0, 0, 0, 0, 0};
        pD3DImmediateContext->CSSetUnorderedAccessViews(0, 8, nullUAViews, 0);

        return hr;*/
    }

    void OnCreate(int width, int height/*, ID3D11DeviceContext* pContext, IDXGISwapChain* pSwapChain*/){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        if(fbo == null)
            fbo = new RenderTargets();

//        ID3DBlob* pShaderBlob;
//        ID3DBlob* pErrorBlob;

	    final int MAX_DEFINES = 128; // Note: use MAX_DEFINES to avoid dynamic allocation.  Arbitrarily choose 128.  Not sure if there is a real limit.
        Macro[] pFinalShaderMacros = new Macro[MAX_DEFINES];

        Macro AOITMacro2 = new Macro( "AOIT_NODE_COUNT", "2" );
        Macro AOITMacro4 = new Macro( "AOIT_NODE_COUNT", "4" );
        Macro AOITMacro8 = new Macro( "AOIT_NODE_COUNT", "8" );
        Macro AOITHDRMacro = new Macro( "dohdr", "1");

//        cString ExecutableDirectory;
//        CPUTFileSystem::GetExecutableDirectory(&ExecutableDirectory);

        final String shaderPath = "Intel/OIT/shaders/";
        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
        {
            switch(i)
            {
                case 0:		pFinalShaderMacros[0]   = AOITMacro2; pFinalShaderMacros[1] = null; break;
                case 1:		pFinalShaderMacros[0]   = AOITMacro4; pFinalShaderMacros[1] = null; break;
                case 2:		pFinalShaderMacros[0]   = AOITMacro8; pFinalShaderMacros[1] = null; break;
                case 3:		pFinalShaderMacros[0] = AOITMacro2; pFinalShaderMacros[1] = AOITHDRMacro;  pFinalShaderMacros[2] = null; break;
                case 4:		pFinalShaderMacros[0] = AOITMacro4; pFinalShaderMacros[1] = AOITHDRMacro; pFinalShaderMacros[2] = null; break;
                case 5:		pFinalShaderMacros[0] = AOITMacro8; pFinalShaderMacros[1] = AOITHDRMacro; pFinalShaderMacros[2] = null; break;
            }

            try {
                m_pAOITSPResolvePS[i] = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                        shaderPath + "AOIT_ResolvePS.frag", pFinalShaderMacros);
                m_pAOITSPClearPS[i] = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                        shaderPath + "AOIT_ClearPS.frag", pFinalShaderMacros);
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*cString shaderpath(ExecutableDirectory);

            shaderpath += POST_PROCESS_HLSL;

            {
                hr = D3DCompileFromFile(shaderpath.c_str(), (D3D_SHADER_MACRO*)pFinalShaderMacros, D3D_COMPILE_STANDARD_FILE_INCLUDE,  "AOITSPResolvePS", "ps_5_0",
                        NULL, NULL, &pShaderBlob, &pErrorBlob);
                if(!SUCCEEDED(hr))
                {
                    cString msg = s2ws((char*)pErrorBlob->GetBufferPointer());
                    OutputDebugString(msg.c_str()); DEBUG_PRINT(_L("Assert %s"), msg );
                }

                hr = pD3DDevice->CreatePixelShader(pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),
                        NULL, &m_pAOITSPResolvePS[i]);

                hr = D3DReflect(pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(), IID_ID3D11ShaderReflection, (void**) &m_pAOITSPResolvePSReflection[i]);

                SAFE_RELEASE(pShaderBlob);
            }

            {
                hr = D3DCompileFromFile(shaderpath.c_str(), (D3D_SHADER_MACRO*)pFinalShaderMacros, D3D_COMPILE_STANDARD_FILE_INCLUDE,  "AOITSPClearPS", "ps_5_0",                 NULL, NULL, &pShaderBlob, &pErrorBlob);
                if(!SUCCEEDED(hr))
                {
                    cString msg = s2ws((char*)pErrorBlob->GetBufferPointer());
                    OutputDebugString(msg.c_str()); DEBUG_PRINT(_L("Assert %s"), msg );
                }

                hr = pD3DDevice->CreatePixelShader(pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(),
                        NULL, &m_pAOITSPClearPS[i]);
                SAFE_RELEASE(pShaderBlob);
            }*/

        }

        /*cString shaderpath(ExecutableDirectory);
        shaderpath += POST_PROCESS_DXHLSL;

        hr = D3DCompileFromFile(shaderpath.c_str(), NULL, D3D_COMPILE_STANDARD_FILE_INCLUDE, "AOITResolvePS", "ps_5_0",
                NULL, NULL, &pShaderBlob, &pErrorBlob);
        if(!SUCCEEDED(hr))
        {
            cString msg = s2ws((char*)pErrorBlob->GetBufferPointer());
            OutputDebugString(msg.c_str()); DEBUG_PRINT(_L("Assert %s"), msg );

        }

        hr = pD3DDevice->CreatePixelShader(pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(), NULL, &m_pDXResolvePS);

        hr = D3DReflect(pShaderBlob->GetBufferPointer(), pShaderBlob->GetBufferSize(), IID_ID3D11ShaderReflection, (void**) &m_pDXResolvePSReflection);

        SAFE_RELEASE(pShaderBlob);


        ID3DBlob* pBlob = NULL;*/

        try {
            m_pDXResolvePS = GLSLProgram.createFromFiles("shader_libs/PostProcessingDefaultScreenSpaceVS.vert",
                    shaderPath + "DX_ResolvePS.frag");
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*cString quadshaderpath(ExecutableDirectory);
        quadshaderpath += FULL_SCREEN_QUAD;

        hr = D3DCompileFromFile( quadshaderpath.c_str(), NULL, D3D_COMPILE_STANDARD_FILE_INCLUDE, "VSMain", "vs_4_0", NULL, NULL, &pBlob, NULL);
        hr = pD3DDevice->CreateVertexShader( pBlob->GetBufferPointer(), pBlob->GetBufferSize(),
                NULL, &m_pFullScreenQuadVS );

        D3D11_INPUT_ELEMENT_DESC InputLayout[] =
                { { "SV_POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
                };
        hr = pD3DDevice->CreateInputLayout( InputLayout, ARRAYSIZE( InputLayout ),  pBlob->GetBufferPointer(), pBlob->GetBufferSize(),   &m_pFullScreenQuadLayout );
        pBlob->Release();

        float data[] =
                {	-1.0f,  1.0f, 0.0f,
                        1.0f,  1.0f, 0.0f,
                        -1.0f, -1.0f, 0.0f,
                        1.0f, -1.0f, 0.0f,
                };

        D3D11_BUFFER_DESC BufferDesc;
        ZeroMemory( &BufferDesc, sizeof( BufferDesc ) );
        BufferDesc.ByteWidth = sizeof(float) * 3 * 4;
        BufferDesc.Usage = D3D11_USAGE_DEFAULT;
        BufferDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER;
        BufferDesc.CPUAccessFlags = 0;

        D3D11_SUBRESOURCE_DATA subresourceData;
        subresourceData.pSysMem = data;
        subresourceData.SysMemPitch = 0;
        subresourceData.SysMemSlicePitch = 0;

        pD3DDevice->CreateBuffer(&BufferDesc, &subresourceData, &m_pQuadVB);*/

        /*BufferDesc.ByteWidth = sizeof(int) * 4;
        BufferDesc.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        pD3DDevice->CreateBuffer(&BufferDesc, NULL, &m_pConstBuffer);*/

        m_pConstBuffer = new BufferGL();
        m_pConstBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_DRAW);


        /*D3D11_SAMPLER_DESC samplerDesc = CD3D11_SAMPLER_DESC();
        ZeroMemory(&samplerDesc, sizeof(D3D11_SAMPLER_DESC));
        samplerDesc.AddressU = D3D11_TEXTURE_ADDRESS_CLAMP;
        samplerDesc.AddressV = D3D11_TEXTURE_ADDRESS_CLAMP;
        samplerDesc.Filter = D3D11_FILTER_MIN_MAG_MIP_POINT;
        samplerDesc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
        samplerDesc.ComparisonFunc = D3D11_COMPARISON_NEVER;
        samplerDesc.MaxAnisotropy = 16;
        samplerDesc.MaxLOD = FLT_MAX;
        samplerDesc.MinLOD = FLT_MIN;
        samplerDesc.MipLODBias = 0.0f;

        pD3DDevice->CreateSamplerState(&samplerDesc, &m_pPointSampler);*/
        SamplerDesc samplerDesc = new SamplerDesc();
        samplerDesc.wrapR = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.wrapS = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
        samplerDesc.magFilter = GLenum.GL_NEAREST;
        samplerDesc.minFilter = GLenum.GL_NEAREST_MIPMAP_NEAREST;
        samplerDesc.anisotropic = 16;
        m_pPointSampler = SamplerUtils.createSampler(samplerDesc);

        // Create OIT blend state
        {
            /*CD3D11_BLEND_DESC desc(D3D11_DEFAULT);
            desc.RenderTarget[0].BlendEnable = true;
            desc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
            desc.RenderTarget[0].DestBlend = D3D11_BLEND_SRC_ALPHA;
            desc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
            pD3DDevice->CreateBlendState(&desc, &mAOITCompositeBlendState);*/
            mAOITCompositeBlendState = new Runnable() {
                @Override
                public void run() {
                    gl.glEnable(GLenum.GL_BLEND);
                    gl.glBlendFunc(GLenum.GL_ONE, GLenum.GL_ONE_MINUS_SRC_ALPHA);
                }
            };
        }

        // Create AOIT depth stencil desc
        {
            /*D3D11_DEPTH_STENCIL_DESC DSDesc;
            DSDesc.DepthEnable                  = FALSE;
            DSDesc.DepthFunc                    = D3D11_COMPARISON_GREATER;
            DSDesc.DepthWriteMask               = D3D11_DEPTH_WRITE_MASK_ZERO;
            DSDesc.StencilEnable                = TRUE;
            DSDesc.StencilReadMask              = 0xFF;
            DSDesc.StencilWriteMask             = 0x00;
            DSDesc.FrontFace.StencilFailOp      = D3D11_STENCIL_OP_KEEP;
            DSDesc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_KEEP;
            DSDesc.FrontFace.StencilPassOp      = D3D11_STENCIL_OP_KEEP;
            DSDesc.FrontFace.StencilFunc        = D3D11_COMPARISON_EQUAL;
            DSDesc.BackFace.StencilFailOp       = D3D11_STENCIL_OP_KEEP;
            DSDesc.BackFace.StencilDepthFailOp  = D3D11_STENCIL_OP_KEEP;
            DSDesc.BackFace.StencilPassOp       = D3D11_STENCIL_OP_KEEP;
            DSDesc.BackFace.StencilFunc         = D3D11_COMPARISON_EQUAL;
            hr = pD3DDevice->CreateDepthStencilState(&DSDesc, &mAOITCompositeDepthStencilState );*/
            mAOITCompositeDepthStencilState = new Runnable() {
                @Override
                public void run() {
                    gl.glDisable(GLenum.GL_DEPTH_TEST);
                    gl.glEnable(GLenum.GL_STENCIL_TEST);
                    gl.glStencilFunc(GLenum.GL_EQUAL, 0x01, 0xFF);
                    gl.glStencilOp(GLenum.GL_KEEP,GLenum.GL_KEEP,GLenum.GL_KEEP);
                    gl.glStencilMask(0x00);  // TODO
                }
            };
        }

        /*D3D11_BUFFER_DESC bd = {0};
        bd.ByteWidth = sizeof(FL_Constants);
        bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;
        bd.Usage = D3D11_USAGE_DYNAMIC;
        bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;

        ID3D11Buffer * pOurConstants;
        hr = (CPUT_DX11::GetDevice())->CreateBuffer( &bd, NULL, &pOurConstants );*/
        BufferGL pOurConstants = new BufferGL();
        pOurConstants.initlize(GLenum.GL_UNIFORM_BUFFER, 16, null, GLenum.GL_DYNAMIC_READ);

        String name =("$FL_Constants");

        CPUTAssetLibrary.GetAssetLibrary().AddConstantBuffer( name, mFragmentListConstants );
        SAFE_RELEASE(pOurConstants); // We're done with it.  The CPUTBuffer now owns it.
        //mGlobalProperties

        final int DXGI_FORMAT_R32_UINT = GLenum.GL_R32UI;
        mpClearMaskRT = (CPUTTextureDX11)CPUTTextureDX11.CreateTexture(("$ClearMaskRT"), DXGI_FORMAT_R32_UINT, width, height, DXGI_FORMAT_R32_UINT, 0/*D3D11_BIND_RENDER_TARGET |D3D11_BIND_UNORDERED_ACCESS*/, 1);
        mpClearMaskRT.AddUAVView(DXGI_FORMAT_R32_UINT);
    }
    void OnShutdown(){
        ReleaseResources();

//        SAFE_RELEASE(m_pQuadVB);
//        SAFE_RELEASE(m_pFullScreenQuadVS);
//        SAFE_RELEASE(m_pFullScreenQuadLayout);

        for (int i = 0; i < MAX_SHADER_VARIATIONS; ++i)
        {
            SAFE_RELEASE(m_pAOITSPResolvePS[i]);
//            SAFE_RELEASE(m_pAOITSPResolvePSReflection[i]);
            SAFE_RELEASE(m_pAOITSPClearPS[i]);
        }

        SAFE_RELEASE(m_pDXResolvePS);
//        SAFE_RELEASE(m_pDXResolvePSReflection);

        SAFE_RELEASE (mpIntelExt);
        SAFE_RELEASE( mFragmentListNodesBuffer );
        SAFE_RELEASE( mpAOITSPColorDataUAVBuffer);
        SAFE_RELEASE (mpAOITSPDepthDataUAVBuffer);
        SAFE_RELEASE( mp8AOITSPColorDataUAVBuffer);
        SAFE_RELEASE (mp8AOITSPDepthDataUAVBuffer);
        SAFE_RELEASE( mpFragmentListFirstNodeOffseBuffer);

        SAFE_RELEASE (mFragmentListConstants);

        SAFE_RELEASE(m_pConstBuffer);

//        SAFE_RELEASE(mAOITCompositeBlendState);
//        SAFE_RELEASE(mAOITCompositeDepthStencilState);

        /*if(m_pBackBufferSurfaceDesc != NULL)
        {
            delete m_pBackBufferSurfaceDesc;
            m_pBackBufferSurfaceDesc = NULL;
        }*/

        SamplerUtils.releaseCaches();
    }

    void OnSize(/*ID3D11Device* pD3DDevice,*/ int width, int height){
        CPUTAssetLibraryDX11 pAssetLibrary = (CPUTAssetLibraryDX11)CPUTAssetLibraryDX11.GetAssetLibrary();


        if(width == 0 || height == 0)
            return;


        mpClearMaskRT.Resize(width, height);

//        m_pBackBufferSurfaceDesc.Height = height;
//        m_pBackBufferSurfaceDesc->Width = width;

        ReleaseResources();


        mAOITSPDepthData[0].initlize(GLenum.GL_SHADER_STORAGE_BUFFER, width*height*4 * 4, null, GLenum.GL_DYNAMIC_READ );
        mAOITSPColorData[0].initlize(GLenum.GL_SHADER_STORAGE_BUFFER,width*height*4 * 4, null, GLenum.GL_DYNAMIC_READ );
        mAOITSPDepthData[1].initlize(GLenum.GL_SHADER_STORAGE_BUFFER,width*height*4 * 8, null, GLenum.GL_DYNAMIC_READ  );
        mAOITSPColorData[1].initlize(GLenum.GL_SHADER_STORAGE_BUFFER,width*height*4 * 8, null, GLenum.GL_DYNAMIC_READ );


//        UINT bindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_RENDER_TARGET;

        int structSize = /*sizeof(FragmentNode)*/12;
        final int nodeCount = mLisTexNodeCount * 2;
        mFragmentListNodes.initlize(GLenum.GL_SHADER_STORAGE_BUFFER, 1*nodeCount* structSize,null, GLenum.GL_DYNAMIC_READ);


        mATSPClearMaskInitialized = false;
//        mFragmentListFirstNodeOffset.initlize(pD3DDevice, width, height, DXGI_FORMAT_R32_UINT, bindFlags | D3D11_BIND_UNORDERED_ACCESS, 1);
        SAFE_RELEASE(mFragmentListFirstNodeOffset);

        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_R32UI);
        mFragmentListFirstNodeOffset = TextureUtils.createTexture2D(desc, null);


        if(mpFragmentListFirstNodeOffseBuffer != null)
        {
            mpFragmentListFirstNodeOffseBuffer.SetBufferAndViews( null, mFragmentListFirstNodeOffset );
        }
        else
        {
            String FragmentListFirstNodeAddressName = "$gFragmentListFirstNodeAddressUAV";
            mpFragmentListFirstNodeOffseBuffer  = new CPUTBufferDX11( FragmentListFirstNodeAddressName, null, mFragmentListFirstNodeOffset );
            mpFragmentListFirstNodeOffseBuffer.SetShaderResourceView(mFragmentListFirstNodeOffset);  // TODO
            pAssetLibrary.AddBuffer( FragmentListFirstNodeAddressName,  mpFragmentListFirstNodeOffseBuffer );
        }

        if(mFragmentListNodesBuffer == null)
        {
            String gFragmentListNodesUAVName = "$gFragmentListNodesUAV";
            mFragmentListNodesBuffer  = new CPUTBufferDX11( gFragmentListNodesUAVName, mFragmentListNodes, null );
            pAssetLibrary.AddBuffer( gFragmentListNodesUAVName, mFragmentListNodesBuffer );
        }
        else
        {
            mFragmentListNodesBuffer.SetBufferAndViews( mFragmentListNodes, null);
        }

        if(mpAOITSPDepthDataUAVBuffer == null)
        {
            String gAOITSPDepthDataUAVName = "$gAOITSPDepthDataUAV";
            mpAOITSPDepthDataUAVBuffer  = new CPUTBufferDX11( gAOITSPDepthDataUAVName, mAOITSPDepthData[0] );
            pAssetLibrary.AddBuffer( gAOITSPDepthDataUAVName, mpAOITSPDepthDataUAVBuffer );
        }

        if(mp8AOITSPDepthDataUAVBuffer == null)
        {
            String gAOITSPDepthDataUAVName = "$g8AOITSPDepthDataUAV";
            mp8AOITSPDepthDataUAVBuffer  = new CPUTBufferDX11( gAOITSPDepthDataUAVName, mAOITSPDepthData[1] );
            pAssetLibrary.AddBuffer( gAOITSPDepthDataUAVName, mp8AOITSPDepthDataUAVBuffer );
        }

        if(mpAOITSPColorDataUAVBuffer == null)
        {
            String gAOITSPColorDataUAVName = "$gAOITSPColorDataUAV";
            mpAOITSPColorDataUAVBuffer  = new CPUTBufferDX11( gAOITSPColorDataUAVName, mAOITSPColorData[0] );
            pAssetLibrary.AddBuffer( gAOITSPColorDataUAVName, mpAOITSPColorDataUAVBuffer );
        }

        if(mp8AOITSPColorDataUAVBuffer == null)
        {
            String gAOITSPColorDataUAVName = "$g8AOITSPColorDataUAV";
            mp8AOITSPColorDataUAVBuffer  = new CPUTBufferDX11( gAOITSPColorDataUAVName, mAOITSPColorData[1]);
            pAssetLibrary.AddBuffer( gAOITSPColorDataUAVName, mp8AOITSPColorDataUAVBuffer );
        }


        /*mpAOITSPDepthDataUAVBuffer.SetBufferAndViews( NULL, mAOITSPDepthData[0].m_pSRV, mAOITSPDepthData[0].m_pUAV );
        mpAOITSPColorDataUAVBuffer.SetBufferAndViews( NULL, mAOITSPColorData[0].m_pSRV, mAOITSPColorData[0].m_pUAV );
        mp8AOITSPDepthDataUAVBuffer.SetBufferAndViews( NULL, mAOITSPDepthData[1].m_pSRV, mAOITSPDepthData[1].m_pUAV );
        mp8AOITSPColorDataUAVBuffer.SetBufferAndViews( NULL, mAOITSPColorData[1].m_pSRV, mAOITSPColorData[1].m_pUAV );*/


        if(mpIntelExt != null)
        {
            ((CPUTBufferDX11)mpIntelExt).SetBufferAndViews( null, null );
        }
        else
        {
            String g_IntelExtName = "$g_IntelExt";
            mpIntelExt  = new CPUTBufferDX11( /*g_IntelExtName, null*/ );
            pAssetLibrary.AddBuffer( g_IntelExtName, mpIntelExt );
        }
    }

    void FillFragmentListConstants(/*ID3D11DeviceContext* d3dDeviceContext,*/ int listNodeCount){
        BufferGL pBuffer = mFragmentListConstants.GetNativeBuffer();

        // List texture related constants
        /*D3D11_MAPPED_SUBRESOURCE mapInfo;  TODO
        d3dDeviceContext->Map( pBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &mapInfo );
        {
            FL_Constants & consts = *((FL_Constants*)mapInfo.pData);

            consts.mMaxListNodes = listNodeCount;

            d3dDeviceContext->Unmap(pBuffer,0);
        }*/
    }

    void UpdateConstantBuffer(/*ID3D11DeviceContext* pD3DImmediateContext,*/ int mipLevel0, int mipLevel1, int width,  int height){
//        int src[] = {mipLevel0, mipLevel1, width, height};
        int rowpitch = /*sizeof(int)*/4 * 4;
        /*pD3DImmediateContext->UpdateSubresource(m_pConstBuffer, 0, NULL, &src, rowpitch, 0);
        pD3DImmediateContext->PSSetConstantBuffers(0, 1, &m_pConstBuffer);*/

        IntBuffer buffer = CacheBuffer.wrap(mipLevel0, mipLevel1, width, height);
        m_pConstBuffer.update(0, buffer);
    }
//    void GetBindIndex(struct ID3D11ShaderReflection* shaderReflection, const char *name, UINT *bindIndex);

    void Clear(/*ID3D11DeviceContext* pD3DImmediateContext,*/ Texture2D pDSV,int NodeIndex){
        /*ID3D11UnorderedAccessView* pUAVs[3];
        switch(NodeIndex)
        {
            case 0:
                pUAVs[0] = mpClearMaskRT->GetColorUAV();
                pUAVs[1] = NULL;
                pUAVs[2] = mpAOITSPColorDataUAVBuffer->GetUnorderedAccessView();
                break;
            case 1:
                pUAVs[0] = mpClearMaskRT->GetColorUAV();
                pUAVs[1] = mpAOITSPDepthDataUAVBuffer->GetUnorderedAccessView();
                pUAVs[2] = mpAOITSPColorDataUAVBuffer->GetUnorderedAccessView();
                break;
            case 2:
                pUAVs[0] = mpClearMaskRT->GetColorUAV();
                pUAVs[1] = mp8AOITSPDepthDataUAVBuffer->GetUnorderedAccessView();
                pUAVs[2] = mp8AOITSPColorDataUAVBuffer->GetUnorderedAccessView();
                break;
        }

        pD3DImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews( 0, NULL, pDSV, 1, 3, pUAVs, NULL );
        pD3DImmediateContext->PSSetShader(m_pAOITSPClearPS[NodeIndex], NULL, NULL);
        pD3DImmediateContext->OMSetBlendState(mAOITCompositeBlendState, 0, 0xffffffff);
        ID3D11DepthStencilState * pBackupState = NULL; UINT backupStencilRef = 0;
        pD3DImmediateContext->OMGetDepthStencilState( &pBackupState, &backupStencilRef );
        if( pDSV != NULL )
        {
            pD3DImmediateContext->OMSetDepthStencilState( mAOITCompositeDepthStencilState, 0x01 );
        }
        DrawFullScreenQuad(pD3DImmediateContext);
        pD3DImmediateContext->OMSetDepthStencilState( pBackupState, backupStencilRef );
        SAFE_RELEASE( pBackupState );*/

        // TODO setup the unorderviews.
        m_pAOITSPClearPS[NodeIndex].enable();
        mAOITCompositeBlendState.run();
        if(pDSV != null){
            mAOITCompositeDepthStencilState.run();
        }
        DrawFullScreenQuad();
    }

    void Resolve(/*ID3D11DeviceContext* pD3DImmediateContext,*/  Texture2D pOutput, Texture2D  pDSV,int NodeIndex){
//        pD3DImmediateContext->OMSetRenderTargets(1, &pOutput, pDSV);
        TextureGL[] RTVs = {pOutput, pDSV};
        fbo.setRenderTextures(RTVs, null);

//        ID3D11ShaderResourceView* pAOITClearMaskSRV[] = {  mpClearMaskRT->GetShaderResourceView() };

        TextureGL  pAOITClearMaskSRV = mpClearMaskRT.GetTexture();
        int bindIndex = -1;
        int bindIndex2 = -1;
        int clearBindIndex = -1;


        if((NodeIndex==2) || (NodeIndex == 5)) // 8 node version for normal and HDR
        {
            TextureGL pSRVs = mp8AOITSPColorDataUAVBuffer.GetShaderResourceView();
            /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "g8AOITSPColorDataSRV", &bindIndex) == S_OK) {
                pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  pSRVs);  TODO
            }*/

            if (NodeIndex == 5)
            {
                TextureGL pDSRVs = mp8AOITSPDepthDataUAVBuffer.GetShaderResourceView();
                /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "g8AOITSPDepthDataSRV", &bindIndex2) == S_OK) {
                    pD3DImmediateContext->PSSetShaderResources(bindIndex2, 1, pDSRVs);  TODO
                }*/
            }
        }else
        {
            TextureGL pSRVs = mpAOITSPColorDataUAVBuffer.GetShaderResourceView();
            /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "gAOITSPColorDataSRV", &bindIndex) == S_OK) {
                pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  pSRVs);  TODO
            }*/
            if (NodeIndex == 4)
            {
                TextureGL pDSRVs = mpAOITSPDepthDataUAVBuffer.GetShaderResourceView();
                /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "gAOITSPDepthDataSRV", &bindIndex2) == S_OK) {
                    pD3DImmediateContext->PSSetShaderResources(bindIndex2, 1, pDSRVs);  TODO
                }*/
            }

        }

        /*if (GetBindIndex(m_pAOITSPResolvePSReflection[NodeIndex], "gAOITSPClearMaskSRV", &clearBindIndex) == S_OK) {
            pD3DImmediateContext->PSSetShaderResources(clearBindIndex,1,  &pAOITClearMaskSRV[0]);  TODO
        }*/

        m_pAOITSPResolvePS[NodeIndex].enable();

//        pD3DImmediateContext->OMSetBlendState(mAOITCompositeBlendState, 0, 0xffffffff);
        mAOITCompositeBlendState.run();

        /*ID3D11DepthStencilState * pBackupState = NULL; UINT backupStencilRef = 0;
        pD3DImmediateContext->OMGetDepthStencilState( &pBackupState, &backupStencilRef );
        pD3DImmediateContext->OMSetDepthStencilState( mAOITCompositeDepthStencilState, 0x01 );*/
        mAOITCompositeDepthStencilState.run();

        DrawFullScreenQuad();

        /*pD3DImmediateContext->OMSetDepthStencilState( pBackupState, backupStencilRef );
        SAFE_RELEASE( pBackupState );*/
        gl.glDisable(GLenum.GL_STENCIL_TEST);
        gl.glDisable(GLenum.GL_BLEND);

       /* if( bindIndex != -1 )  TODO
        {
            ID3D11ShaderResourceView * nullSRV = NULL;
            pD3DImmediateContext->PSSetShaderResources( bindIndex, 1, &nullSRV );
        }
        if (bindIndex2 != -1)
        {
            ID3D11ShaderResourceView * nullSRV = NULL;
            pD3DImmediateContext->PSSetShaderResources(bindIndex2, 1, &nullSRV);
        }
        if( clearBindIndex != -1 )
        {
            ID3D11ShaderResourceView * nullSRV = NULL;
            pD3DImmediateContext->PSSetShaderResources( clearBindIndex, 1, &nullSRV );
        }*/
    }
    void ResolveDX(/*ID3D11DeviceContext* pD3DImmediateContext,*/ Texture2D pOutput){
//        pD3DImmediateContext->OMSetRenderTargets(1, &pOutput, NULL);
        fbo.setRenderTexture(pOutput, null);

//        ID3D11ShaderResourceView* pAOITClearMaskSRV[] = { mpClearMaskRT->GetShaderResourceView()};

        /*UINT bindIndex;
        if (GetBindIndex(m_pDXResolvePSReflection, "gFragmentListFirstNodeAddressSRV", &bindIndex) == S_OK) {
            pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  &mFragmentListFirstNodeOffset.m_pSRV);  TODO
        }
        if (GetBindIndex(m_pDXResolvePSReflection, "gFragmentListNodesSRV", &bindIndex) == S_OK) {
            pD3DImmediateContext->PSSetShaderResources(bindIndex,1,  &mFragmentListNodes.m_pSRV);  TODO
        }*/

        /*pD3DImmediateContext->PSSetShader(m_pDXResolvePS, NULL, NULL);
        pD3DImmediateContext->OMSetBlendState(mAOITCompositeBlendState, 0, 0xffffffff);*/
        m_pDXResolvePS.enable();
        mAOITCompositeBlendState.run();

        DrawFullScreenQuad();

        gl.glDisable(GLenum.GL_BLEND);
    }

    void ReleaseResources(){
//create float render target
        SAFE_RELEASE(m_pDSView);
        SAFE_RELEASE(m_pSwapChainRTV);

        for (int i = 0; i < 2; ++i)
        {
            SAFE_RELEASE(mAOITSPDepthData[i]);
            SAFE_RELEASE(mAOITSPColorData[i]);
        }

        SAFE_RELEASE(mFragmentListNodes);
        SAFE_RELEASE(mFragmentListFirstNodeOffset);
    }

    /*
        DrawFullScreenQuad
        Helper functions for drawing a full screen quad (used for the final tonemapping and bloom composite.
        Renders the quad with and passes vertex position and texture coordinates to current pixel shader
    */
    void DrawFullScreenQuad(){
        gl.glBindVertexArray(0);
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
    }
}
