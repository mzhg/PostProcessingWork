package jet.opengl.postprocessing.core.bloom;

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

final class PostProcessingBloomSetupPass extends PostProcessingRenderPass {

    private static PostProcessingBloomProgram g_BloomProgram;

    public PostProcessingBloomSetupPass() {
        super("Bloom");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_BloomProgram == null){
            try {
                g_BloomProgram = new PostProcessingBloomProgram();
                addDisposedResource(g_BloomProgram);
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
        context.setProgram(g_BloomProgram);
        g_BloomProgram.setUniforms(parameters.getBloomThreshold(), parameters.getExposureScale());

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
