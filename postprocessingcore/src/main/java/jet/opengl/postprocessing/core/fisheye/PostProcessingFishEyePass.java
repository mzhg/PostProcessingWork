package jet.opengl.postprocessing.core.fisheye;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

final class PostProcessingFishEyePass extends PostProcessingRenderPass {

    private static PostProcessingFishEyeProgram g_FishEyeProgram;

    public PostProcessingFishEyePass() {
        super("Fish Eye");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_FishEyeProgram == null){
            try {
                g_FishEyeProgram = new PostProcessingFishEyeProgram();
                addDisposedResource(g_FishEyeProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input == null){
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_FishEyeProgram);
        g_FishEyeProgram.setUniforms(output.getWidth(), output.getHeight(), parameters.getFishEyeFactor());

        context.bindTexture(input, 0, 0);
        context.setBlendState(null); 
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
        }
    }
}
