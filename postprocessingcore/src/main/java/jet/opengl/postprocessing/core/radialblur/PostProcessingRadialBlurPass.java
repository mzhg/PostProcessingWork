package jet.opengl.postprocessing.core.radialblur;

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

public class PostProcessingRadialBlurPass extends PostProcessingRenderPass {

    private static PostProcessingRadialBlurProgram g_RadialBlurProgram;

    public PostProcessingRadialBlurPass() {
        super("Radial Blur");
        set(1,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_RadialBlurProgram == null){
            try {
                g_RadialBlurProgram = new PostProcessingRadialBlurProgram();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input == null){
            return;
        }

        context.setViewport(0,0, input.getWidth(), input.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_RadialBlurProgram);
        g_RadialBlurProgram.setUniformValue(parameters.getRadialBlurCenterX(), parameters.getRadialBlurCenterY(),
                                            parameters.getGlobalTime(), parameters.getRadialBlurSamples());

        context.bindTexture(input, 0, 0);
        context.setBlendState(null); 
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

//        FloatBuffer red = CacheBuffer.wrap(1.0f,0,0,0);
//        GLFuncProviderFactory.getGLFuncProvider().glClearBufferfv(GLenum.GL_COLOR, 0, red);
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

    public static void releaseResources(){
        if(g_RadialBlurProgram != null){
            g_RadialBlurProgram.dispose();
            g_RadialBlurProgram = null;
        }
    }
}
