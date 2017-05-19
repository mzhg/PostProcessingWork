package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
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

final class PostProcessingUpscaleInscatteringRadiancePass extends PostProcessingRenderPass{

    private VolumetricLightingProgram m_UpscaleInsctrdRadianceProgram = null;
    private SharedData m_sharedData;
    private final Texture2D[] m_RenderTargets;

    public PostProcessingUpscaleInscatteringRadiancePass(SharedData sharedData, Texture2D colorTexture, Texture2D depthStencilTexture) {
        super("UpscaleInscatteringRadiance");

        m_sharedData = sharedData;

        // no inputs.
        set(3, 1);

        if(colorTexture != null){
            setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);
        }

        if(depthStencilTexture != null || colorTexture != null){
            m_RenderTargets = new Texture2D[2];
            m_RenderTargets[0] = colorTexture;
            m_RenderTargets[1] = depthStencilTexture;
        }else{
            m_RenderTargets = null;
        }
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_UpscaleInsctrdRadianceProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_UpscaleInsctrdRadianceProgram = m_sharedData.getUpscaleInsctrdRadianceProgram();
        }

        Texture2D output = getOutputTexture(0);
        if(output == null)
            output = m_RenderTargets[0];

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(m_UpscaleInsctrdRadianceProgram);
        m_sharedData.setUniforms(m_UpscaleInsctrdRadianceProgram);

//        m_ptex2DCameraSpaceZSRV.bind(RenderTechnique.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
//        frameAttribs.ptex2DSrcColorBufferSRV.bind(RenderTechnique.TEX2D_COLOR_BUFFER, m_psamLinearClamp);
//        m_ptex2DDownscaledScatteredLightSRV.bind(RenderTechnique.TEX2D_DOWNSCALED_INSCTR, m_psamLinearClamp);
        Texture2D ptex2DCameraSpaceZSRV = getInput(0);
        Texture2D ptex2DSrcColorBufferSRV = getInput(1);
        Texture2D ptex2DDownscaledScatteredLightSRV = getInput(2);

        int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
        context.bindTexture(ptex2DCameraSpaceZSRV, VolumetricLightingProgram.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
        context.bindTexture(ptex2DSrcColorBufferSRV, VolumetricLightingProgram.TEX2D_COLOR_BUFFER, m_psamLinearClamp);
        context.bindTexture(ptex2DDownscaledScatteredLightSRV, VolumetricLightingProgram.TEX2D_DOWNSCALED_INSCTR, m_psamLinearClamp);

        context.setBlendState(null);
        context.setDepthStencilState(m_sharedData.m_pDisableDepthTestIncrStencilDS);
        context.setRasterizerState(null);
        if(m_RenderTargets != null){
            if(m_RenderTargets[0] == null)
                m_RenderTargets[0] = output;
            context.setRenderTargets(m_RenderTargets);
        }else{
            context.setRenderTarget(output);
        }
        GLFuncProviderFactory.getGLFuncProvider().glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 0.0f, 0);

        context.drawFullscreenQuad();
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
//        out.width = m_sharedData.m_ScatteringAttribs.m_uiNumEpipolarSlices;
//        out.height = 1;
        out.format = GLenum.GL_RGBA8;

        super.computeOutDesc(index, out);
    }
}
