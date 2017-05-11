package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017-05-11 16:17:38.
 */

public class PostProcessingReconstructNormalPass extends PostProcessingRenderPass {

    private static PostProcessingReconstructNormalProgram g_ReconstructNormalProgram = null;

    public PostProcessingReconstructNormalPass() {
        super("ReconstructNormalPass");

        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_ReconstructNormalProgram == null){
            try {
                g_ReconstructNormalProgram = new PostProcessingReconstructNormalProgram();
                addDisposedResource(g_ReconstructNormalProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "ReconstructNormalPass:: Missing input texture!");
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_ReconstructNormalProgram);
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
        g_ReconstructNormalProgram.setCameraMatrixs(frameAttribs.projMat, frameAttribs.getProjInvertMatrix());

        context.bindTexture(input0, 0, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        context.bindTexture(input0, 0, 0);  // unbind sampler.
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.format = GLenum.GL_RGB8;
        }

        super.computeOutDesc(index, out);
    }
}
