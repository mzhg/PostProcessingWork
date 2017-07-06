package jet.opengl.demos.gpupro.cloud;

import com.nvidia.developer.opengl.utils.BoundingBox;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureAttachDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Interface object to light and render clouds.<p></p>
 * Created by mazhen'gui on 2017/7/6.
 */

final class CCloud {
    private Texture2D     m_pDensityMap;        // density map
    private Texture2D     m_pBlurredMap;        // blurred density map
    private Texture2D     m_pShadowMap;         // shadow map

    private SSceneParamter  m_pSceneParam;        // scene parameters
    private CCloudGrid      m_grid;               // cloud grid
    CRenderDensityShader   m_densityShader;      // shader to render density
    CRenderShadowShader    m_shadowShader;       // shader to render shadow
    CCloudBlur             m_blur;               // blur shader
    CCloudPlane            m_finalCloud;         // object to render a screen cloud in the final pass
    private RenderTargets  m_fbo;

    private GLFuncProvider gl;

    void Create(SSceneParamter pSceneParam, int width, int height){
        m_pSceneParam = pSceneParam;
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_fbo = new RenderTargets();

        // create render targets of density map and blurred density map.
//        D3DVIEWPORT9 viewport;
//        HRESULT hr = pDev->GetViewport(&viewport);
//        if (FAILED(hr)) {
//            return FALSE;
//        }
//        hr = pDev->CreateTexture( viewport.Width/2, viewport.Height/2, 1,
//                D3DUSAGE_RENDERTARGET, D3DFMT_A8R8G8B8, D3DPOOL_DEFAULT, &m_pDensityMap, NULL );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
        m_pDensityMap = TextureUtils.createTexture2D(new Texture2DDesc(width/2, height/2, GLenum.GL_RGBA8), null);
//        hr = pDev->CreateTexture( viewport.Width/2, viewport.Height/2, 1,
//                D3DUSAGE_RENDERTARGET, D3DFMT_A8R8G8B8, D3DPOOL_DEFAULT, &m_pBlurredMap, NULL );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
        m_pBlurredMap = TextureUtils.createTexture2D(new Texture2DDesc(width/2, height/2, GLenum.GL_RGBA8), null);

        // create render targets of cloud shadowmap
        int nWidth = Math.max( 256, width );
        int nHeight = Math.max( 256, height );
//        hr = pDev->CreateTexture( nWidth, nHeight, 1,
//                D3DUSAGE_RENDERTARGET, D3DFMT_A8R8G8B8, D3DPOOL_DEFAULT, &m_pShadowMap, NULL );
//        if ( FAILED(hr) ) {
//            return FALSE;
//        }
        m_pShadowMap = TextureUtils.createTexture2D(new Texture2DDesc(nWidth, nHeight, GLenum.GL_DEPTH_COMPONENT16), null);

        m_grid.Create();
        m_densityShader = new CRenderDensityShader(pSceneParam);
        m_shadowShader = new CRenderShadowShader(pSceneParam);
        m_blur.Create(pSceneParam);
        m_finalCloud.Create(pSceneParam, m_pDensityMap, m_pBlurredMap);
    }

    void Delete(){
        CommonUtil.safeRelease(m_pBlurredMap);
        CommonUtil.safeRelease(m_pDensityMap);
    }

    void Update(float dt, BoundingBox bbGround){
        // animate uv
        m_grid.Update( dt, m_pSceneParam );

        // compute transform matrix for shadowmap
        m_shadowShader.Update( bbGround, m_grid.GetBoundingBox() );
    }

    //--------------------------------------------------------------------------------------
    // Render clouds into redertargets before scene rendering
    //  Cloud shadowmap, densitymap are rendered and then the density map is blurred.
    //--------------------------------------------------------------------------------------
    void PrepareCloudTextures(){
        // preserve current render target
//        HRESULT hr;
//        LPDIRECT3DSURFACE9 pCurrentSurface;
//        hr = pDev->GetRenderTarget( 0, &pCurrentSurface );
//        if ( FAILED(hr) ) {
//            return;
//        }

        // Setup render states.
        // All passes in this function do not require a depth buffer and alpha blending
        //  because there is no multiple clouds in this demo.
//        pDev->SetRenderState(D3DRS_ALPHABLENDENABLE, FALSE);
//        pDev->SetRenderState(D3DRS_ALPHATESTENABLE, FALSE);
//        pDev->SetRenderState( D3DRS_ZWRITEENABLE, FALSE );
//        pDev->SetRenderState( D3DRS_ZENABLE, FALSE );
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_CULL_FACE);

        // Pass 1 : Render clouds to a shadow map
        if ( SetRenderTarget( m_pShadowMap ) ) {
            // Clouds are always far away so shadowmap of clouds does not have to have depth.
            // Only transparency is stored to the shadowmap.
//            pDev->Clear( 0, NULL, D3DCLEAR_TARGET, 0xFFFFFF, 1.0f, 0 );
            gl.glClearBufferfv(GLenum.GL_DEPTH, 0, CacheBuffer.wrap(1.0f));
            gl.glEnable(GLenum.GL_DEPTH_TEST);
            if (m_shadowShader.Begin(m_grid)) {
                // Since the cloud grid is viewed from outside, reverse cullmode.
//                pDev->SetRenderState( D3DRS_CULLMODE, D3DCULL_CW );
                m_grid.Draw(  );
//                m_shadowShader.End();
                // restore
//                pDev->SetRenderState( D3DRS_CULLMODE, D3DCULL_CCW );
            }

            gl.glDisable(GLenum.GL_DEPTH_TEST);
            // Pass 2 : Render cloud density
            if ( SetRenderTarget( m_pDensityMap ) ) {
//                pDev->Clear( 0, NULL, D3DCLEAR_TARGET, 0, 1.0f, 0 );
                gl.glClearBufferfv(GLenum.GL_COLOR, 0, CacheBuffer.wrap(0.0f, 0.0f, 0.0f, 0.0f));
                if (m_densityShader.Begin(m_grid)) {
                    m_grid.Draw(  );
//                    m_densityShader.end();
                }

                // Pass 3 : Blur the density map
                if ( SetRenderTarget(  m_pBlurredMap ) ) {
                    m_blur.Blur(  m_pDensityMap );
                }
            }
        }

        // restore render target and render states.
//        pDev->SetRenderState( D3DRS_ZWRITEENABLE, TRUE );
//        pDev->SetRenderState( D3DRS_ZENABLE, TRUE );
//        pDev->SetRenderTarget( 0, pCurrentSurface );
//
//        pCurrentSurface->Release();
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
    }

    void DrawFinalQuad(){
        m_finalCloud.Draw();
    }

    void SetCloudCover(float fCloudCover) { m_grid.SetCloudCover( fCloudCover );}
    float GetCurrentCloudCover() { return m_grid.GetCurrentCloudCover();}
    Matrix4f GetWorld2ShadowMatrix() { return m_shadowShader.GetW2ShadowMapMatrix();}
    Texture2D GetShadowMap() { return m_pShadowMap;}

    boolean SetRenderTarget(Texture2D tex){
        m_fbo.bind();
        m_fbo.setRenderTexture(tex, new TextureAttachDesc());
        gl.glViewport(0,0, tex.getWidth(), tex.getHeight());

        return true;
    }
}
