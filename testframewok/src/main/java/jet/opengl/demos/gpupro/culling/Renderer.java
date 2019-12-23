package jet.opengl.demos.gpupro.culling;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

abstract class Renderer implements Disposeable {

    Texture2D mColorBuffer;
    Texture2D mDepthBuffer;

    GLFuncProvider gl;
    RenderTargets mFBO;

    int mFrameNumber;

    private int m_BlitFBO;
    private final Texture2D[] mOutput = new Texture2D[2];

    void onCreate(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        mFBO = new RenderTargets();
        m_BlitFBO = gl.glGenFramebuffer();
    }

    void onResize(int width, int height){
        if(mColorBuffer == null || mColorBuffer.getWidth()!= width || mColorBuffer.getHeight() != height){
            SAFE_RELEASE(mColorBuffer);
            SAFE_RELEASE(mDepthBuffer);
        }

        Texture2DDesc desc = new Texture2DDesc(width, height, GLenum.GL_RGBA8);
        mColorBuffer = TextureUtils.createTexture2D(desc, null);
        desc.format = GLenum.GL_DEPTH24_STENCIL8;
        mDepthBuffer = TextureUtils.createTexture2D(desc, null);

        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_BlitFBO);
        gl.glFramebufferTexture2D(GLenum.GL_READ_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0, mColorBuffer.getTarget(), mColorBuffer.getTexture(), 0);
        gl.glReadBuffer(GLenum.GL_COLOR_ATTACHMENT0);

        mOutput[0] = mColorBuffer;
        mOutput[1] = mDepthBuffer;
    }

    void setOutputRenderTaget(){
        mFBO.bind();
        mFBO.setRenderTextures(mOutput, null);
        gl.glViewport(0, 0, mColorBuffer.getWidth(), mColorBuffer.getHeight());
    }

    abstract void render(Scene scene, boolean clearFBO);

    void present(){
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, m_BlitFBO);

        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0,0,mColorBuffer.getWidth(),mColorBuffer.getHeight(),
                0,0,mColorBuffer.getWidth(),mColorBuffer.getHeight(),
                GLenum.GL_COLOR_BUFFER_BIT, GLenum.GL_NEAREST);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(mColorBuffer);
        SAFE_RELEASE(mDepthBuffer);
        SAFE_RELEASE(mFBO);

        if(m_BlitFBO != 0){
            gl.glDeleteFramebuffer(m_BlitFBO);
            m_BlitFBO = 0;
        }
    }
}
