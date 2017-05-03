package jet.opengl.postprocessing.core;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLCheck;
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

    private static final PostProcessingDownsampleProgram[] g_DownsamplePrograms = new PostProcessingDownsampleProgram[3];
    private static final Disposeable g_CleanArray = new Disposeable() {
        @Override
        public void dispose() {
            g_DownsamplePrograms[0] = null;
            g_DownsamplePrograms[1] = null;
            g_DownsamplePrograms[2] = null;
        }
    };

    private final int m_DownsampleCount;  // 2 or 4
    private final int m_DownsampleMethod;
    private final float m_TexelFactor;

    public PostProcessingDownsamplePass(){
        this(2, DOWMSAMPLE_NORMAL);
    }

    public PostProcessingDownsamplePass(int downsampleCount, int downsampleMethod) {
        super("Downsample");
        set(1, 1);

        m_DownsampleCount = downsampleCount;
        m_DownsampleMethod = downsampleMethod;
        m_TexelFactor = (float) (Math.log(m_DownsampleCount)/Math.log(2));

        if(GLCheck.CHECK){
            if(m_DownsampleCount == 2 && m_TexelFactor != 1.0f){
                throw new IllegalArgumentException();
            }

            if(m_DownsampleCount == 4 && m_TexelFactor != 2.0f){
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_DownsamplePrograms[m_DownsampleMethod] == null){
            try {
                g_DownsamplePrograms[m_DownsampleMethod] = new PostProcessingDownsampleProgram(m_DownsampleMethod);
                addDisposedResource(g_DownsamplePrograms[m_DownsampleMethod]);
                addDisposedResource(g_CleanArray);
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
        context.setProgram(g_DownsamplePrograms[m_DownsampleMethod]);
        g_DownsamplePrograms[m_DownsampleMethod].setTexelSize(m_TexelFactor/input0.getWidth(), m_TexelFactor/input0.getHeight());

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

        super.computeOutDesc(index, out);
    }
}
