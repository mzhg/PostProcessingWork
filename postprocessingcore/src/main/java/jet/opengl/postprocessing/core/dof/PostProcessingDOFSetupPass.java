package jet.opengl.postprocessing.core.dof;

import java.io.IOException;
import java.util.Arrays;

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

final class PostProcessingDOFSetupPass extends PostProcessingRenderPass {

    private static PostProcessingDOFSetupProgram[] g_DOFSetupPrograms = new PostProcessingDOFSetupProgram[4];

    private final boolean m_bInFarBlur;
    private final boolean m_bInNearBlur;
    private int m_sampler;

    public PostProcessingDOFSetupPass(boolean bInFarBlur, boolean bInNearBlur) {
        super("DOFSetup");
        m_bInFarBlur = bInFarBlur;
        m_bInNearBlur = bInNearBlur;

        // input0: scene texture
        // input1: depth texture
        set(2,(bInFarBlur?1:0)+(bInNearBlur?1:0));
    }

    private static PostProcessingDOFSetupProgram getDOFSetupProgram(boolean bInFarBlur, boolean bInNearBlur){
        int low = bInNearBlur ? 1:0;
        int high = bInFarBlur ? 2:0;

        int index = low | high;
        if(g_DOFSetupPrograms[index] == null){
            try {
                g_DOFSetupPrograms[index] = new PostProcessingDOFSetupProgram(bInFarBlur, bInNearBlur);
                addDisposedResource(g_DOFSetupPrograms[index]);
                addDisposedResource(()-> Arrays.fill(g_DOFSetupPrograms, null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return g_DOFSetupPrograms[index];
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        PostProcessingDOFSetupProgram dofSetupProgram = getDOFSetupProgram(m_bInFarBlur, m_bInNearBlur);
        if(m_sampler == 0){
            m_sampler = SamplerUtils.getDefaultSampler();
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
        context.setProgram(dofSetupProgram);
//        g_BloomProgram.setUniforms(parameters.getBloomThreshold(), parameters.getExposureScale());
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();
//        g_DOFBokehProgram.setUniform(input0.getWidth(), input0.getHeight(), frameAttribs.cameraNear, frameAttribs.cameraFar,
//                parameters.getFocalDepth(), parameters.getFocalLength(), parameters.getFstop());
        dofSetupProgram.setUniform(input0.getWidth(), input0.getHeight(), frameAttribs.cameraNear, frameAttribs.cameraFar,
                parameters.getFocalDepth(), parameters.getFocalLength(), parameters.getNearTransitionRegion(), parameters.getFarTransitionRegion(),
                parameters.getFieldScale());
        dofSetupProgram.setScreenToWorld(frameAttribs.getViewProjInvertMatrix());

        context.bindTexture(input0, 0, 0);
        context.bindTexture(input1, 1, m_sampler);  // To keep the depth texture have correct sampler.

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTargets(m_PassOutputs);  // Multi-output.

        context.drawFullscreenQuad();
//        context.bindTexture(input1, 1, 0); // reset the sampler
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
