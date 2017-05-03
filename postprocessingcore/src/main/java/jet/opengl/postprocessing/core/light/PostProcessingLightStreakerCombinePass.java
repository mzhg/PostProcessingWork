package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/28.
 */

final class PostProcessingLightStreakerCombinePass extends PostProcessingRenderPass {

    private static PostProcessingLightStreakerComposeProgram g_CombineProgram = null;

    public PostProcessingLightStreakerCombinePass(int inputCount) {
        super("LightStreakerCombine");
        set(inputCount, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_CombineProgram == null){
            try {
                g_CombineProgram = new PostProcessingLightStreakerComposeProgram();
                addDisposedResource(g_CombineProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_CombineProgram);

//        m_bindTextures[0] = input0;
//        m_bindTextures[1] = input1;

        context.bindTextures(m_PassInputs, null, null);
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

        super.computeOutDesc(index, out);
    }
}
