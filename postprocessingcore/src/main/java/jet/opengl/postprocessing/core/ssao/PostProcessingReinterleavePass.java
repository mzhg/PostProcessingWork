package jet.opengl.postprocessing.core.ssao;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:36:43.
 */

final class PostProcessingReinterleavePass extends PostProcessingRenderPass {

    private static PostProcessingReinterleaveProgram g_ReinterleaveProgram = null;

    public PostProcessingReinterleavePass() {
        super("Reinterleave");

        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_ReinterleaveProgram == null){
            try {
                g_ReinterleaveProgram = new PostProcessingReinterleaveProgram();
                addDisposedResource(g_ReinterleaveProgram);
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

        context.setViewport(0,0, output.getWidth() * 4, output.getHeight() * 4);
        context.setVAO(null);
        context.setProgram(g_ReinterleaveProgram);

        context.bindTexture(input0, 0, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();

        if(GLCheck.CHECK)
            GLCheck.checkError("ReinterleavePass");
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.format = GLenum.GL_RG8;
        }

        super.computeOutDesc(index, out);
    }
}
