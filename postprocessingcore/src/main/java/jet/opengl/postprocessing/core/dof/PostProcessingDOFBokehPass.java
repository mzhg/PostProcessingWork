package jet.opengl.postprocessing.core.dof;

import java.io.IOException;

import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

final class PostProcessingDOFBokehPass extends PostProcessingRenderPass {

    private static PostProcessingDOFBokehProgram g_DOFBokehProgram;

    private int m_sampler;

    public PostProcessingDOFBokehPass() {
        super("DOFBokeh");
        // input0: scene texture
        // input1: depth texture
        set(2,1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_DOFBokehProgram == null){
            try {
                g_DOFBokehProgram = new PostProcessingDOFBokehProgram();
                addDisposedResource(g_DOFBokehProgram);

                m_sampler = SamplerUtils.getDefaultSampler();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);
        Texture2D output = getOutputTexture(0);
        if(input0 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "DOFBokehPass:: Missing scene texture!");
            return;
        }

        if(input1 == null){
            LogUtil.e(LogUtil.LogType.DEFAULT, "DOFBokehPass:: Missing depth texture!");
            return;
        }

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_DOFBokehProgram);
//        g_BloomProgram.setUniforms(parameters.getBloomThreshold(), parameters.getExposureScale());
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
        g_DOFBokehProgram.setUniform(input0.getWidth(), input0.getHeight(), frameAttribs.cameraNear, frameAttribs.cameraFar,
                parameters.getFocalDepth(), parameters.getFocalLength(), parameters.getFstop());

        context.bindTexture(input0, 0, 0);
        context.bindTexture(input1, 1, m_sampler);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        context.bindTexture(null, 1, 0); // reset the samplers
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
