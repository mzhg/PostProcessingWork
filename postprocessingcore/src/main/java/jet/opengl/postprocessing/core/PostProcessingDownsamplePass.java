package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/4/28.
 */

public class PostProcessingDownsamplePass extends PostProcessingRenderPass {

    public static final int DOWMSAMPLE_FASTEST = 0;
    public static final int DOWMSAMPLE_NORMAL = 1;
    public static final int DOWMSAMPLE_COMBINED_DEPTH = 2;

    // TODO：this need declare as arrays.
    private static PostProcessingDownsampleProgram g_DownsampleProgram = null;
    private final int m_DownsampleCount;  // 2 or 4
    private final int m_DownsampleMethod;

    public PostProcessingDownsamplePass(){
        this(2, DOWMSAMPLE_NORMAL);
    }

    public PostProcessingDownsamplePass(int downsampleCount, int downsampleMethod) {
        super("Downsample");
        set(1, 1);

        m_DownsampleCount = downsampleCount;
        m_DownsampleMethod = downsampleMethod;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_DownsampleProgram == null){
            try {
                g_DownsampleProgram = new PostProcessingDownsampleProgram(m_DownsampleMethod);
                addDisposedResource(g_DownsampleProgram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_DownsampleProgram);
        g_DownsampleProgram.setTexelSize(1.0f/input0.getWidth(), 1.0f/input0.getHeight());

        context.bindTexture(input0, 0, 0);
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

            out.width = Numeric.divideAndRoundUp(out.width, m_DownsampleCount);
            out.height = Numeric.divideAndRoundUp(out.height, m_DownsampleCount);
        }
    }
}
