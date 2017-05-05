package jet.opengl.postprocessing.core.light;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/5/5.
 */

public class Downsample4xPass extends PostProcessingRenderPass {
    private static Downsample4xProgram g_DownsampleProgram;

    public Downsample4xPass() {
        super("Downsample4xPass");

        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_DownsampleProgram == null){
            try {

                g_DownsampleProgram = new Downsample4xProgram();
                addDisposedResource(g_DownsampleProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input = getInput(0);

        Texture2D output = getOutputTexture(0);
        if(input == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "PostProcessingTonemappingPass:: Missing sceneTex!");
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_DownsampleProgram);
        g_DownsampleProgram.setTwoTexelSize(2.0f/ input.getWidth(), 2.0f/input.getHeight());

        context.bindTexture(input, 0, 0);  // Scene texture.

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
