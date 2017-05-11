package jet.opengl.postprocessing.core;

import java.io.IOException;
import java.util.HashMap;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/28.
 */

public class PostProcessingGaussionBlurPass2 extends PostProcessingRenderPass {

    private static final HashMap<ProgramDesc, PostProcessingGaussionBlurProgram2[]> g_GaussionBlurPrograms = new HashMap<>();
    private static boolean g_MapAdded = false;
    private final Integer m_KernelSize;
    private static final ProgramDesc g_ProgramKey = new ProgramDesc();

    public PostProcessingGaussionBlurPass2(int kernelSize) {
        super("GaussionBlur");
        set(1, 1);

        m_KernelSize = kernelSize;
    }

    private static PostProcessingGaussionBlurProgram2[] getGaussianBlurProgram(ProgramDesc key){
        PostProcessingGaussionBlurProgram2[] gaussionBlurProgram = g_GaussionBlurPrograms.get(key);
        if(gaussionBlurProgram == null){
            try {
                PostProcessingGaussionBlurProgram2 program_vert = new PostProcessingGaussionBlurProgram2(key.width, key.height, true, key.weight);
                PostProcessingGaussionBlurProgram2 program_hori = new PostProcessingGaussionBlurProgram2(key.width, key.height, false, key.weight);

                gaussionBlurProgram = new PostProcessingGaussionBlurProgram2[] {program_vert, program_hori};
                g_GaussionBlurPrograms.put(new ProgramDesc(key), gaussionBlurProgram);

                if(!g_MapAdded){
                    addDisposedResource(()->g_GaussionBlurPrograms.clear());
                    g_MapAdded = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return gaussionBlurProgram;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            return;
        }

        g_ProgramKey.weight = 0.3f;
        g_ProgramKey.width = input0.getWidth();
        g_ProgramKey.height = input0.getHeight();

        PostProcessingGaussionBlurProgram2[] gaussionBlurPrograms = getGaussianBlurProgram(g_ProgramKey);

        Texture2D tempTex = RenderTexturePool.getInstance().findFreeElement(input0.getWidth(), input0.getHeight(), input0.getFormat());

        { // horital blur pass
            context.setViewport(0,0, tempTex.getWidth(), tempTex.getHeight());
            context.setVAO(null);
            context.setProgram(gaussionBlurPrograms[0]);
//            gaussionBlurProgram.setHalfPixelSize(1.0f/input0.getWidth(), 0);

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
            context.setProgram(gaussionBlurPrograms[1]);
//            gaussionBlurProgram.setHalfPixelSize(0, 1.0f/tempTex.getHeight());

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

    private static final class ProgramDesc{
        int width;
        int height;
        float weight;

        ProgramDesc(){}
        public ProgramDesc(int width, int height, float weight) {
            this.width = width;
            this.height = height;
            this.weight = weight;
        }

        public ProgramDesc(ProgramDesc o){
            this.width = o.width;
            this.height = o.height;
            this.weight = o.weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;

            ProgramDesc that = (ProgramDesc) o;

            if (width != that.width) return false;
            if (height != that.height) return false;
            return Float.compare(that.weight, weight) == 0;

        }

        @Override
        public int hashCode() {
            int result = width;
            result = 31 * result + height;
            result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
            return result;
        }
    }
}
