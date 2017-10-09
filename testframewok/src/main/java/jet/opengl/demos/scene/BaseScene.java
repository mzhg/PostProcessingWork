package jet.opengl.demos.scene;

import com.nvidia.developer.opengl.app.NvSampleApp;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/10/9.
 */

public abstract class BaseScene implements Disposeable{
    protected final CameraData mSceneData = new CameraData();
    protected NvSampleApp mNVApp;
    private final SceneConfig mConfigs = new SceneConfig();
    private boolean m_bConfigDirty = false;

    private int mFramebuffer;
    private Texture2D m_SceneColorTex;
    private Texture2D m_SceneDepthTex;
    private int m_SceneWidth, m_SceneHeight;
    protected GLFuncProvider gl;

    public void setNVApp(NvSampleApp app) {mNVApp = app;}

    public final void initScene(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
        initConfig(mConfigs);
        checkConfigs(mConfigs);
        onCreate(null);
    }

    public abstract void onCreate(Object prevSavedData);
    public void onCreateUI(){}

    public void onResize(int width, int height){
        if(width <=0 || height <=0)
            return;

        m_SceneWidth = width;
        m_SceneHeight = height;
        mSceneData.aspect = (float)width/(float)height;

        if(m_SceneColorTex == null || m_SceneColorTex.getWidth() != width || m_SceneColorTex.getHeight() != height){
            initFramebuffers(width/mConfigs.downsampleScale, height/mConfigs.downsampleScale, mConfigs.sampleCount);
        }
    }

    public void setConfigs(SceneConfig configs){
        if(!mConfigs.equals(configs)){
            checkConfigs(configs);
            m_bConfigDirty = true;
            mConfigs.set(configs);
        }
    }

    public final void draw(boolean renderToFBO, boolean clearFBO){
        int vx = -1;
        int vy = -1;
        int vw = -1;
        int vh = -1;
        int old_fbo = 0;

        if(renderToFBO && !mConfigs.noFBO) {
            if(m_bConfigDirty){
                m_bConfigDirty = false;
                initFramebuffers(m_SceneWidth/mConfigs.downsampleScale, m_SceneHeight/mConfigs.downsampleScale, mConfigs.sampleCount);
            }

            IntBuffer viewport = CacheBuffer.getCachedIntBuffer(4);
            gl.glGetIntegerv(GLenum.GL_VIEWPORT, viewport);

            vx = viewport.get();
            vy = viewport.get();
            vw = viewport.get();
            vh = viewport.get();

            old_fbo = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);
            gl.glViewport(0, 0, m_SceneWidth / mConfigs.downsampleScale, m_SceneHeight / mConfigs.downsampleScale);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, mFramebuffer);
        }

        onRender(clearFBO);

        if(renderToFBO && !mConfigs.noFBO) {
            gl.glViewport(vx, vy, vw, vh);
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, old_fbo);
        }
    }

    @Override
    public final void dispose() {
        releaseResources();
        onDestroy();
    }

    private void releaseResources(){
        if(mFramebuffer != 0){
            gl.glDeleteFramebuffer(mFramebuffer);
            mFramebuffer = 0;
        }

        if(m_SceneColorTex != null){
            m_SceneColorTex.dispose();
            m_SceneColorTex = null;
        }

        if(m_SceneDepthTex != null){
            m_SceneDepthTex.dispose();
            m_SceneDepthTex = null;
        }
    }

    protected abstract void update(float dt);
    protected abstract void onRender(boolean clearFBO);
    protected abstract void onDestroy();

    protected void initConfig(SceneConfig configuration){}

    void initFramebuffers(int width, int height, int sampples){
        releaseResources();
        if(mConfigs.noFBO){
            return;
        }

        Texture2DDesc desc = new Texture2DDesc();
        desc.width = width;
        desc.height = height;
        desc.mipLevels = 1;
        desc.arraySize = 1;
        desc.sampleCount = sampples;

        if(mConfigs.colorFormat != GLenum.GL_NONE) {
            desc.format = mConfigs.colorFormat;
            m_SceneColorTex = TextureUtils.createTexture2D(desc, null);
            m_SceneColorTex.setName("SceneColor");
        }

        if(mConfigs.depthStencilFormat != GLenum.GL_NONE) {
            desc.format = mConfigs.depthStencilFormat;
            m_SceneDepthTex = TextureUtils.createTexture2D(desc, null);
            m_SceneDepthTex.setName("SceneDepth");
        }

        if(mFramebuffer == 0)
            mFramebuffer = gl.glGenFramebuffer();

        final int old_fbo = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, mFramebuffer);
        {
            if(m_SceneColorTex != null)
                gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_COLOR_ATTACHMENT0,  m_SceneColorTex.getTarget(),m_SceneColorTex.getTexture(), 0);
            if(m_SceneDepthTex != null){
                int format_conponemt = TextureUtils.measureFormat(m_SceneDepthTex.getFormat());
                switch (format_conponemt)
                {
                    case GLenum.GL_DEPTH_COMPONENT:
                        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_ATTACHMENT, m_SceneDepthTex.getTarget(),m_SceneDepthTex.getTexture(), 0);
                        break;
                    case GLenum.GL_DEPTH_STENCIL:
                        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_DEPTH_STENCIL_ATTACHMENT, m_SceneDepthTex.getTarget(),m_SceneDepthTex.getTexture(), 0);
                        break;
                    case GLenum.GL_STENCIL:
                        gl.glFramebufferTexture2D(GLenum.GL_FRAMEBUFFER, GLenum.GL_STENCIL_ATTACHMENT, m_SceneDepthTex.getTarget(),m_SceneDepthTex.getTexture(), 0);
                        break;
                    default:
                        throw new Error("Innel err!!!");
                }
            }
        }

        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, old_fbo);
    }

    private static void checkConfigs(SceneConfig mConfigs){
        if(mConfigs.colorFormat != GLenum.GL_NONE && !TextureUtils.isColorFormat(mConfigs.colorFormat)){
            throw new IllegalArgumentException("invalid colorFormat: " + Integer.toHexString(mConfigs.colorFormat));
        }

        if(mConfigs.depthStencilFormat != GLenum.GL_NONE && !(TextureUtils.isDepthFormat(mConfigs.depthStencilFormat) || TextureUtils.isStencilFormat(mConfigs.depthStencilFormat))){
            throw new IllegalArgumentException("invalid depthStencilFormat: " + Integer.toHexString(mConfigs.depthStencilFormat));
        }

        if(mConfigs.colorFormat == GLenum.GL_NONE && mConfigs.depthStencilFormat == GLenum.GL_NONE){
            throw new IllegalArgumentException("The colorFormat and depthStencilFormat can't both are none!");
        }

        if(mConfigs.downsampleScale < 1 || mConfigs.downsampleScale > 4){
            throw new IllegalArgumentException("The downsampleScale is " + mConfigs.downsampleScale + " that is out of the range[1,4].");
        }

        if(mConfigs.sampleCount < 1 || mConfigs.sampleCount > 8){
            throw new IllegalArgumentException("The sampleCount is " + mConfigs.sampleCount + " that is out of the range[1,8].");
        }
    }

    public void resoveMultisampleTexture(int mask){
        final int old_fbo = gl.glGetInteger(GLenum.GL_FRAMEBUFFER_BINDING);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, mFramebuffer);
        gl.glBindFramebuffer(GLenum.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0,0,m_SceneWidth/mConfigs.downsampleScale,m_SceneHeight/mConfigs.downsampleScale,
                0,0,m_SceneWidth,m_SceneHeight,
                mask, GLenum.GL_NEAREST);
        gl.glBindFramebuffer(GLenum.GL_READ_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, old_fbo);
    }

    public CameraData getSceneData(){return mSceneData;}
    public int getSceneWidth() { return m_SceneWidth;}
    public int getSceneHeight() { return m_SceneHeight;}
    public int getDownsamplescale() { return mConfigs.downsampleScale;}
    public int getSampleCount() { return mConfigs.sampleCount;}
    public Texture2D getSceneColorTex() { return m_SceneColorTex;}
    public Texture2D getSceneDepthTex() { return m_SceneDepthTex;}
}
