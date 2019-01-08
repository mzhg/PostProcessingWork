package jet.opengl.demos.amdfx.tiledrendering;

import org.lwjgl.util.vector.Matrix4f;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

final class ShadowRenderer implements Disposeable, ICONST {

    interface  RenderSceneCallback{
        void onRender();
    }

    private UpdateCameraCallback        m_CameraCallback;
    private RenderSceneCallback         m_RenderCallback;

    private Texture2D     m_pPointAtlasTexture;
    private Texture2D     m_pPointAtlasView;
    private Texture2D     m_pPointAtlasSRV;

    private Texture2D     m_pSpotAtlasTexture;
    private Texture2D     m_pSpotAtlasView;
    private Texture2D     m_pSpotAtlasSRV;
    private RenderTargets m_FBO;

    private GLFuncProvider gl;

    private static final int gPointShadowResolution = 256;
    private static final int gSpotShadowResolution = 256;

    void SetCallbacks( UpdateCameraCallback cameraCallback, RenderSceneCallback renderCallback ){
        m_CameraCallback = cameraCallback;
        m_RenderCallback = renderCallback;
    }

    // Various hook functions
    void OnCreateDevice( /*ID3D11Device* pd3dDevice*/ ){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_FBO = new RenderTargets();

        Texture2DDesc desc = new Texture2DDesc(6 * gPointShadowResolution, MAX_NUM_SHADOWCASTING_POINTS * gPointShadowResolution, GLenum.GL_DEPTH_COMPONENT16);
        m_pPointAtlasSRV = m_pPointAtlasView = m_pPointAtlasTexture = TextureUtils.createTexture2D(desc, null);

        desc.width = MAX_NUM_SHADOWCASTING_SPOTS * gSpotShadowResolution;
        desc.height = gSpotShadowResolution;
        m_pSpotAtlasSRV = m_pSpotAtlasView = m_pSpotAtlasTexture = TextureUtils.createTexture2D(desc, null);
    }

    void OnDestroyDevice(){
        SAFE_RELEASE( m_pSpotAtlasSRV );
        SAFE_RELEASE( m_pSpotAtlasView );
        SAFE_RELEASE( m_pSpotAtlasTexture );

        SAFE_RELEASE( m_pPointAtlasSRV );
        SAFE_RELEASE( m_pPointAtlasView );
        SAFE_RELEASE( m_pPointAtlasTexture );
    }

    void OnResizedSwapChain( /*ID3D11Device* pd3dDevice, const DXGI_SURFACE_DESC* pBackBufferSurfaceDesc*/ int width, int height){ }
    void OnReleasingSwapChain(){}

    void RenderPointMap( int numShadowCastingPointLights ){
//        ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();

        /*D3D11_VIEWPORT oldVp[ 8 ];
        UINT numVPs = 1;
        pd3dImmediateContext->RSGetViewports( &numVPs, oldVp );*/

        IntBuffer oldVp = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, oldVp);
        int viewportWidth = oldVp.get(2);
        int viewportHeight = oldVp.get(3);

        /*D3D11_VIEWPORT vp;
        vp.Width = gPointShadowResolution;
        vp.Height = gPointShadowResolution;
        vp.MinDepth = 0.0f;
        vp.MaxDepth = 1.0f;*/

//        pd3dImmediateContext->ClearDepthStencilView( m_pPointAtlasView, D3D11_CLEAR_DEPTH, 1.0f, 0 );


        /*ID3D11RenderTargetView* pNULLRTV = NULL;
        pd3dImmediateContext->OMSetRenderTargets( 1, &pNULLRTV, m_pPointAtlasView );*/
        m_FBO.setRenderTexture(m_pPointAtlasView, null);
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0,CacheBuffer.wrap(1.f));

        final Matrix4f[][] PointLightViewProjArray = LightUtil.GetShadowCastingPointLightViewProjTransposedArray();

        for ( int p = 0; p < numShadowCastingPointLights; p++ )
        {
            float  TopLeftY = (float)p * gPointShadowResolution;

            for ( int i = 0; i < 6; i++ )
            {
                m_CameraCallback.onUpdateCamera(( PointLightViewProjArray[p][i] ));

                float TopLeftX = (float)i * gPointShadowResolution;
//                pd3dImmediateContext->RSSetViewports( 1, &vp );
                gl.glViewportIndexedf(0, TopLeftX, TopLeftY, gPointShadowResolution, gPointShadowResolution);

                m_RenderCallback.onRender();
            }
        }

//        pd3dImmediateContext->RSSetViewports( 1, oldVp );
        gl.glViewport(0,0,viewportWidth, viewportHeight);
    }

    void RenderSpotMap( int numShadowCastingSpotLights ){
        /*ID3D11DeviceContext* pd3dImmediateContext = DXUTGetD3D11DeviceContext();
        D3D11_VIEWPORT oldVp[ 8 ];
        UINT numVPs = 1;
        pd3dImmediateContext->RSGetViewports( &numVPs, oldVp );*/

        IntBuffer oldVp = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, oldVp);
        int viewportWidth = oldVp.get(2);
        int viewportHeight = oldVp.get(3);

        /*D3D11_VIEWPORT vp;
        vp.Width = gSpotShadowResolution;
        vp.Height = gSpotShadowResolution;
        vp.MinDepth = 0.0f;
        vp.MaxDepth = 1.0f;
        vp.TopLeftY = 0.0f;*/

//        pd3dImmediateContext->ClearDepthStencilView( m_pSpotAtlasView, D3D11_CLEAR_DEPTH, 1.0f, 0 );

        m_FBO.setRenderTexture(m_pSpotAtlasView, null);
        gl.glClearBufferfv(GLenum.GL_DEPTH, 0,CacheBuffer.wrap(1.f));

        /*ID3D11RenderTargetView* pNULLRTV = NULL;
        pd3dImmediateContext->OMSetRenderTargets( 1, &pNULLRTV, m_pSpotAtlasView );*/

        Matrix4f[] SpotLightViewProjArray = LightUtil.GetShadowCastingSpotLightViewProjTransposedArray();

        for ( int i = 0; i < numShadowCastingSpotLights; i++ )
        {
            float TopLeftX = (float)i * gSpotShadowResolution;

            m_CameraCallback.onUpdateCamera( SpotLightViewProjArray[i] );

//            pd3dImmediateContext->RSSetViewports( 1, &vp );
            gl.glViewportIndexedf(0, TopLeftX, 0, gPointShadowResolution, gPointShadowResolution);

            m_RenderCallback.onRender();
        }

//        pd3dImmediateContext->RSSetViewports( 1, oldVp );
        gl.glViewport(0,0,viewportWidth, viewportHeight);
    }

    Texture2D   GetPointAtlasSRVParam()  { return m_pPointAtlasSRV; }
    Texture2D   GetSpotAtlasSRVParam()  { return m_pSpotAtlasSRV; }

    @Override
    public void dispose() {

    }
}
