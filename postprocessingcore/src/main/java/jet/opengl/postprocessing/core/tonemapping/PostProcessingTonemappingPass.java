package jet.opengl.postprocessing.core.tonemapping;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class PostProcessingTonemappingPass extends PostProcessingRenderPass {

    private static PostProcessingTonemappingProgram g_TonemappingProgram;
    private boolean enableEyeAdaption;

    public PostProcessingTonemappingPass(boolean enableEyeAdaption) {
        super("HDRTonemapping");
        set(enableEyeAdaption ? 3 : 2,1);

        this.enableEyeAdaption = enableEyeAdaption;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_TonemappingProgram == null){
            try {

                g_TonemappingProgram = new PostProcessingTonemappingProgram(enableEyeAdaption);
                addDisposedResource(()->{g_TonemappingProgram.dispose(); g_TonemappingProgram = null;});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D sceneTex = getInput(0);
        Texture2D blurTex = getInput(1);
        Texture2D lumTex = getInput(2);

        Texture2D output = getOutputTexture(0);
        if(sceneTex == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "PostProcessingTonemappingPass:: Missing sceneTex!");
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_TonemappingProgram);
        g_TonemappingProgram.setUniforms(parameters.getLightEffectAmout(), parameters.getLightEffectExpose(), parameters.getGamma());

        context.bindTexture(sceneTex, 0, 0);  // Scene texture.
        context.bindTexture(blurTex, 1, 0);  // Blur texture.
        context.bindTexture(lumTex, 2, 0);  // Lum texture.

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
