package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

final class PostProcessingUpdateAverageLuminancePass extends PostProcessingRenderPass{

    private RenderTechnique m_UpdateAverageLuminanceProgram = null;
    private SharedData m_sharedData;
    private Texture2D m_ptex2DAverageLuminance;

    public PostProcessingUpdateAverageLuminancePass(SharedData sharedData, Texture2D averageLuminance) {
        super("UpdateAverageLuminance");

        m_ptex2DAverageLuminance = averageLuminance;
        m_sharedData = sharedData;

        // no inputs.
        set(1, 1);
        setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_UpdateAverageLuminanceProgram == null){
            m_UpdateAverageLuminanceProgram = m_sharedData.getUpdateAverageLuminanceProgram();

            m_PassOutputs[0] = m_ptex2DAverageLuminance;
        }

        Texture2D input = getInput(0);
        Texture2D output = getOutputTexture(0);
        output.setName("AverageLuminanceTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(m_UpdateAverageLuminanceProgram);
        SMiscDynamicParams MiscDynamicParams = m_sharedData.m_MiscDynamicParams;
        MiscDynamicParams.fElapsedTime = m_sharedData.m_CommonFrameAttribs.elapsedTime;

        m_sharedData.setUniforms(m_UpdateAverageLuminanceProgram);

        context.bindTexture(input, RenderTechnique.TEX2D_LOW_RES_LUMINACE, 0);

        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.width = 1;
        out.height = 1;
        out.format = GLenum.GL_R16F;

        super.computeOutDesc(index, out);
    }

    @Override
    public void dispose() {
        if(m_ptex2DAverageLuminance != null){
            m_ptex2DAverageLuminance.dispose();
            m_ptex2DAverageLuminance = null;
            m_PassOutputs[0] = null;
        }
    }
}
