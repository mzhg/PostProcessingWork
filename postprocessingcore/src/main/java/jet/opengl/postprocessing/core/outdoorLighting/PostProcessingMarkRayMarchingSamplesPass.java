package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

final class PostProcessingMarkRayMarchingSamplesPass extends PostProcessingRenderPass{

    private RenderTechnique m_MarkRayMarchingSamplesInStencilProgram = null;
    private SharedData m_sharedData;

    public PostProcessingMarkRayMarchingSamplesPass(SharedData sharedData) {
        super("MarkRayMarchingSamples");

        m_sharedData = sharedData;

        // no inputs.
        set(1, 0);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_MarkRayMarchingSamplesInStencilProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_MarkRayMarchingSamplesInStencilProgram = m_sharedData.getMarkRayMarchingSamplesInStencilProgram();
        }

        Texture2D input = getInput(0);
        Texture2D output = m_sharedData.getEpipolarImageDSV();
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(m_MarkRayMarchingSamplesInStencilProgram);
        m_sharedData.setUniforms(m_MarkRayMarchingSamplesInStencilProgram);

        context.bindTexture(input, RenderTechnique.TEX2D_INTERPOLATION_SOURCE, m_sharedData.m_psamLinearClamp);

        context.setBlendState(null);

        // Mark ray marching samples in the stencil
        // The depth stencil state is configured to pass only pixels, whose stencil value equals 1. Thus all epipolar samples with
        // coordinates outsied the screen (generated on the previous pass) are automatically discarded. The pixel shader only
        // passes samples which are interpolated from themselves, the rest are discarded. Thus after this pass all ray
        // marching samples will be marked with 2 in stencil
        m_sharedData.m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilRef = 1;
        m_sharedData.m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilRef = 1;
        context.setDepthStencilState(m_sharedData.m_pNoDepth_StEqual_IncrStencilDS);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }
}
