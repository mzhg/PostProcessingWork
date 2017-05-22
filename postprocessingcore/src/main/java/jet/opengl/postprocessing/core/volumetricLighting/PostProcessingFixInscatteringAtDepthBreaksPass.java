package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017-05-19 13:50:24.
 */

final class PostProcessingFixInscatteringAtDepthBreaksPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram m_fixInsctrAtDepthBreaksProgram = null;
    private SharedData m_sharedData;
    private final boolean m_bAttenuateBackground;
    private final SMiscDynamicParams m_MiscDynamicParams = new SMiscDynamicParams();
    private final int m_uiMaxStepsAlongRay;

    private final Texture2D[] m_RenderTargets;

    public PostProcessingFixInscatteringAtDepthBreaksPass(SharedData sharedData, boolean bAttenuateBackground, int iMaxStepsAlongRay,
                                                          Texture2D colorTexture,  Texture2D depthStencilTexture) {
        super("FixInscatteringAtDepthBreaks");

        m_bAttenuateBackground = bAttenuateBackground;
        m_sharedData = sharedData;
        m_uiMaxStepsAlongRay = iMaxStepsAlongRay;

//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=18, value = 1]
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=19, value = 11]
//        Uniform [sampler2DShdow name=g_tex2DLightSpaceDepthMap, location=20, value = 6]
//        Uniform [sampler2D name=g_tex2DPrecomputedPointLightInsctr, location=21, value = 14]

        if(colorTexture != null)
            setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);

        // no inputs.
        int inputCount = m_sharedData.m_ScatteringInitAttribs.m_uiLightType == LightType.DIRECTIONAL ? 3 : 4;
        set(inputCount, 1);

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
        if(m_fixInsctrAtDepthBreaksProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_fixInsctrAtDepthBreaksProgram = m_sharedData.getFixInsctrAtDepthBreaksProgram(m_bAttenuateBackground);
        }

        Texture2D output = getOutputTexture(0);

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(m_fixInsctrAtDepthBreaksProgram);
        SMiscDynamicParams MiscDynamicParams = m_MiscDynamicParams;
        MiscDynamicParams.fMaxStepsAlongRay = m_uiMaxStepsAlongRay;
        m_sharedData.setUniforms(m_fixInsctrAtDepthBreaksProgram, m_MiscDynamicParams);

        Texture2D tex2DCamSpaceZ = getInput(0);
        Texture2D tex2DColorBuffer = getInput(1);
        Texture2D tex2DLightSpaceDepthMap = getInput(2);
        Texture2D tex2DPrecomputedPointLightInsctr = getInput(3);

//        m_ptex2DCameraSpaceZSRV.bind(RenderTechnique.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
//        frameAttribs.ptex2DSrcColorBufferSRV.bind(RenderTechnique.TEX2D_COLOR_BUFFER, m_psamLinearClamp);
//        m_ptex2DSliceUVDirAndOriginSRV.bind(RenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
//        frameAttribs.ptex2DShadowMapSRV.bind(RenderTechnique.TEX2D_SHADOWM_MAP, m_psamComparison);
//        m_ptex2DMinMaxShadowMapSRV[0].bind(RenderTechnique.TEX2D_MIN_MAX_DEPTH, m_psamLinearClamp);
//        if(frameAttribs.ptex2DStainedGlassSRV != null)
//            frameAttribs.ptex2DStainedGlassSRV.bind(RenderTechnique.TEX2D_STAINED_DEPTH, m_psamLinearClamp);
//        if(m_ptex2DPrecomputedPointLightInsctrSRV != null)
//            m_ptex2DPrecomputedPointLightInsctrSRV.bind(RenderTechnique.TEX2D_PRECOMPUTED_INSCTR, m_psamLinearClamp);
        int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
        context.bindTexture(tex2DCamSpaceZ, VolumetricLightingProgram.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
        context.bindTexture(tex2DColorBuffer, VolumetricLightingProgram.TEX2D_COLOR_BUFFER, m_psamLinearClamp);
        context.bindTexture(tex2DLightSpaceDepthMap, VolumetricLightingProgram.TEX2D_SHADOWM_MAP, m_sharedData.m_psamComparison);
        if(tex2DPrecomputedPointLightInsctr != null)
            context.bindTexture(tex2DPrecomputedPointLightInsctr, VolumetricLightingProgram.TEX2D_PRECOMPUTED_INSCTR, m_psamLinearClamp);

        context.setBlendState(null);
        m_sharedData.m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilRef = 0;
        m_sharedData.m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilRef = 0;
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        if(m_RenderTargets != null){
            if(m_RenderTargets[0] == null)
                m_RenderTargets[0] = output;
            context.setRenderTargets(m_RenderTargets);
        }else{
            context.setRenderTarget(output);
        }

        context.drawFullscreenQuad();
    }

    @Override
    public Texture2D getOutputTexture(int idx) {
        if(getOutputTarget() == PostProcessingRenderPassOutputTarget.INTERNAL){
            return idx == 0 ? m_RenderTargets[0] : null;
        }else {
            return super.getOutputTexture(idx);
        }
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.format = GLenum.GL_RGBA8;

        super.computeOutDesc(index, out);
    }
}
