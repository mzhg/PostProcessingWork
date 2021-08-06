package jet.opengl.postprocessing.core.grayscreen;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2021/8/2.
 */

final class PostProcessingGrayScreenPass extends PostProcessingRenderPass {

    private static PostProcessingGrayScreenProgram g_FishEyeProgram;

    public PostProcessingGrayScreenPass() {
        super("Gray Screen");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_FishEyeProgram == null){
            try {
                g_FishEyeProgram = new PostProcessingGrayScreenProgram();
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
        g_FishEyeProgram.setUniforms(output.getWidth(), output.getHeight(), parameters.getRectBorder());

        context.bindTexture(input, 0, 0);
        context.bindTexture(parameters.getPaper(), 1, 0);

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
