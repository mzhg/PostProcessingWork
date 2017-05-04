package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/28.
 */

public class PostProcessingCombinePass extends PostProcessingRenderPass {

    private static PostProcessingCombineProgram g_CombineProgram = null;
    public PostProcessingCombinePass() {
        super("Combine");
        set(2, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_CombineProgram == null){
            try {
                g_CombineProgram = new PostProcessingCombineProgram();
                addDisposedResource(g_CombineProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);
        Texture2D output = getOutputTexture(0);
        if(input0 == null || input1 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "Missing input textures: input0 = " + input0 + ", input1 = " + input1);
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_CombineProgram);
        g_CombineProgram.setIntensity(1.0f, parameters.getBloomIntensity());

//        m_bindTextures[0] = input0;
//        m_bindTextures[1] = input1;

        context.bindTextures(m_PassInputs, null, null);
//        context.bindTexture(input0, 0, 0);
//        context.bindTexture(input1, 1, 0);
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
