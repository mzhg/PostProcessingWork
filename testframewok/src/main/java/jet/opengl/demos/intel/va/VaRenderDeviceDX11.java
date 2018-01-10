package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector2i;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2018/1/9.
 */

public final class VaRenderDeviceDX11 extends VaRenderDevice{

//    IDXGIFactory1 *             m_DXGIFactory;
//    ID3D11Device *              m_device;
//    ID3D11DeviceContext *       m_deviceImmediateContext;
//    IDXGISwapChain *            m_swapChain;
//    IDXGIOutput *               m_mainOutput;

    private Texture2D m_mainRenderTargetView;
    private final Texture2DDesc m_backbufferTextureDesc = new Texture2DDesc();

    private Texture2D           m_mainDepthStencil;
    private Texture2D           m_mainDepthStencilView;
    private Texture2D           m_mainDepthSRV;

    private int       m_framebuffer;

//    int64                       m_renderFrameCounter;

//    vaApplication *             m_application;

//    vaDebugCanvas2DDX11         m_canvas2D;
//    vaDebugCanvas3DDX11         m_canvas3D;

    public VaRenderDeviceDX11(){
        boolean initialized = Initialize();
        assert( initialized );

//        vaDirectXFont::InitializeFontGlobals( );
    }

    private boolean Initialize( ){
        m_backbufferTextureDesc.sampleCount = 1;
        m_backbufferTextureDesc.arraySize = 1;
        m_backbufferTextureDesc.format = GLenum.GL_RGBA8;

        // main canvas
        {
            m_mainDeviceContext = VaRenderDeviceContextDX11.Create( /*m_deviceImmediateContext*/ );
        }

        return true;
    }

    private void Deinitialize( ){
        ReleaseSwapChainRelatedObjects( );

        // have to release these first!
        m_mainColor = null;
        m_mainDepth = null;
        m_mainDeviceContext = null;

        VaDirectXCore.GetInstance( ).PostDeviceDestroyed( );

        /*SAFE_RELEASE( m_mainOutput );
        SAFE_RELEASE( m_deviceImmediateContext );
        SAFE_RELEASE( m_swapChain );
        SAFE_RELEASE( m_device );
        SAFE_RELEASE( m_DXGIFactory );*/
    }

    @Override
    public void OnAppAboutToQuit() {

    }

    @Override
    public void CreateSwapChain(int width, int height, boolean windowed) {
        m_backbufferTextureDesc.width = width;
        m_backbufferTextureDesc.height = height;

        /*desc.BufferDesc.Format = c_DefaultBackbufferFormat;
        desc.BufferDesc.Width = width;
        desc.BufferDesc.Height = height;
        desc.BufferDesc.RefreshRate.Numerator = 1;
        desc.BufferDesc.RefreshRate.Denominator = 60;
        desc.BufferDesc.Scaling = DXGI_MODE_SCALING_UNSPECIFIED;
        desc.BufferDesc.ScanlineOrdering = DXGI_MODE_SCANLINE_ORDER_UNSPECIFIED;

        desc.SampleDesc.Count = 1;
        desc.SampleDesc.Quality = 0;

        desc.BufferCount = c_DefaultBackbufferCount;
        desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT | DXGI_USAGE_BACK_BUFFER | DXGI_USAGE_SHADER_INPUT; //DXGI_USAGE_UNORDERED_ACCESS
        desc.OutputWindow = hwnd;
        desc.Flags = c_DefaultSwapChainFlags;
        desc.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;

        desc.Windowed = windowed;

        if( !windowed )
        {
            DXGI_MODE_DESC closestMatch;
            memset( &closestMatch, 0, sizeof( closestMatch ) );

            HRESULT hr = m_mainOutput->FindClosestMatchingMode( &desc.BufferDesc, &closestMatch, m_device );
            if( FAILED( hr ) )
            {
                VA_ERROR( L"Error trying to find closest matching display mode" );
            }
            desc.BufferDesc = closestMatch;
        }

        //IDXGISwapChain * pSwapChain = NULL;
        HRESULT hr = m_DXGIFactory->CreateSwapChain( m_device, &desc, &m_swapChain ); //&pSwapChain );
        if( FAILED( hr ) )
        {
            VA_ERROR( L"Error trying to create D3D11 swap chain" );
        }

        // stop automatic alt+enter, we'll handle it manually
        {
            hr = m_DXGIFactory->MakeWindowAssociation( hwnd, DXGI_MWA_NO_ALT_ENTER );
        }

        //if( FAILED( d3dResource->QueryInterface( __uuidof( IDXGISwapChain1 ), (void**)&m_swapChain ) ) )
        //{
        //   VA_ERROR( L"Error trying to cast into IDXGISwapChain1" );
        //}
        //SAFE_RELEASE( pSwapChain );

        //   m_swapChain->GetBuffer

        // Broadcast that the device was created!
        vaDirectXCore::GetInstance( ).PostDeviceCreated( m_device, m_swapChain );

        vaGPUTimerDX11::OnDeviceAndContextCreated( m_device );

#ifdef VA_IMGUI_INTEGRATION_ENABLED
        ImGui_ImplDX11_Init( hwnd, m_device, m_deviceImmediateContext );
#endif*/

        CreateSwapChainRelatedObjects( );

        LogUtil.i(LogUtil.LogType.DEFAULT, "DirectX 11 device and swap chain created");
    }

    private void CreateSwapChainRelatedObjects(){
        // Create render target view
        {
            // Get the back buffer and desc
            /*ID3D11Texture2D* pBackBuffer = NULL;
            hr = m_swapChain->GetBuffer( 0, __uuidof( *pBackBuffer ), (LPVOID*)&pBackBuffer );
            if( FAILED( hr ) )
            {
                VA_ERROR( L"Error trying to get back buffer texture" );
            }
            pBackBuffer->GetDesc( &m_backbufferTextureDesc );

            m_mainRenderTargetView = vaDirectXTools::CreateRenderTargetView( pBackBuffer );

            m_mainColor = std::shared_ptr< vaTexture >( vaTextureDX11::CreateWrap( pBackBuffer ) );*/
            m_mainRenderTargetView = TextureUtils.createTexture2D(m_backbufferTextureDesc, null);
            m_mainColor = VaTextureDX11.CreateWrap(m_mainRenderTargetView);

//            SAFE_RELEASE( pBackBuffer );
        }

        // Create depth-stencil
        {
            /*m_mainDepthStencil = vaDirectXTools::CreateTexture2D( DXGI_FORMAT_R32G8X24_TYPELESS, m_backbufferTextureDesc.Width, m_backbufferTextureDesc.Height, NULL, 1, 0, D3D11_BIND_DEPTH_STENCIL | D3D11_BIND_SHADER_RESOURCE );
            m_mainDepthStencilView = vaDirectXTools::CreateDepthStencilView( m_mainDepthStencil, DXGI_FORMAT_D32_FLOAT_S8X24_UINT );
            m_mainDepthSRV = vaDirectXTools::CreateShaderResourceView( m_mainDepthStencil, DXGI_FORMAT_R32_FLOAT_X8X24_TYPELESS );*/
            Texture2DDesc depthDesc = new Texture2DDesc(m_backbufferTextureDesc);
            depthDesc.format = GLenum.GL_DEPTH24_STENCIL8;
            m_mainDepthStencil = m_mainDepthStencilView = m_mainDepthSRV = TextureUtils.createTexture2D(depthDesc, null);

            m_mainDepth = //std::shared_ptr< vaTexture >( vaTextureDX11::CreateWrap( m_mainDepthStencil, vaTextureFormat::R32_FLOAT_X8X24_TYPELESS, vaTextureFormat::Unknown, vaTextureFormat::D32_FLOAT_S8X24_UINT ) );
                        VaTextureDX11.CreateWrap(m_mainDepthStencil);
            //delete vaTexture::CreateView( m_mainDepth, vaTextureBindSupportFlags::ShaderResource, vaTextureFormat::R32_FLOAT_X8X24_TYPELESS );
        }

        // Create framebuffer
        {
            if(m_framebuffer == 0){
                final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
                m_framebuffer = gl.glGenFramebuffer();
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_framebuffer);
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_mainRenderTargetView.getTarget(), m_mainRenderTargetView.getTexture(), 0);
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, m_mainDepthStencil.getTarget(), m_mainDepthStencil.getTexture(), 0);
                gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            }
        }

        /*DXGI_SWAP_CHAIN_DESC scdesc;
        m_swapChain->GetDesc( &scdesc );

        DXGI_SURFACE_DESC sdesc;
        sdesc.Width = scdesc.BufferDesc.Width;
        sdesc.Height = scdesc.BufferDesc.Height;
        sdesc.SampleDesc = scdesc.SampleDesc;
        sdesc.Format = scdesc.BufferDesc.Format;

        m_mainDeviceContext->SetRenderTarget( m_mainColor, m_mainDepth, true );*/

        //m_mainViewport.X = 0;
        //m_mainViewport.Y = 0;
        //m_mainViewport.Width = scdesc.BufferDesc.Width;
        //m_mainViewport.Height = scdesc.BufferDesc.Height;

        VaDirectXCore. GetInstance( ).PostResizedSwapChain( /*sdesc*/ m_backbufferTextureDesc.width, m_backbufferTextureDesc.height );

/*#ifdef VA_IMGUI_INTEGRATION_ENABLED
        ImGui_ImplDX11_CreateDeviceObjects();
#endif*/
    }

    private void ReleaseSwapChainRelatedObjects(){
//#ifdef VA_IMGUI_INTEGRATION_ENABLED
//        ImGui_ImplDX11_InvalidateDeviceObjects();
//#endif

        VaDirectXCore.GetInstance( ).PostReleasingSwapChain( );

        SAFE_RELEASE( m_mainRenderTargetView );
        SAFE_RELEASE( m_mainDepthStencil );
        SAFE_RELEASE( m_mainDepthStencilView );
        SAFE_RELEASE( m_mainDepthSRV );

//        m_mainDeviceContext->SetRenderTarget( NULL, NULL, false );
        m_mainColor = null;
        m_mainDepth = null;
    }

    @Override
    public boolean ResizeSwapChain(int width, int height, boolean windowed) {
        if( m_backbufferTextureDesc.width == width && m_backbufferTextureDesc.height == height /*&& windowed == !IsFullscreen( )*/ ) return false;

        if( m_backbufferTextureDesc.width != width || m_backbufferTextureDesc.height != height )
        {
            m_backbufferTextureDesc.width = width;
            m_backbufferTextureDesc.height = height;
            ReleaseSwapChainRelatedObjects( );

            /*HRESULT hr = m_swapChain->ResizeBuffers( c_DefaultBackbufferCount, width, height, DXGI_FORMAT_UNKNOWN, c_DefaultSwapChainFlags );
            if( FAILED( hr ) )
            {
                assert( false );
                //VA_ERROR( L"Error trying to m_swapChain->ResizeBuffers" );
                return false;
            }*/

            CreateSwapChainRelatedObjects( );
            return true;
        }

        //if( windowed != !IsFullscreen( ) )
        //{
        //    HRESULT hr = m_swapChain->SetFullscreenState( !windowed, NULL );
        //    if( FAILED( hr ) )
        //    {
        //        //VA_ERROR( L"Error trying to m_swapChain->SetFullscreenState" );
        //    }
        //
        //    IsFullscreen( );
        //    return true;
        //}
        return true;
    }

    public void SetMainRenderTargetToImmediateContext(/*RenderTargets renderTargets*/)
    {
//        m_mainDeviceContext->SetRenderTarget( m_mainColor, m_mainDepth, true );
//        renderTargets.setRenderTextures(CommonUtil.toArray(m_mainRenderTargetView, m_mainDepthStencilView), null);
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_framebuffer);
        gl.glViewport(0,0,m_backbufferTextureDesc.width, m_backbufferTextureDesc.height);
    }

    @Override
    public boolean IsFullscreen() {
        return false;
    }

    @Override
    public boolean IsSwapChainCreated() {
        return true;
    }

    @Override
    public void FindClosestFullscreenMode(Vector2i screenSize) {

    }

    @Override
    public void BeginFrame(float deltaTime) {
        m_renderFrameCounter++;

       /* if( (vaInputKeyboardBase::GetCurrent() != NULL) && vaInputKeyboardBase::GetCurrent()->IsKeyDown( KK_CONTROL ) )
        {
            if( vaInputKeyboardBase::GetCurrent()->IsKeyClicked( ( vaKeyboardKeys )'R' ) )
            vaDirectXShaderManager::GetInstance( ).RecompileFileLoadedShaders( );
        }*/

        VaDirectXCore.GetInstance( ).TickInternal( );

        SetMainRenderTargetToImmediateContext( );

//    // maybe not needed here?
//    vaDirectXTools::ClearColorDepthStencil( m_deviceImmediateContext, true, true, true, vaVector4( 0.0f, 0.5f, 0.0f, 0.0f ), 1.0f, 0 );

//        vaGPUTimerDX11::OnFrameStart();
    }

    @Override
    public void EndAndPresentFrame() {
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_framebuffer);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0,0,m_backbufferTextureDesc.width,m_backbufferTextureDesc.height,
                0,0,m_backbufferTextureDesc.width,m_backbufferTextureDesc.height,
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
    }

    @Override
    public void DrawDebugCanvas2D() {

    }

    @Override
    public void DrawDebugCanvas3D(VaDrawContext drawContext) {

    }

    @Override
    public void RecompileFileLoadedShaders() {
        VaDirectXShaderManager.GetInstance( ).RecompileFileLoadedShaders( );
    }

    @Override
    public void dispose() {
        CleanupAPIDependencies( );

        //assert( s_mainDevice == this );
        //s_mainDevice = NULL;
//        vaDirectXFont::DeinitializeFontGlobals( );

        Deinitialize( );
    }
}
