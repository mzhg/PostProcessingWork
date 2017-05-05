package jet.opengl.postprocessing.core.eyeAdaption;

import java.io.IOException;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

final class PostProcessingCalculateLuminancePass extends PostProcessingRenderPass{

    private static PostProcessingCalculateLuminanceProgram g_CalculateLumianceProgram = null;

    private Texture2D m_SrcLum;
    private Texture2D m_DstLum;

    // Input: dowmsampled 16 x16 scene texture
    // Output: m_DstLum
    public PostProcessingCalculateLuminancePass() {
        super("CalculateLuminance");

        set(1, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_CalculateLumianceProgram == null){
            Texture2DDesc desc = new Texture2DDesc(1,1, GLenum.GL_R16F);

            m_SrcLum = TextureUtils.createTexture2D(desc, null);
            m_DstLum = TextureUtils.createTexture2D(desc, null);

            try {
                g_CalculateLumianceProgram = new PostProcessingCalculateLuminanceProgram();
                addDisposedResource(g_CalculateLumianceProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = m_DstLum;
        if(input0 == null){
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_CalculateLumianceProgram);
//        g_DownsamplePrograms[m_DownsampleMethod].setTexelSize(m_TexelFactor/input0.getWidth(), m_TexelFactor/input0.getHeight());
        g_CalculateLumianceProgram.setElapsedTime(parameters.getElapsedTime());

        context.bindTexture(input0, 0, 0);
        context.bindTexture(m_SrcLum, 1, 0);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();

        // Swap
        Texture2D temp = m_SrcLum;
        m_SrcLum = m_DstLum;
        m_DstLum = temp;
    }

    @Override
    public Texture2D getOutputTexture(int idx) {
        return idx == 0 ? m_SrcLum : null;
    }

    @Override
    protected final boolean useIntenalOutputTexture() {
        return true;
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
            out.width = 1;
            out.height = 1;
            out.format = GLenum.GL_R16F;
        }
    }
}
