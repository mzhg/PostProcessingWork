package jet.opengl.postprocessing.core.ssao;

import java.io.IOException;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.core.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:36:43.
 */

final class PostProcessingHBAOBlurPass extends PostProcessingRenderPass {

    private static PostProcessingHBAOBlurProgram g_HBAOBlurProgram0 = null;
    private static PostProcessingHBAOBlurProgram g_HBAOBlurProgram1 = null;
    private final boolean m_bMSAA;
    private final BlendState m_bsstate;

    public PostProcessingHBAOBlurPass(boolean enableMSAA, int sampleIndex) {
        super("HBAOCalculateBlur");

        m_bMSAA = enableMSAA;
        set(1,1);
        setOutputTarget(PostProcessingRenderPassOutputTarget.SOURCE_COLOR);

        m_bsstate = new BlendState();
        m_bsstate.blendEnable = true;
        m_bsstate.srcBlend = GLenum.GL_ZERO;
        m_bsstate.srcBlendAlpha = GLenum.GL_ZERO;
        m_bsstate.destBlend = GLenum.GL_SRC_COLOR;
        m_bsstate.destBlendAlpha = GLenum.GL_SRC_COLOR;

        m_bsstate.sampleMask = enableMSAA;
        m_bsstate.sampleMaskValue = 1 << sampleIndex;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_HBAOBlurProgram0 == null){
            try {
                g_HBAOBlurProgram0 = new PostProcessingHBAOBlurProgram(0);
                addDisposedResource(g_HBAOBlurProgram0);

                g_HBAOBlurProgram1 = new PostProcessingHBAOBlurProgram(1);
                addDisposedResource(g_HBAOBlurProgram1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);  // The source texture
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReinterleavePass:: Missing depth texture!");
            return;
        }

        Texture2D tempTex = RenderTexturePool.getInstance().findFreeElement(output.getWidth(), output.getHeight(), GLenum.GL_RG16F);

        // Two passes for HBAO blur.
        {   // first pass: blur the input to tempTex
            context.setViewport(0,0, tempTex.getWidth(), tempTex.getHeight());
            context.setVAO(null);
            context.setProgram(g_HBAOBlurProgram0);
            g_HBAOBlurProgram0.setUVAndResolution(1.0f/input0.getWidth(), 0, 40.0f); // TODO sharpness

            context.bindTexture(input0, 0, 0);
            context.setBlendState(null);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setRenderTarget(tempTex);

            context.drawFullscreenQuad();
        }

        {   // second pass: blur the tempTex to output
            context.setViewport(0,0, output.getWidth(), output.getHeight());
            context.setVAO(null);
            context.setProgram(g_HBAOBlurProgram1);
            g_HBAOBlurProgram1.setUVAndResolution(0, 1.0f/input0.getHeight(), 40.0f); // TODO sharpness

            context.bindTexture(tempTex, 0, 0);
            context.setBlendState(m_bsstate);       // TODO blend enabled.
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setRenderTarget(output);

//            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
//            gl.glEnable(GLenum.GL_BLEND);
//            gl.glBlendFunc(GLenum.GL_ZERO,GLenum.GL_SRC_COLOR);
//            if(m_bMSAA){
//                gl.glEnable(GLenum.GL_SAMPLE_MASK);
//                gl.glSampleMaski(0, m_bsstate.sampleMaskValue);
//            }

            context.drawFullscreenQuad();

//            if(m_bMSAA){
//                gl.glDisable(GLenum.GL_SAMPLE_MASK);
//                gl.glSampleMaski(0, -1);
//            }
//
//            gl.glDisable(GLenum.GL_BLEND);
        }

        RenderTexturePool.getInstance().freeUnusedResource(tempTex);

        if(GLCheck.CHECK)
            GLCheck.checkError("HBAOBlurPass");
    }
}
