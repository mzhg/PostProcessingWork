package jet.opengl.demos.amdfx.tiledrendering;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.Texture2D;

final class ForwardPlusUtil implements ICONST, Disposeable {
    // shaders for Forward+
    private ShaderProgram m_pScenePositionOnlyVS;
    private ShaderProgram m_pScenePositionAndTexVS;
    private ShaderProgram m_pSceneForwardVS;
    private ShaderProgram m_pSceneAlphaTestOnlyPS;
    private Runnable          m_pLayoutPositionOnly11;
    private Runnable          m_pLayoutPositionAndTex11;
    private Runnable          m_pLayoutForward11;

    private static final int NUM_FORWARD_PIXEL_SHADERS = 2*2*2;  // alpha test on/off, shadows on/off, VPLs on/off
    private ShaderProgram[]   m_pSceneForwardPS = new ShaderProgram[NUM_FORWARD_PIXEL_SHADERS];

    // compute shaders for tiled culling
    private static final int NUM_LIGHT_CULLING_COMPUTE_SHADERS = 2*NUM_MSAA_SETTINGS;  // one for each MSAA setting,
    // times two for VPLs enabled/disabled
    private GLSLProgram[] m_pLightCullCS = new GLSLProgram[NUM_LIGHT_CULLING_COMPUTE_SHADERS];

    // state for Forward+
    private Runnable           m_pBlendStateOpaque;
    private Runnable           m_pBlendStateOpaqueDepthOnly;
    private Runnable           m_pBlendStateAlphaToCoverageDepthOnly;
    private Runnable           m_pBlendStateAlpha;

    private GLFuncProvider     gl;

    // Constructor / destructor
    ForwardPlusUtil();
        ~ForwardPlusUtil();

    void AddShadersToCache( AMD::ShaderCache *pShaderCache );
    void RenderSceneForShadowMaps( const GuiState& CurrentGuiState, const Scene& Scene, const CommonUtil& CommonUtil );

    // Various hook functions
    void OnCreateDevice( /*ID3D11Device* pd3dDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        // Create blend states
        /*D3D11_BLEND_DESC BlendStateDesc;
        ZeroMemory( &BlendStateDesc, sizeof( D3D11_BLEND_DESC ) );
        BlendStateDesc.AlphaToCoverageEnable = FALSE;
        BlendStateDesc.IndependentBlendEnable = FALSE;
        BlendStateDesc.RenderTarget[0].BlendEnable = FALSE;
        BlendStateDesc.RenderTarget[0].SrcBlend = D3D11_BLEND_ONE;
        BlendStateDesc.RenderTarget[0].DestBlend = D3D11_BLEND_ZERO;
        BlendStateDesc.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
        BlendStateDesc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ONE;
        BlendStateDesc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ZERO;
        BlendStateDesc.RenderTarget[0].BlendOpAlpha = D3D11_BLEND_OP_ADD;
        BlendStateDesc.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateOpaque ) );*/
        m_pBlendStateOpaque = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
            gl.glColorMask(true, true, true, true);
        };

        /*BlendStateDesc.RenderTarget[0].RenderTargetWriteMask = 0;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateOpaqueDepthOnly ) );*/
        m_pBlendStateOpaqueDepthOnly = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
            gl.glColorMask(false, false, false, false);
        };

        /*BlendStateDesc.AlphaToCoverageEnable = TRUE;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateAlphaToCoverageDepthOnly ) );*/
        m_pBlendStateAlphaToCoverageDepthOnly = ()->
        {
            gl.glDisable(GLenum.GL_BLEND);
            gl.glColorMask(false, false, false, false);
            gl.glEnable(GLenum.GL_SAMPLE_ALPHA_TO_COVERAGE);  // todo
        };

        /*BlendStateDesc.AlphaToCoverageEnable = FALSE;
        BlendStateDesc.RenderTarget[0].BlendEnable = TRUE;
        BlendStateDesc.RenderTarget[0].SrcBlend = D3D11_BLEND_SRC_ALPHA;
        BlendStateDesc.RenderTarget[0].DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
        BlendStateDesc.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_SRC_ALPHA;
        BlendStateDesc.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_INV_SRC_ALPHA;
        BlendStateDesc.RenderTarget[0].RenderTargetWriteMask = D3D11_COLOR_WRITE_ENABLE_ALL;
        V_RETURN( pd3dDevice->CreateBlendState( &BlendStateDesc, &m_pBlendStateAlpha ) );*/
        m_pBlendStateAlpha = ()->
        {
            gl.glEnable(GLenum.GL_BLEND);
            gl.glBlendFunc(GLenum.GL_SRC_ALPHA, GLenum.GL_ONE_MINUS_SRC_ALPHA);
        };
    }
    void OnDestroyDevice(){dispose();}
    void OnResizedSwapChain( int width, int height ){}
    void OnReleasingSwapChain();

    /**Render hook function, to draw the lights (as instanced quads)*/
    void OnRender(float fElapsedTime, /*const GuiState& CurrentGuiState,*/ Texture2D DepthStencilBufferForOpaque, Texture2D DepthStencilBufferForTransparency, Scene Scene, CommonUtil CommonUtil, const LightUtil LightUtil, ShadowRenderer ShadowRenderer,  RSMRenderer RSMRenderer );

    ID3D11PixelShader * GetScenePS( bool bAlphaTestEnabled, bool bShadowsEnabled, bool bVPLsEnabled ) const;
    ID3D11ComputeShader * GetLightCullCS( unsigned uMSAASampleCount, bool bVPLsEnabled )

    @Override
    public void dispose() {
        SAFE_RELEASE(m_pScenePositionOnlyVS);
        SAFE_RELEASE(m_pScenePositionAndTexVS);
        SAFE_RELEASE(m_pSceneForwardVS);
        SAFE_RELEASE(m_pSceneAlphaTestOnlyPS);
//        SAFE_RELEASE(m_pLayoutPositionOnly11);
//        SAFE_RELEASE(m_pLayoutPositionAndTex11);
//        SAFE_RELEASE(m_pLayoutForward11);

        for( int i = 0; i < NUM_FORWARD_PIXEL_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pSceneForwardPS[i]);
        }

        for( int i = 0; i < NUM_LIGHT_CULLING_COMPUTE_SHADERS; i++ )
        {
            SAFE_RELEASE(m_pLightCullCS[i]);
        }

//        SAFE_RELEASE(m_pBlendStateOpaque);
//        SAFE_RELEASE(m_pBlendStateOpaqueDepthOnly);
//        SAFE_RELEASE(m_pBlendStateAlphaToCoverageDepthOnly);
//        SAFE_RELEASE(m_pBlendStateAlpha);
    }
}
