package jet.opengl.postprocessing.core.ssao;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.core.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:36:43.
 */

final class PostProcessingHBAOBlurPass extends PostProcessingRenderPass {

    private static PostProcessingHBAOBlurProgram g_HBAOBlurProgram = null;

    public PostProcessingHBAOBlurPass() {
        super("HBAOCalculateBlur");

        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_HBAOBlurProgram == null){
            try {
                g_HBAOBlurProgram = new PostProcessingHBAOBlurProgram();
                addDisposedResource(g_HBAOBlurProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReinterleavePass:: Missing depth texture!");
            return;
        }

        Texture2D tempTex = RenderTexturePool.getInstance().findFreeElement(input0.getWidth(), input0.getHeight(), GLenum.GL_RG16F);

        // Two passes for HBAO blur.
        {   // first pass: blur the input to tempTex
            context.setViewport(0,0, tempTex.getWidth(), tempTex.getHeight());
            context.setVAO(null);
            context.setProgram(g_HBAOBlurProgram);
            g_HBAOBlurProgram.setUVAndResolution(1.0f/input0.getWidth(), 0, 1.0f); // TODO sharpness

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
            context.setProgram(g_HBAOBlurProgram);
            g_HBAOBlurProgram.setUVAndResolution(0, 1.0f/input0.getHeight(), 1.0f); // TODO sharpness

            context.bindTexture(input0, 0, 0);
            context.setBlendState(null);       // TODO blend enabled.
            context.setDepthStencilState(null);
            context.setRasterizerState(null);  // TODO sample enabled.
            context.setRenderTarget(output);

            context.drawFullscreenQuad();
        }

        RenderTexturePool.getInstance().freeUnusedResource(tempTex);
    }

    @Override
    protected PostProcessingRenderPassOutputTarget getOutputTarget() {
        return PostProcessingRenderPassOutputTarget.SOURCE_COLOR;
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.format = GLenum.GL_RG16F;
        }

        super.computeOutDesc(index, out);
    }
}
