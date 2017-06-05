package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017-05-19 11:32:20.
 */

final class PostProcessingInterpolateInsctrIrradiancePass extends PostProcessingRenderPass{

    private RenderTechnique m_InterpolateIrradianceProgram = null;
    private SharedData m_sharedData;

    public PostProcessingInterpolateInsctrIrradiancePass(SharedData sharedData) {
        super("GenerateSliceEndpoints");

        m_sharedData = sharedData;

        // no inputs.
        set(2, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_InterpolateIrradianceProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_InterpolateIrradianceProgram = m_sharedData.getInterpolateIrradianceProgram();
        }

        Texture2D output = getOutputTexture(0);
        output.setName("InterpolateInsctrIrradiance");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(m_InterpolateIrradianceProgram);
        m_sharedData.setUniforms(m_InterpolateIrradianceProgram);

//        m_ptex2DInitialScatteredLightSRV.bind(RenderTechnique.TEX2D_INIT_INSCTR, m_psamLinearClamp);
//        m_ptex2DInterpolationSourcesSRV.bind(RenderTechnique.TEX2D_INTERP_SOURCE, m_psamLinearClamp);
        Texture2D ptex2DInitialScatteredLightSRV = getInput(0);
        Texture2D ptex2DInterpolationSourcesSRV = getInput(1);
        context.bindTexture(ptex2DInitialScatteredLightSRV, RenderTechnique.TEX2D_INITIAL_IRRANDIANCE, m_sharedData.m_psamLinearClamp);
        context.bindTexture(ptex2DInterpolationSourcesSRV, RenderTechnique.TEX2D_INTERPOLATION_SOURCE, m_sharedData.m_psamLinearClamp);

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
        out.width = m_sharedData.m_ScatteringInitAttribs.m_uiMaxSamplesInSlice;
        out.height = m_sharedData.m_ScatteringInitAttribs.m_uiNumEpipolarSlices;
        out.format = GLenum.GL_RGBA16F;  // 16FP or 32FP
    }
}
