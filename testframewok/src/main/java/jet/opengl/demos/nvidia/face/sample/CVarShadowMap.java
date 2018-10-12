package jet.opengl.demos.nvidia.face.sample;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/9/5.
 */

final class CVarShadowMap implements Disposeable{
    Texture2D		m_pRtv;				// Main RT, stores (z, z^2)
    Texture2D		m_pSrv;
    Texture2D		m_pRtvTemp;			// Temp RT for Gaussian blur
    Texture2D		m_pSrvTemp;
    /** Shadow map resolution*/
    int								m_size;
    /** Radius of Gaussian in UV space*/
    float							m_blurRadius = 1.0f;		//
    private GLFuncProvider gl;
    private int m_shadow_fbo;
    private CShaderManager g_shdmgr;
    private int g_fullscreen;

    CVarShadowMap(CShaderManager shdmgr, int fullscreen){
        g_shdmgr = shdmgr;
        g_fullscreen = fullscreen;

        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    void Init(int size){
        Texture2DDesc texDesc = new Texture2DDesc(size, size, GLenum.GL_RG32F);
        m_pRtv = m_pSrv = TextureUtils.createTexture2D(texDesc, null);
        m_pRtvTemp = m_pSrvTemp = TextureUtils.createTexture2D(texDesc, null);

        m_shadow_fbo = gl.glGenFramebuffer();

        m_pRtv.setName("VSM - RTV");
        m_pSrv.setName("VSM - SRV");
        m_pRtvTemp.setName("VSM - temp RTV");
        m_pSrvTemp.setName("VSM - temp SRV");
        LogUtil.i(LogUtil.LogType.DEFAULT, String.format("Created VSM, format R32G32_FLOAT, %d x %d", size, size));
        // The texture isn't needed any more
//        SAFE_RELEASE(pTex);

        m_size = size;
    }
    void UpdateFromShadowMap(CShadowMap shadow){
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_shadow_fbo);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, GLenum.GL_TEXTURE_2D, m_pRtv.getTexture(), 0);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);
        gl.glViewport(0,0, m_size, m_size);
        gl.glDisable(GLenum.GL_CULL_FACE);

        g_shdmgr.BindCreateVSM( shadow.m_pSrv);
//        g_meshFullscreen.Draw(pCtx);
        gl.glBindVertexArray(g_fullscreen);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);
    }

    void GaussianBlur(){
        gl.glViewport(0,0, m_size, m_size);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, m_shadow_fbo);
        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_pRtvTemp.getTarget(), m_pRtvTemp.getTexture(), 0);
        gl.glDrawBuffers(GLenum.GL_COLOR_ATTACHMENT0);

        g_shdmgr.BindGaussian(m_pSrv, m_blurRadius, 0.0f);
//        g_meshFullscreen.Draw(pCtx);  TODO: 2017/9/5
        gl.glBindVertexArray(g_fullscreen);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);

        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, m_pRtv.getTarget(), m_pRtv.getTexture(), 0);
        g_shdmgr.BindGaussian(m_pSrvTemp, 0.0f, m_blurRadius);
//        g_meshFullscreen.Draw(pCtx); TODO: 2017/9/5
        gl.glBindVertexArray(g_fullscreen);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindVertexArray(0);
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_pRtv);
        CommonUtil.safeRelease(m_pSrv);
        CommonUtil.safeRelease(m_pRtvTemp);
        CommonUtil.safeRelease(m_pSrvTemp);
    }
}
