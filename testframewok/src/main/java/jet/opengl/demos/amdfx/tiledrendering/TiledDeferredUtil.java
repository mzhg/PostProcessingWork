package jet.opengl.demos.amdfx.tiledrendering;

import java.util.Objects;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

final class TiledDeferredUtil implements ICONST, Disposeable {
    static final int MAX_NUM_GBUFFER_RENDER_TARGETS = 5;

    // G-Buffer
    private final Texture2D[] m_pGBuffer = new Texture2D[MAX_NUM_GBUFFER_RENDER_TARGETS];
//    ID3D11ShaderResourceView* m_pGBufferSRV[MAX_NUM_GBUFFER_RENDER_TARGETS];
//    ID3D11RenderTargetView* m_pGBufferRTV[MAX_NUM_GBUFFER_RENDER_TARGETS];

    // off-screen buffer for shading (the compute shader writes to this)
    private Texture2D           m_pOffScreenBuffer;
    /*ID3D11ShaderResourceView*   m_pOffScreenBufferSRV;
    ID3D11RenderTargetView*     m_pOffScreenBufferRTV;
    ID3D11UnorderedAccessView*  m_pOffScreenBufferUAV;*/

    // shaders for Tiled Deferred (G-Buffer)
    private static final int NUM_GBUFFER_PIXEL_SHADERS = 2*(MAX_NUM_GBUFFER_RENDER_TARGETS-1);
    /*ID3D11VertexShader*         m_pSceneDeferredBuildGBufferVS;
    ID3D11PixelShader*          m_pSceneDeferredBuildGBufferPS[NUM_GBUFFER_PIXEL_SHADERS];*/
    private GLSLProgram[]       m_pSceneDeferredBuildGBufferPS = new GLSLProgram[NUM_GBUFFER_PIXEL_SHADERS];
    private ID3D11InputLayout   m_pLayoutDeferredBuildGBuffer11;

    // compute shaders for tiled culling and shading
    private static final int NUM_DEFERRED_LIGHTING_COMPUTE_SHADERS = 2*2*NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1);
    private GLSLProgram[]        m_pLightCullAndShadeCS = new GLSLProgram[NUM_DEFERRED_LIGHTING_COMPUTE_SHADERS];

    // debug draw shaders for the lights-per-tile visualization modes
    private static final int NUM_DEBUG_DRAW_COMPUTE_SHADERS = 2*2*NUM_MSAA_SETTINGS;  // one for each MSAA setting,
    // times 2 for VPL on/off,
    // times 2 for radar vs. grayscale
    private GLSLProgram[]        m_pDebugDrawNumLightsPerTileCS = new GLSLProgram[NUM_DEBUG_DRAW_COMPUTE_SHADERS];

    // state for Tiled Deferred
    private Runnable           m_pBlendStateOpaque;
    private Runnable           m_pBlendStateAlphaToCoverage;
    private Runnable           m_pBlendStateAlpha;

    private GLFuncProvider gl;

    // Constructor / destructor

    void AddShadersToCache( /*AMD::ShaderCache *pShaderCache*/ ){
        // Ensure all shaders (and input layouts) are released
//        SAFE_RELEASE(m_pSceneDeferredBuildGBufferVS);
//        SAFE_RELEASE(m_pLayoutDeferredBuildGBuffer11);

        for( int i = 0; i < NUM_GBUFFER_PIXEL_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pSceneDeferredBuildGBufferPS[i]);
        }

        for( int i = 0; i < NUM_DEFERRED_LIGHTING_COMPUTE_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pLightCullAndShadeCS[i]);
        }

        for( int i = 0; i < NUM_DEBUG_DRAW_COMPUTE_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pDebugDrawNumLightsPerTileCS[i]);
        }

        Macro[] ShaderMacroBuildGBufferPS = {
            new Macro("USE_ALPHA_TEST", 0),
            new Macro("NUM_GBUFFER_RTS", 0),
        };
        /*wcscpy_s( ShaderMacroBuildGBufferPS[0].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"USE_ALPHA_TEST" );
        wcscpy_s( ShaderMacroBuildGBufferPS[1].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"NUM_GBUFFER_RTS" );*/

        final int DXGI_FORMAT_R32G32B32_FLOAT = GLenum.GL_RGBA32F;
        final int DXGI_FORMAT_R32G32_FLOAT = GLenum.GL_RG32F;
        final int D3D11_INPUT_PER_VERTEX_DATA = 0;
        final D3D11_INPUT_ELEMENT_DESC Layout[] =
        {
            new D3D11_INPUT_ELEMENT_DESC("POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT, 0,  0, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "NORMAL",   0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 12, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,    0, 24, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
            new D3D11_INPUT_ELEMENT_DESC( "TANGENT",  0, DXGI_FORMAT_R32G32B32_FLOAT, 0, 32, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
        };

        /*pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pSceneDeferredBuildGBufferVS, AMD::ShaderCache::SHADER_TYPE_VERTEX, L"vs_5_0", L"RenderSceneToGBufferVS",
                L"Deferred.hlsl", 0, NULL, &m_pLayoutDeferredBuildGBuffer11, Layout, ARRAYSIZE( Layout ) );*/

        m_pLayoutDeferredBuildGBuffer11 = ID3D11InputLayout.createInputLayoutFrom(Layout);

        for( int i = 0; i < 2; i++ )
        {
            // USE_ALPHA_TEST false first time through, then true
            ShaderMacroBuildGBufferPS[0].value = i;

            for( int j = 2; j <= MAX_NUM_GBUFFER_RENDER_TARGETS; j++ )
            {
                // set NUM_GBUFFER_RTS
                ShaderMacroBuildGBufferPS[1].value = j;

                /*pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pSceneDeferredBuildGBufferPS[(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*i+j-2], AMD::ShaderCache::SHADER_TYPE_PIXEL, L"ps_5_0", L"RenderSceneToGBufferPS",
                    L"Deferred.hlsl", 2, ShaderMacroBuildGBufferPS, NULL, NULL, 0 );*/
                m_pSceneDeferredBuildGBufferPS[(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*i+j-2] = GLSLProgram.createProgram(SHADER_PATH + "RenderSceneToGBufferVS.vert",
                        SHADER_PATH + "RenderSceneToGBufferPS.frag", ShaderMacroBuildGBufferPS);
            }
        }

        /*AMD::ShaderCache::Macro ShaderMacroLightCullCS[5];
        wcscpy_s( ShaderMacroLightCullCS[0].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"TILED_CULLING_COMPUTE_SHADER_MODE" );
        wcscpy_s( ShaderMacroLightCullCS[1].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"SHADOWS_ENABLED" );
        wcscpy_s( ShaderMacroLightCullCS[2].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"NUM_MSAA_SAMPLES" );
        wcscpy_s( ShaderMacroLightCullCS[3].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"NUM_GBUFFER_RTS" );
        wcscpy_s( ShaderMacroLightCullCS[4].m_wsName, AMD::ShaderCache::m_uMACRO_MAX_LENGTH, L"LIGHTS_PER_TILE_MODE" );*/

        Macro[] ShaderMacroLightCullCS = {
            new Macro("TILED_CULLING_COMPUTE_SHADER_MODE", 0),
            new Macro("SHADOWS_ENABLED", 0),
            new Macro("NUM_MSAA_SAMPLES", 0),
            new Macro("NUM_GBUFFER_RTS", 0),
            new Macro("LIGHTS_PER_TILE_MODE", 0),
        };

        // Set LIGHTS_PER_TILE_MODE to 0 (lights per tile visualization disabled)
        ShaderMacroLightCullCS[4].value = 0;

        for( int i = 0; i < 2; i++ )
        {
            // TILED_CULLING_COMPUTE_SHADER_MODE 2 first time through (Tiled Deferred, VPLs disabled),
            // then 3 (Tiled Deferred, VPLs enabled)
            ShaderMacroLightCullCS[0].value = i+2;

            for( int j = 0; j < 2; j++ )
            {
                // SHADOWS_ENABLED false first time through, then true
                ShaderMacroLightCullCS[1].value = j;

                for( int k = 0; k < NUM_MSAA_SETTINGS; k++ )
                {
                    // set NUM_MSAA_SAMPLES
                    ShaderMacroLightCullCS[2].value = g_nMSAASampleCount[k];

                    for( int m = 2; m <= MAX_NUM_GBUFFER_RENDER_TARGETS; m++ )
                    {
                        // set NUM_GBUFFER_RTS
                        ShaderMacroLightCullCS[3].value = m;

                        /*pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pLightCullAndShadeCS[2*NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*i + NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*j + (MAX_NUM_GBUFFER_RENDER_TARGETS-1)*k + m-2], AMD::ShaderCache::SHADER_TYPE_COMPUTE, L"cs_5_0", L"CullLightsAndDoLightingCS",
                            L"TilingDeferred.hlsl", 5, ShaderMacroLightCullCS, NULL, NULL, 0 );*/

                        final String root = "amdfx\\TiledLighting11\\shaders\\";
                        m_pLightCullAndShadeCS[2*NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*i + NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*j + (MAX_NUM_GBUFFER_RENDER_TARGETS-1)*k + m-2] =
                                GLSLProgram.createProgram(root + "CullLightsAndDoLightingCS.comp",  ShaderMacroBuildGBufferPS);
                    }
                }
            }
        }

        // Set SHADOWS_ENABLED to 0 (false)
        ShaderMacroLightCullCS[1].value = 0;

        // Set NUM_GBUFFER_RTS to 2
        ShaderMacroLightCullCS[3].value = 2;

        for( int i = 0; i < 2; i++ )
        {
            // TILED_CULLING_COMPUTE_SHADER_MODE 2 first time through (Tiled Deferred, VPLs disabled),
            // then 3 (Tiled Deferred, VPLs enabled)
            ShaderMacroLightCullCS[0].value = i+2;

            for( int j = 0; j < 2; j++ )
            {
                // LIGHTS_PER_TILE_MODE 1 first time through (grayscale), then 2 (radar colors)
                ShaderMacroLightCullCS[4].value = j+1;

                for( int k = 0; k < NUM_MSAA_SETTINGS; k++ )
                {
                    // set NUM_MSAA_SAMPLES
                    ShaderMacroLightCullCS[2].value = g_nMSAASampleCount[k];

                   /* pShaderCache->AddShader( (ID3D11DeviceChild**)&m_pDebugDrawNumLightsPerTileCS[2*NUM_MSAA_SETTINGS*i + NUM_MSAA_SETTINGS*j + k], AMD::ShaderCache::SHADER_TYPE_COMPUTE, L"cs_5_0", L"CullLightsAndDoLightingCS",
                        L"TilingDeferred.hlsl", 5, ShaderMacroLightCullCS, NULL, NULL, 0 );*/

                    final String root = "amdfx\\TiledLighting11\\shaders\\";
                    m_pDebugDrawNumLightsPerTileCS[2*NUM_MSAA_SETTINGS*i + NUM_MSAA_SETTINGS*j + k] =
                            GLSLProgram.createProgram(root + "TilingDeferred.comp",  ShaderMacroBuildGBufferPS);
                }
            }
        }
    }

    // Various hook functions
    void OnCreateDevice( /*ID3D11Device* pd3dDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();

        // Create blend states
        /*D3D11_BLEND_DESC BlendStateDesc;
        ZeroMemory( &BlendStateDesc, sizeof( D3D11_BLEND_DESC ) );
        BlendStateDesc.AlphaToCoverageEnable = FALSE;
        BlendStateDesc.IndependentBlendEnable = FALSE;
        D3D11_RENDER_TARGET_BLEND_DESC RTBlendDesc;
        RTBlendDesc.BlendEnable = FALSE;
        RTBlendDesc.SrcBlend = D3D11_BLEND_ONE;
        RTBlendDesc.DestBlend = D3D11_BLEND_ZERO;
        RTBlendDesc.BlendOp = D3D11_BLEND_OP_ADD;
        RTBlendDesc.SrcBlendAlpha = D3D11_BLEND_ONE;
        RTBlendDesc.DestBlendAlpha = D3D11_BLEND_ZERO;
        RTBlendDesc.BlendOpAlpha = D3D11_BLEND_OP_ADD;
        RTBlendDesc.RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        BlendStateDesc.RenderTarget[0] = RTBlendDesc;
        BlendStateDesc.RenderTarget[1] = RTBlendDesc;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateOpaque ) );*/
        m_pBlendStateOpaque = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
        };

        /*BlendStateDesc.AlphaToCoverageEnable = TRUE;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateAlphaToCoverage ) );*/
        m_pBlendStateAlphaToCoverage = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
        };

        /*BlendStateDesc.AlphaToCoverageEnable = FALSE;
        RTBlendDesc.BlendEnable = TRUE;
        RTBlendDesc.SrcBlend = D3D11_BLEND_SRC_ALPHA;
        RTBlendDesc.DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
        RTBlendDesc.SrcBlendAlpha = D3D11_BLEND_SRC_ALPHA;
        RTBlendDesc.DestBlendAlpha = D3D11_BLEND_INV_SRC_ALPHA;
        BlendStateDesc.RenderTarget[0] = RTBlendDesc;
        BlendStateDesc.RenderTarget[1] = RTBlendDesc;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateAlpha ) );*/

        m_pBlendStateAlpha= ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);
        };
    }
    void OnDestroyDevice(){  dispose();}
    void OnResizedSwapChain( int width, int height, int samplerCount ){
        // create the G-Buffer
//        V_RETURN( AMD::CreateSurface( &m_pGBuffer[0], &m_pGBufferSRV[0], &m_pGBufferRTV[0], NULL, DXGI_FORMAT_R8G8B8A8_UNORM, pBackBufferSurfaceDesc->Width, pBackBufferSurfaceDesc->Height, pBackBufferSurfaceDesc->SampleDesc.Count ) );
//        V_RETURN( AMD::CreateSurface( &m_pGBuffer[1], &m_pGBufferSRV[1], &m_pGBufferRTV[1], NULL, DXGI_FORMAT_R8G8B8A8_UNORM, pBackBufferSurfaceDesc->Width, pBackBufferSurfaceDesc->Height, pBackBufferSurfaceDesc->SampleDesc.Count ) );
        Texture2DDesc desc = new Texture2DDesc(width, height,GLenum.GL_RGBA8);
        desc.sampleCount = samplerCount;
        m_pGBuffer[0] = TextureUtils.createTexture2D(desc, null);
        m_pGBuffer[1] = TextureUtils.createTexture2D(desc, null);

        // create extra dummy G-Buffer render targets to test performance
        // as the G-Buffer gets fatter
        for( int i = 2; i < MAX_NUM_GBUFFER_RENDER_TARGETS; i++ )
        {
//            V_RETURN( AMD::CreateSurface( &m_pGBuffer[i], &m_pGBufferSRV[i], &m_pGBufferRTV[i], NULL, DXGI_FORMAT_R8G8B8A8_UNORM, pBackBufferSurfaceDesc->Width, pBackBufferSurfaceDesc->Height, pBackBufferSurfaceDesc->SampleDesc.Count ) );
            m_pGBuffer[i] = TextureUtils.createTexture2D(desc, null);
        }

        // create the offscreen buffer for shading
        // (note, multisampling is not supported with UAVs,
        // so scale the resolution instead)
        int uCorrectedWidth = (samplerCount > 1)  ? 2*width : width;
        int uCorrectedHeight = (samplerCount == 4) ? 2*height : height;
//        V_RETURN( AMD::CreateSurface( &m_pOffScreenBuffer, &m_pOffScreenBufferSRV, &m_pOffScreenBufferRTV, &m_pOffScreenBufferUAV, DXGI_FORMAT_R16G16B16A16_FLOAT, uCorrectedWidth, uCorrectedHeight, 1 ) );

        desc.width = uCorrectedWidth;
        desc.height = uCorrectedHeight;
        desc.format = GLenum.GL_RGBA16F;
        desc.sampleCount = 1;
        m_pOffScreenBuffer = TextureUtils.createTexture2D(desc, null);
    }

    void OnReleasingSwapChain(){
        for( int i = 0; i < MAX_NUM_GBUFFER_RENDER_TARGETS; i++ )
        {
            SAFE_RELEASE(m_pGBuffer[i]);
        }

        SAFE_RELEASE(m_pOffScreenBuffer);
    }

    void OnRender( float fElapsedTime, GuiState CurrentGuiState, Texture2D DepthStencilBufferForOpaque, Texture2D DepthStencilBufferForTransparency,
                   Scene Scene, CommonUtil CommonUtil, LightUtil LightUtil, ShadowRenderer ShadowRenderer, RSMRenderer RSMRenderer ){
        assert(CurrentGuiState.m_nNumGBufferRenderTargets >=2 && CurrentGuiState.m_nNumGBufferRenderTargets <= MAX_NUM_GBUFFER_RENDER_TARGETS);

        // no need to clear DXUT's main RT, because we do a full-screen blit to it later

        /*float ClearColorGBuffer[4] = { 0.0f, 0.0f, 0.0f, 0.0f };
        for( int i = 0; i < CurrentGuiState.m_nNumGBufferRenderTargets; i++ )
        {
            pd3dImmediateContext->ClearRenderTargetView( m_pGBufferRTV[i], ClearColorGBuffer );
        }
        pd3dImmediateContext->ClearDepthStencilView( DepthStencilBufferForOpaque.m_pDepthStencilView, D3D11_CLEAR_DEPTH, 0.0f, 0 );  // we are using inverted depth, so clear to zero

        if( CurrentGuiState.m_bTransparentObjectsEnabled )
        {
            pd3dImmediateContext->ClearDepthStencilView( DepthStencilBufferForTransparency.m_pDepthStencilView, D3D11_CLEAR_DEPTH, 0.0f, 0 );  // we are using inverted depth, so clear to zero
            CommonUtil.SortTransparentObjects(Scene.m_pCamera->GetEyePt());
        }

        bool bMSAAEnabled = ( CurrentGuiState.m_uMSAASampleCount > 1 );
        bool bShadowsEnabled = ( CurrentGuiState.m_nLightingMode == LIGHTING_SHADOWS ) && CurrentGuiState.m_bShadowsEnabled;
        bool bVPLsEnabled = ( CurrentGuiState.m_nLightingMode == LIGHTING_SHADOWS ) && CurrentGuiState.m_bVPLsEnabled;
        bool bDebugDrawingEnabled = ( CurrentGuiState.m_nDebugDrawType == DEBUG_DRAW_RADAR_COLORS ) || ( CurrentGuiState.m_nDebugDrawType == DEBUG_DRAW_GRAYSCALE );

        // Light culling compute shader
        ID3D11ComputeShader* pLightCullCS = bDebugDrawingEnabled ? GetDebugDrawNumLightsPerTileCS( CurrentGuiState.m_uMSAASampleCount, CurrentGuiState.m_nDebugDrawType, bVPLsEnabled ) : GetLightCullAndShadeCS( CurrentGuiState.m_uMSAASampleCount, CurrentGuiState.m_nNumGBufferRenderTargets, bShadowsEnabled, bVPLsEnabled );
        ID3D11ShaderResourceView* pDepthSRV = DepthStencilBufferForOpaque.m_pDepthStencilSRV;

        // Light culling compute shader for transparent objects
        ID3D11ComputeShader* pLightCullCSForTransparency = CommonUtil.GetLightCullCSForBlendedObjects(CurrentGuiState.m_uMSAASampleCount);
        ID3D11ShaderResourceView* pDepthSRVForTransparency = DepthStencilBufferForTransparency.m_pDepthStencilSRV;

        // Switch off alpha blending
        float BlendFactor[1] = { 0.0f };
        pd3dImmediateContext->OMSetBlendState( m_pBlendStateOpaque, BlendFactor, 0xffffffff );

        // Render objects here...
        {
            ID3D11RenderTargetView* pNULLRTVs[MAX_NUM_GBUFFER_RENDER_TARGETS] = { NULL, NULL, NULL, NULL, NULL };
            ID3D11DepthStencilView* pNULLDSV = NULL;
            ID3D11ShaderResourceView* pNULLSRV = NULL;
            ID3D11ShaderResourceView* pNULLSRVs[MAX_NUM_GBUFFER_RENDER_TARGETS] = { NULL, NULL, NULL, NULL, NULL };
            ID3D11UnorderedAccessView* pNULLUAV = NULL;
            ID3D11SamplerState* pNULLSampler = NULL;

            TIMER_Begin( 0, L"Core algorithm" );

            TIMER_Begin( 0, L"G-Buffer" );
            {
                // Set render targets to GBuffer RTs
                ID3D11RenderTargetView* pRTViews[MAX_NUM_GBUFFER_RENDER_TARGETS] = { NULL, NULL, NULL, NULL, NULL };
                for( int i = 0; i < CurrentGuiState.m_nNumGBufferRenderTargets; i++ )
                {
                    pRTViews[i] = m_pGBufferRTV[i];
                }
                pd3dImmediateContext->OMSetRenderTargets( (unsigned)CurrentGuiState.m_nNumGBufferRenderTargets, pRTViews, DepthStencilBufferForOpaque.m_pDepthStencilView );
                pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DEPTH_GREATER), 0x00 );  // we are using inverted 32-bit float depth for better precision
                pd3dImmediateContext->IASetInputLayout( m_pLayoutDeferredBuildGBuffer11 );
                pd3dImmediateContext->VSSetShader( m_pSceneDeferredBuildGBufferVS, NULL, 0 );
                pd3dImmediateContext->PSSetShader( m_pSceneDeferredBuildGBufferPS[CurrentGuiState.m_nNumGBufferRenderTargets-2], NULL, 0 );
                pd3dImmediateContext->PSSetSamplers( 0, 1, CommonUtil.GetSamplerStateParam(SAMPLER_STATE_ANISO) );

                // Draw the grid objects (i.e. the "lots of triangles" system)
                for( int i = 0; i < CurrentGuiState.m_nNumGridObjects; i++ )
                {
                    CommonUtil.DrawGrid(i, CurrentGuiState.m_nGridObjectTriangleDensity);
                }

                // Draw the main scene
                Scene.m_pSceneMesh->Render( pd3dImmediateContext, 0, 1 );

                // Draw the alpha test geometry
                if( bMSAAEnabled )
                {
                    pd3dImmediateContext->OMSetBlendState( m_pBlendStateAlphaToCoverage, BlendFactor, 0xffffffff );
                }
                pd3dImmediateContext->RSSetState( CommonUtil.GetRasterizerState(RASTERIZER_STATE_DISABLE_CULLING) );
                pd3dImmediateContext->PSSetShader( m_pSceneDeferredBuildGBufferPS[(MAX_NUM_GBUFFER_RENDER_TARGETS-1) + (CurrentGuiState.m_nNumGBufferRenderTargets-2)], NULL, 0 );
                Scene.m_pAlphaMesh->Render( pd3dImmediateContext, 0, 1 );
                pd3dImmediateContext->RSSetState( NULL );
                if( bMSAAEnabled )
                {
                    pd3dImmediateContext->OMSetBlendState( m_pBlendStateOpaque, BlendFactor, 0xffffffff );
                }

                if( CurrentGuiState.m_bTransparentObjectsEnabled )
                {
                    ID3D11RenderTargetView* pNULLRTV = NULL;
                    pd3dImmediateContext->OMSetRenderTargets( 1, &pNULLRTV, DepthStencilBufferForTransparency.m_pDepthStencilView );  // depth buffer for blended objects
                    // depth-only rendering of the transparent objects,
                    // render them as if they were opaque, to fill the second depth buffer
                    CommonUtil.RenderTransparentObjects(CurrentGuiState.m_nDebugDrawType, false, false, true);
                }
            }
            TIMER_End(); // G-Buffer

            TIMER_Begin( 0, L"Cull and light" );
            {
                // Cull lights and do lighting on the GPU, using a single Compute Shader
                pd3dImmediateContext->OMSetRenderTargets( (unsigned)CurrentGuiState.m_nNumGBufferRenderTargets, pNULLRTVs, pNULLDSV );  // null color buffers and depth-stencil
                pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST), 0x00 );
                pd3dImmediateContext->VSSetShader( NULL, NULL, 0 );  // null vertex shader
                pd3dImmediateContext->PSSetShader( NULL, NULL, 0 );  // null pixel shader
                pd3dImmediateContext->PSSetShaderResources( 0, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 1, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetSamplers( 0, 1, &pNULLSampler );
                pd3dImmediateContext->CSSetShader( pLightCullCS, NULL, 0 );
                pd3dImmediateContext->CSSetShaderResources( 0, 1, LightUtil.GetPointLightBufferCenterAndRadiusSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->CSSetShaderResources( 1, 1, LightUtil.GetSpotLightBufferCenterAndRadiusSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->CSSetShaderResources( 2, 1, RSMRenderer.GetVPLBufferCenterAndRadiusSRVParam() );
                pd3dImmediateContext->CSSetShaderResources( 3, 1, &pDepthSRV );
                pd3dImmediateContext->CSSetShaderResources( 4, 1, LightUtil.GetPointLightBufferColorSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->CSSetShaderResources( 5, 1, LightUtil.GetSpotLightBufferColorSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->CSSetShaderResources( 6, 1, LightUtil.GetSpotLightBufferSpotParamsSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->CSSetShaderResources( 7, 1, RSMRenderer.GetVPLBufferDataSRVParam() );
                pd3dImmediateContext->CSSetShaderResources( 8, (unsigned)CurrentGuiState.m_nNumGBufferRenderTargets, &m_pGBufferSRV[0] );
                pd3dImmediateContext->CSSetUnorderedAccessViews( 0, 1,  &m_pOffScreenBufferUAV, NULL );

                if( bShadowsEnabled )
                {
                    pd3dImmediateContext->CSSetSamplers( 1, 1, CommonUtil.GetSamplerStateParam(SAMPLER_STATE_SHADOW) );
                    pd3dImmediateContext->CSSetShaderResources( 13, 1, ShadowRenderer.GetPointAtlasSRVParam() );
                    pd3dImmediateContext->CSSetShaderResources( 14, 1, ShadowRenderer.GetSpotAtlasSRVParam() );
                }

                pd3dImmediateContext->Dispatch(CommonUtil.GetNumTilesX(),CommonUtil.GetNumTilesY(),1);

                if( bShadowsEnabled )
                {
                    pd3dImmediateContext->CSSetSamplers( 1, 1, &pNULLSampler );
                    pd3dImmediateContext->CSSetShaderResources( 13, 1, &pNULLSRV );
                    pd3dImmediateContext->CSSetShaderResources( 14, 1, &pNULLSRV );
                }

                pd3dImmediateContext->CSSetShaderResources( 4, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 5, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 6, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 7, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 8, (unsigned)CurrentGuiState.m_nNumGBufferRenderTargets, &pNULLSRVs[0] );

                if( CurrentGuiState.m_bTransparentObjectsEnabled )
                {
                    pd3dImmediateContext->CSSetShader( pLightCullCSForTransparency, NULL, 0 );
                    pd3dImmediateContext->CSSetShaderResources( 4, 1, &pDepthSRVForTransparency );
                    pd3dImmediateContext->CSSetUnorderedAccessViews( 0, 1,  CommonUtil.GetLightIndexBufferForBlendedObjectsUAVParam(), NULL );
                    pd3dImmediateContext->CSSetUnorderedAccessViews( 1, 1,  CommonUtil.GetSpotIndexBufferForBlendedObjectsUAVParam(), NULL );
                    pd3dImmediateContext->Dispatch(CommonUtil.GetNumTilesX(),CommonUtil.GetNumTilesY(),1);
                    pd3dImmediateContext->CSSetShaderResources( 4, 1, &pNULLSRV );
                    pd3dImmediateContext->CSSetUnorderedAccessViews( 1, 1, &pNULLUAV, NULL );
                }

                pd3dImmediateContext->CSSetShader( NULL, NULL, 0 );
                pd3dImmediateContext->CSSetShaderResources( 0, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 1, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 2, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetShaderResources( 3, 1, &pNULLSRV );
                pd3dImmediateContext->CSSetUnorderedAccessViews( 0, 1, &pNULLUAV, NULL );
            }
            TIMER_End(); // Cull and light

            TIMER_End(); // Core algorithm

            TIMER_Begin( 0, L"Blit to main RT" );
            {
                ID3D11RenderTargetView* pRTV = DXUTGetD3D11RenderTargetView();
                pd3dImmediateContext->OMSetRenderTargets( 1, &pRTV, pNULLDSV );
                pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DISABLE_DEPTH_TEST), 0x00 );

                // Set the input layout
                pd3dImmediateContext->IASetInputLayout( NULL );

                // Set vertex buffer
                UINT stride = 0;
                UINT offset = 0;
                ID3D11Buffer* pBuffer[1] = { NULL };
                pd3dImmediateContext->IASetVertexBuffers( 0, 1, pBuffer, &stride, &offset );

                // Set primitive topology
                pd3dImmediateContext->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP);

                pd3dImmediateContext->VSSetShader( CommonUtil.GetFullScreenVS(), NULL, 0 );
                pd3dImmediateContext->PSSetShader( CommonUtil.GetFullScreenPS(1), NULL, 0 );
                pd3dImmediateContext->PSSetSamplers( 0, 1, CommonUtil.GetSamplerStateParam(SAMPLER_STATE_LINEAR) );
                pd3dImmediateContext->PSSetShaderResources( 0, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 1, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 2, 1, &m_pOffScreenBufferSRV );

                // Draw fullscreen quad
                pd3dImmediateContext->Draw(3,0);

                // restore to default
                pd3dImmediateContext->PSSetShaderResources( 2, 1, &pNULLSRV );
            }
            TIMER_End(); // Blit to main RT

            TIMER_Begin( 0, L"Forward transparency" );
            if( CurrentGuiState.m_bTransparentObjectsEnabled )
            {
                if( !bDebugDrawingEnabled )
                {
                    pd3dImmediateContext->RSSetState( CommonUtil.GetRasterizerState(RASTERIZER_STATE_DISABLE_CULLING) );
                    pd3dImmediateContext->OMSetBlendState( m_pBlendStateAlpha, BlendFactor, 0xffffffff );
                }

                ID3D11RenderTargetView* pRTV = DXUTGetD3D11RenderTargetView();
                pd3dImmediateContext->OMSetRenderTargets( 1, &pRTV, DepthStencilBufferForOpaque.m_pDepthStencilView );
                pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DEPTH_GREATER_AND_DISABLE_DEPTH_WRITE), 0x00 );  // we are using inverted 32-bit float depth for better precision
                pd3dImmediateContext->PSSetShaderResources( 2, 1, LightUtil.GetPointLightBufferCenterAndRadiusSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->PSSetShaderResources( 3, 1, LightUtil.GetPointLightBufferColorSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->PSSetShaderResources( 4, 1, CommonUtil.GetLightIndexBufferForBlendedObjectsSRVParam() );
                pd3dImmediateContext->PSSetShaderResources( 5, 1, LightUtil.GetSpotLightBufferCenterAndRadiusSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->PSSetShaderResources( 6, 1, LightUtil.GetSpotLightBufferColorSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->PSSetShaderResources( 7, 1, LightUtil.GetSpotLightBufferSpotParamsSRVParam(CurrentGuiState.m_nLightingMode) );
                pd3dImmediateContext->PSSetShaderResources( 8, 1, CommonUtil.GetSpotIndexBufferForBlendedObjectsSRVParam() );

                if( bShadowsEnabled )
                {
                    pd3dImmediateContext->PSSetSamplers( 1, 1, CommonUtil.GetSamplerStateParam(SAMPLER_STATE_SHADOW) );
                    pd3dImmediateContext->PSSetShaderResources( 13, 1, ShadowRenderer.GetPointAtlasSRVParam() );
                    pd3dImmediateContext->PSSetShaderResources( 14, 1, ShadowRenderer.GetSpotAtlasSRVParam() );
                }

                CommonUtil.RenderTransparentObjects(CurrentGuiState.m_nDebugDrawType, bShadowsEnabled, bVPLsEnabled, false);

                if( !bDebugDrawingEnabled )
                {
                    pd3dImmediateContext->RSSetState( NULL );
                    pd3dImmediateContext->OMSetBlendState( m_pBlendStateOpaque, BlendFactor, 0xffffffff );
                }

                // restore to default
                pd3dImmediateContext->PSSetShaderResources( 2, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 3, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 4, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 5, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 6, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 7, 1, &pNULLSRV );
                pd3dImmediateContext->PSSetShaderResources( 8, 1, &pNULLSRV );
                pd3dImmediateContext->OMSetDepthStencilState( CommonUtil.GetDepthStencilState(DEPTH_STENCIL_STATE_DEPTH_GREATER), 0x00 );  // we are using inverted 32-bit float depth for better precision

                if( bShadowsEnabled )
                {
                    pd3dImmediateContext->PSSetSamplers( 1, 1, &pNULLSampler );
                    pd3dImmediateContext->PSSetShaderResources( 13, 1, &pNULLSRV );
                    pd3dImmediateContext->PSSetShaderResources( 14, 1, &pNULLSRV );
                }
            }
            TIMER_End(); // Forward transparency

            TIMER_Begin( 0, L"Light debug drawing" );
            {
                ID3D11RenderTargetView* pRTV = DXUTGetD3D11RenderTargetView();
                pd3dImmediateContext->OMSetRenderTargets( 1, &pRTV, DepthStencilBufferForOpaque.m_pDepthStencilView );

                // Light debug drawing
                if( CurrentGuiState.m_bLightDrawingEnabled )
                {
                    LightUtil.RenderLights( fElapsedTime, CurrentGuiState.m_uNumPointLights, CurrentGuiState.m_uNumSpotLights, CurrentGuiState.m_nLightingMode, CommonUtil );
                }
            }
            TIMER_End(); // Light debug drawing
        }*/
    }

    /** Return one of the light culling and shading compute shaders, based on MSAA settings */
    private GLSLProgram GetLightCullAndShadeCS( int uMSAASampleCount, int nNumGBufferRenderTargets, boolean bShadowsEnabled, boolean bVPLsEnabled ){
        final int nIndexMultiplierShadows = bShadowsEnabled ? 1 : 0;
        final int nIndexMultiplierVPLs = bVPLsEnabled ? 1 : 0;

        int nMSAAMode = 0;
        switch( uMSAASampleCount )
        {
            case 1: nMSAAMode = 0; break;
            case 2: nMSAAMode = 1; break;
            case 4: nMSAAMode = 2; break;
            default: assert(false); break;
        }

        return m_pLightCullAndShadeCS[(2*NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*nIndexMultiplierVPLs) + (NUM_MSAA_SETTINGS*(MAX_NUM_GBUFFER_RENDER_TARGETS-1)*nIndexMultiplierShadows) + ((MAX_NUM_GBUFFER_RENDER_TARGETS-1)*nMSAAMode) + (nNumGBufferRenderTargets-2)];
    }

    /** Return one of the lights-per-tile visualization compute shaders, based on MSAA settings */
    private GLSLProgram GetDebugDrawNumLightsPerTileCS( int uMSAASampleCount, int nDebugDrawType, boolean bVPLsEnabled ){
        if ( ( nDebugDrawType != CommonUtil.DEBUG_DRAW_RADAR_COLORS ) && ( nDebugDrawType != CommonUtil.DEBUG_DRAW_GRAYSCALE ) )
        {
            return null;
        }

        final int nIndexMultiplierDebugDrawType = ( nDebugDrawType == CommonUtil.DEBUG_DRAW_RADAR_COLORS ) ? 1 : 0;
        final int nIndexMultiplierVPLs = bVPLsEnabled ? 1 : 0;

        int nMSAAMode = 0;
        switch( uMSAASampleCount )
        {
            case 1: nMSAAMode = 0; break;
            case 2: nMSAAMode = 1; break;
            case 4: nMSAAMode = 2; break;
            default: assert(false); break;
        }

        return m_pDebugDrawNumLightsPerTileCS[(2*NUM_MSAA_SETTINGS*nIndexMultiplierVPLs) + (NUM_MSAA_SETTINGS*nIndexMultiplierDebugDrawType) + nMSAAMode];
    }

    @Override
    public void dispose() {
        for( int i = 0; i < MAX_NUM_GBUFFER_RENDER_TARGETS; i++ )
        {
            SAFE_RELEASE(m_pGBuffer[i]);
        }

        SAFE_RELEASE(m_pOffScreenBuffer);

//        SAFE_RELEASE(m_pSceneDeferredBuildGBufferVS);
//        SAFE_RELEASE(m_pLayoutDeferredBuildGBuffer11);

        for( int i = 0; i < NUM_GBUFFER_PIXEL_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pSceneDeferredBuildGBufferPS[i]);
        }

        for( int i = 0; i < NUM_DEFERRED_LIGHTING_COMPUTE_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pLightCullAndShadeCS[i]);
        }

        for( int i = 0; i < NUM_DEBUG_DRAW_COMPUTE_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pDebugDrawNumLightsPerTileCS[i]);
        }

        /*SAFE_RELEASE(m_pBlendStateOpaque);
        SAFE_RELEASE(m_pBlendStateAlphaToCoverage);
        SAFE_RELEASE(m_pBlendStateAlpha);*/
    }
}
