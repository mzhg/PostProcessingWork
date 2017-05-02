package jet.opengl.postprocessing.core.fxaa;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

final class PostProcessingFXAAPass extends PostProcessingRenderPass {

    private static PostProcessingFXAAProgram g_FXAAESProgram;
    private static final PostProcessingFXAAProgram[] f_FXAAPrograms = new PostProcessingFXAAProgram[6];

    private static void cleanArrays(){
        Arrays.fill(f_FXAAPrograms, null);
    }

    private static PostProcessingFXAAProgram getFXAAESProgram(int quality){
        try {
            if(GLFuncProviderFactory.getGLFuncProvider().getHostAPI() == GLAPI.ANDROID){
                if(g_FXAAESProgram == null){
                    g_FXAAESProgram = new PostProcessingFXAAProgram();
                    addDisposedResource(g_FXAAESProgram);
                }

                return g_FXAAESProgram;
            }else{
                if(f_FXAAPrograms[quality] == null){
                    f_FXAAPrograms[quality] = new PostProcessingFXAAProgram(quality);
                    addDisposedResource(f_FXAAPrograms[quality]);
                    addDisposedResource(PostProcessingFXAAPass::cleanArrays);
                }

                return f_FXAAPrograms[quality];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public PostProcessingFXAAPass() {
        super("FXAA");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        PostProcessingFXAAProgram fxaaProgram = getFXAAESProgram(parameters.getFXAAQuality());

        Texture2D input = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input == null){
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(fxaaProgram);
        fxaaProgram.setTexelSize(1.0f/output.getWidth(), 1.0f/output.getHeight());

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
