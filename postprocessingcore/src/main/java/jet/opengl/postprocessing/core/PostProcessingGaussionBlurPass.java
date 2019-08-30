package jet.opengl.postprocessing.core;

import java.io.IOException;
import java.util.HashMap;

import jet.opengl.postprocessing.texture.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/28.
 */
@Deprecated
public class PostProcessingGaussionBlurPass extends PostProcessingRenderPass {

    private static final HashMap<Integer, PostProcessingGaussionBlurProgram> g_GaussionBlurPrograms = new HashMap<>();
    private final Integer m_KernelSize;
    private final Texture2DDesc m_Desc = new Texture2DDesc();

    public PostProcessingGaussionBlurPass(int kernelSize) {
        super("GaussionBlur");
        set(1, 1);

        m_KernelSize = kernelSize;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        PostProcessingGaussionBlurProgram gaussionBlurProgram = g_GaussionBlurPrograms.get(m_KernelSize);

        if(gaussionBlurProgram == null){
            try {
                gaussionBlurProgram = new PostProcessingGaussionBlurProgram(m_KernelSize);
                g_GaussionBlurPrograms.put(m_KernelSize, gaussionBlurProgram);

                addDisposedResource(gaussionBlurProgram);
                addDisposedResource(PostProcessingGaussionBlurPass::cleanMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            return;
        }

        Texture2D tempTex = RenderTexturePool.getInstance().findFreeElement(input0.getDesc(m_Desc));

        { // horital blur pass
            context.setViewport(0,0, tempTex.getWidth(), tempTex.getHeight());
            context.setVAO(null);
            context.setProgram(gaussionBlurProgram);
            gaussionBlurProgram.setHalfPixelSize(1.0f/input0.getWidth(), 0);

            context.bindTexture(input0, 0, 0);
            context.setBlendState(null);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setRenderTarget(tempTex);
            context.drawFullscreenQuad();
        }

        {  // vertical blur pass
            context.setViewport(0,0, output.getWidth(), output.getHeight());
            context.setVAO(null);
            context.setProgram(gaussionBlurProgram);
            gaussionBlurProgram.setHalfPixelSize(0, 1.0f/tempTex.getHeight());

            context.bindTexture(tempTex, 0, 0);
            context.setBlendState(null);
            context.setDepthStencilState(null);
            context.setRasterizerState(null);
            context.setRenderTarget(output);
            context.drawFullscreenQuad();
        }

        RenderTexturePool.getInstance().freeUnusedResource(tempTex);

    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
        }
    }

    private static void cleanMap(){
        g_GaussionBlurPrograms.clear();
    }
}
