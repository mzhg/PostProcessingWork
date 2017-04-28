package jet.opengl.postprocessing.core.toon;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

final class PostProcessingToonPass extends PostProcessingRenderPass {

    private static PostProcessingToonProgram g_ToonProgram;

    public PostProcessingToonPass() {
        super("Radial Blur");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_ToonProgram == null){
            try {
                g_ToonProgram = new PostProcessingToonProgram();
                addDisposedResource(g_ToonProgram);
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
        context.setProgram(g_ToonProgram);
        g_ToonProgram.setUniforms(parameters.getEdgeThreshold(), parameters.getEdgeThreshold2(), 1.0f/input.getWidth(), 1.0f/input.getHeight());

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
            GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
//            if(TextureUtils.isCompressedFormat(out.format))
            {
                out.format = gl.getHostAPI() == GLAPI.ANDROID ? GLenum.GL_RGB: GLenum.GL_RGB8;
            }
        }
    }
}
