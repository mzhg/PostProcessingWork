package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/5/17.
 */

final class PostProcessingRenderSampleLocationsPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram m_RenderSampleLocationsProgram = null;
    private SharedData m_sharedData;
    private final BlendState m_bsstate = new BlendState();

    public PostProcessingRenderSampleLocationsPass(SharedData sharedData) {
        super("RenderSampleLocations");

        m_sharedData = sharedData;

        // no inputs.
        set(2, 1);

        m_bsstate.blendEnable = true;
        m_bsstate.srcBlend = GLenum.GL_SRC_ALPHA;
        m_bsstate.srcBlendAlpha = GLenum.GL_ONE;
        m_bsstate.destBlend = GLenum.GL_ONE_MINUS_SRC_ALPHA;
        m_bsstate.destBlendAlpha = GLenum.GL_ZERO;
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_RenderSampleLocationsProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_RenderSampleLocationsProgram = m_sharedData.getRenderSampleLocationsProgram();
        }

        Texture2D output = getOutputTexture(0);
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(m_RenderSampleLocationsProgram);
        m_sharedData.setUniforms(m_RenderSampleLocationsProgram);

        context.setBlendState(m_bsstate);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawArrays(GLenum.GL_POINTS, 0, m_sharedData.m_ScatteringInitAttribs.m_uiMaxSamplesInSlice * m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices);
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
//        out.width = m_sharedData.m_ScatteringAttribs.m_uiNumEpipolarSlices;
//        out.height = 1;
        out.format = GLenum.GL_RGBA8;  // 16FP or 32FP

        super.computeOutDesc(index, out);
    }
}
