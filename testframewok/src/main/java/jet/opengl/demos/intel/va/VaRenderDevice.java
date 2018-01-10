package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector2i;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2018/1/9.
 */

public abstract class VaRenderDevice implements Disposeable{

    protected int                    m_renderFrameCounter;

    protected VaTexture              m_mainColor;    // aka m_swapChainColor
    protected VaTexture              m_mainDepth;    // aka m_swapChainDepth
    protected VaRenderDeviceContext  m_mainDeviceContext;

    protected boolean                m_profilingEnabled;

    public VaRenderDevice( ){
        m_profilingEnabled = true;
    }

    public abstract void                    OnAppAboutToQuit( );

    public abstract void                    CreateSwapChain( int width, int height, boolean windowed);
    /** return true if actually resized (for further handling) */
    public abstract boolean                 ResizeSwapChain( int width, int height, boolean windowed );

    public VaTexture                        GetMainChainColor( )                                                  { return m_mainColor; }
    public VaTexture                        GetMainChainDepth( )                                                  { return m_mainDepth; }

    public abstract boolean                 IsFullscreen( );
    public abstract boolean                 IsSwapChainCreated( );

    public abstract void                    FindClosestFullscreenMode(Vector2i screenSize);

    // main
    public abstract void                    BeginFrame( float deltaTime );
    public abstract void                    EndAndPresentFrame( /*int vsyncInterval = 0*/ );

    public int                              GetFrameCount( )                                                      { return m_renderFrameCounter; }
//        const vaViewport &              GetMainViewport( ) const                                                    { assert( m_mainCanvas != NULL ); return m_mainCanvas->GetViewport(); }

    // void                           SetMainRenderTargetToImmediateContext( );
    // void                           SetMainViewProjTransform( vaMatrix4x4 & mainViewProj );   // necessary for Canvas3D to work

    public abstract void                    DrawDebugCanvas2D( );
    public abstract void                    DrawDebugCanvas3D( VaDrawContext drawContext ) ;

    public VaRenderDeviceContext            GetMainContext( ) { return m_mainDeviceContext; }

//    virtual vaDebugCanvas2DBase *   GetCanvas2D( )                                                              = 0;
//    virtual vaDebugCanvas3DBase *   GetCanvas3D( )                                                              = 0;


    public boolean                          IsProfilingEnabled( ) { return m_profilingEnabled; }

    public abstract void                    RecompileFileLoadedShaders( );

//    virtual wstring                 GetAdapterNameShort( )                                                      = 0;
//    virtual wstring                 GetAdapterNameID( )                                                         = 0;

    void CleanupAPIDependencies( ){
        VaAssetPackManager.g_Instance = null;
        VaRenderMeshManager.g_Instance = null;
        VaRenderMaterialManager.g_Instance = null;
    }
}
