package jet.opengl.demos.nvidia.shadows;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/11/9.
 */

final class VarianceShadowMapGenerator implements Disposeable{
    private RenderTargets m_renderTarget;
    private Texture2D m_VarianceShadowMap;
    private Texture2D m_ShadowMap;
    private Texture2D m_BlurTex;

    private VSMGenerateProgram m_ShadowGenerateProgram;
    private VSMBlurProgram m_BlurProgram;
    private VSMSceneController m_Scene;
    private GLFuncProvider gl;
    private boolean m_printOnce;

    public interface VSMSceneController{
        void onShadowRender(VSMGenerateProgram shader);
    }

    public void setShadowScene(VSMSceneController scene) {m_Scene = scene;}

    public void initlize(int shadowMapSize){
        m_renderTarget = new RenderTargets();
        gl = GLFuncProviderFactory.getGLFuncProvider();

        Texture2DDesc tex_desc = new Texture2DDesc(shadowMapSize, shadowMapSize, GLenum.GL_RG32F);
        tex_desc.mipLevels = (int) (Math.log(shadowMapSize)/Math.log(2));
        m_VarianceShadowMap = TextureUtils.createTexture2D(tex_desc, null);
        gl.glBindTexture(m_VarianceShadowMap.getTarget(), m_VarianceShadowMap.getTexture());
        gl.glTexParameteri(m_VarianceShadowMap.getTarget(), GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(m_VarianceShadowMap.getTarget(), GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(m_VarianceShadowMap.getTarget(), GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(m_VarianceShadowMap.getTarget(), GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

        tex_desc.mipLevels = 1;
        tex_desc.format = GLenum.GL_DEPTH_COMPONENT32F;  // Standrad shadow map
        m_ShadowMap = TextureUtils.createTexture2D(tex_desc, null);

        tex_desc.format = GLenum.GL_RG32F;
        m_BlurTex = TextureUtils.createTexture2D(tex_desc, null);
        gl.glBindTexture(m_BlurTex.getTarget(), m_BlurTex.getTexture());
        gl.glTexParameteri(m_BlurTex.getTarget(), GLenum.GL_TEXTURE_MIN_FILTER, GLenum.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(m_BlurTex.getTarget(), GLenum.GL_TEXTURE_MAG_FILTER, GLenum.GL_LINEAR);
        gl.glTexParameteri(m_BlurTex.getTarget(), GLenum.GL_TEXTURE_WRAP_S, GLenum.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(m_BlurTex.getTarget(), GLenum.GL_TEXTURE_WRAP_T, GLenum.GL_CLAMP_TO_EDGE);

        m_ShadowGenerateProgram = new VSMGenerateProgram();
        m_BlurProgram = new VSMBlurProgram();
    }

    public void generateShadow(){
        // 1, Saved the current FBO
        int vx = -1;
        int vy = -1;
        int vw = -1;
        int vh = -1;
        int old_fbo = 0;
        boolean depthState = gl.glIsEnabled(GLenum.GL_DEPTH_TEST);
        IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);

        vx = viewport.get();
        vy = viewport.get();
        vw = viewport.get();
        vh = viewport.get();
        old_fbo = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);

        // 2, Render the Shadow Map
        m_renderTarget.bind();
        TextureGL RTVs[] = {m_ShadowMap, m_VarianceShadowMap};
        m_renderTarget.setRenderTextures(RTVs, null);
        gl.glViewport(0,0, m_ShadowMap.getWidth(), m_ShadowMap.getHeight());
        gl.glEnable(GLenum.GL_DEPTH_TEST);
        gl.glDepthFunc(GLenum.GL_LESS);
        gl.glDepthMask(true);
        float Zmax = 1.e4f;
        gl.glClearColor(Zmax, Zmax*Zmax, 1.0f, 1.0f);
        gl.glClearDepthf(1.0f);
        gl.glClear(GLenum.GL_COLOR_BUFFER_BIT|GLenum.GL_DEPTH_BUFFER_BIT);

        m_ShadowGenerateProgram.enable();
        m_Scene.onShadowRender(m_ShadowGenerateProgram);
        if(!m_printOnce){
            m_ShadowGenerateProgram.printPrograminfo();
        }

        // 2, Blur the variance shadow map.
        m_renderTarget.setRenderTexture(m_BlurTex, null);
        m_BlurProgram.enable();
        m_BlurProgram.setVertical(true);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(m_VarianceShadowMap.getTarget(), m_VarianceShadowMap.getTexture());
        gl.glBindSampler(0, 0);
        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        gl.glBindTexture(m_VarianceShadowMap.getTarget(), 0);

        m_renderTarget.setRenderTexture(m_VarianceShadowMap, null);
        m_BlurProgram.setVertical(false);
        gl.glBindTexture(m_BlurTex.getTarget(), m_BlurTex.getTexture());
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        gl.glBindTexture(m_VarianceShadowMap.getTarget(), m_VarianceShadowMap.getTexture());
        gl.glGenerateMipmap(m_VarianceShadowMap.getTarget());
        gl.glBindTexture(m_VarianceShadowMap.getTarget(), 0);

        if(!m_printOnce){
            m_BlurProgram.printPrograminfo();
        }

        // 3, Restore the framebuffer and viewport
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, old_fbo);
        gl.glViewport(vx, vy, vw, vh);
        if(depthState)
            gl.glEnable(GLenum.GL_DEPTH_TEST);
        else
            gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDisable(GLenum.GL_POLYGON_OFFSET_FILL);

        m_printOnce = true;
    }

    public Texture2D getVarianceShadowMap() { return m_VarianceShadowMap;}
    public Texture2D getShadowMap() { return m_ShadowMap;}

    @Override
    public void dispose() {
        CommonUtil.safeRelease(m_renderTarget);
        CommonUtil.safeRelease(m_VarianceShadowMap);
        CommonUtil.safeRelease(m_ShadowMap);
        CommonUtil.safeRelease(m_BlurTex);

        CommonUtil.safeRelease(m_ShadowGenerateProgram);
        CommonUtil.safeRelease(m_BlurProgram);
    }
}
