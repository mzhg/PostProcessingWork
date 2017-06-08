package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.PostProcessingRenderPassOutputTarget;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.Texture3D;

/**
 * Created by mazhen'gui on 2017-05-19 13:50:24.
 */

final class PostProcessingFixInscatteringAtDepthBreaksPass extends PostProcessingRenderPass{

    private RenderTechnique m_fixInsctrAtDepthBreaksProgram = null;
    private SharedData m_sharedData;
    private final int m_uiMaxStepsAlongRay;

    private final Texture2D[] m_RenderTargets;

    public PostProcessingFixInscatteringAtDepthBreaksPass(SharedData sharedData, int iMaxStepsAlongRay,
                                                          Texture2D colorTexture, Texture2D depthStencilTexture, boolean bEnableLightShafts) {
        super("FixInscatteringAtDepthBreaks");

        m_sharedData = sharedData;
        m_uiMaxStepsAlongRay = iMaxStepsAlongRay;


        // With Light Shaft
//        Uniform [sampler2D name=g_tex2DAverageLuminance, location=31, value = 20]
//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=32, value = 1]
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=33, value = 10]
//        Uniform [sampler3D name=g_tex3DHighOrderSctrLUT, location=35, value = 15]
//        Uniform [sampler3D name=g_tex3DSingleSctrLUT, location=36, value = 14]
//        Uniform [sampler2DArrayShadow name=g_tex2DShadowMapArray, location=34, value = 22]

        // Without Light Shaft
//        Uniform [sampler2D name=g_tex2DAverageLuminance, location=15, value = 20]
//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=16, value = 1]
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=17, value = 10]
//        Uniform [sampler3D name=g_tex3DMultipleSctrLUT, location=18, value = 16]

        if(colorTexture != null)
            setOutputTarget(PostProcessingRenderPassOutputTarget.INTERNAL);

        // no inputs.
        int inputCount = bEnableLightShafts ? 6 : 4;
        set(inputCount, 1);

        if(depthStencilTexture != null || colorTexture != null){
            m_RenderTargets = new Texture2D[2];
            m_RenderTargets[0] = colorTexture;
            m_RenderTargets[1] = depthStencilTexture;
        }else{
            m_RenderTargets = null;
        }
    }

    private void bindTextures(PostProcessingRenderContext context){
        if(m_sharedData.m_ScatteringInitAttribs.m_bEnableLightShafts) {
            final int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
            // With Light Shaft
//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=32, value = 1]
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=33, value = 10]
//        Uniform [sampler2DArrayShadow name=g_tex2DShadowMapArray, location=34, value = 22]
//        Uniform [sampler3D name=g_tex3DHighOrderSctrLUT, location=35, value = 15]
//        Uniform [sampler3D name=g_tex3DSingleSctrLUT, location=36, value = 14]
//        Uniform [sampler2D name=g_tex2DAverageLuminance, location=31, value = 20]

            Texture2D tex2DCamSpaceZ = getInput(0);
            Texture2D tex2DColorBuffer = getInput(1);
            Texture2D tex2DLightSpaceDepthMap = getInput(2);
            Texture3D tex3DHighOrderSctrLUT = getInput(3);
            Texture3D tex3DSingleSctrLUT = getInput(4);
            Texture2D tex2DAverageLuminance = getInput(5);  // optional

//            FrameAttribs.ptex2DSrcColorBuffer.bind(ScatteringRenderTechnique.TEX2D_COLOR, 0);
//            FrameAttribs.ptex2DShadowMapSRV.bind(ScatteringRenderTechnique.TEX2D_SHADOW_MAP, m_psamComparison);
//            m_ptex2DCameraSpaceZ.bind(ScatteringRenderTechnique.TEX2D_CAM_SPACE, 0);
//            m_ptex2DOccludedNetDensityToAtmTop.bind(ScatteringRenderTechnique.TEX2D_OCCLUDED_NET_DENSITY, 0);
//            m_ptex3DHighOrderScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_HIGH_ORDER_LUT, 0);
//            m_ptex3DSingleScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_SINGLE_LUT, 0);
//            m_ptex3DMultipleScatteringSRV.bind(ScatteringRenderTechnique.TEX3D_MULTIPLE_LUT, 0);
//            m_ptex2DAverageLuminance.bind(ScatteringRenderTechnique.TEX2D_AVERAGE_LUMINACE, 0); // For tone-mapping

            context.bindTexture(tex2DCamSpaceZ, RenderTechnique.TEX2D_CAM_SPACE, 0);
            context.bindTexture(tex2DColorBuffer, RenderTechnique.TEX2D_COLOR, 0);
            context.bindTexture(tex2DLightSpaceDepthMap, RenderTechnique.TEX2D_SHADOW_MAP, m_sharedData.m_psamComparison);
            context.bindTexture(tex3DHighOrderSctrLUT, RenderTechnique.TEX3D_HIGH_ORDER_LUT, 0);
            context.bindTexture(tex3DSingleSctrLUT, RenderTechnique.TEX3D_SINGLE_LUT, 0);
            if (tex2DAverageLuminance != null)
                context.bindTexture(tex2DAverageLuminance, RenderTechnique.TEX2D_AVERAGE_LUMINACE, 0);
        }else{
            // Without Light Shaft
//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=16, value = 1]
//        Uniform [sampler2D name=g_tex2DColorBuffer, location=17, value = 10]
//        Uniform [sampler3D name=g_tex3DMultipleSctrLUT, location=18, value = 16]
//        Uniform [sampler2D name=g_tex2DAverageLuminance, location=15, value = 20]

            Texture2D tex2DCamSpaceZ = getInput(0);
            Texture2D tex2DColorBuffer = getInput(1);
            Texture3D tex3DMultipleSctrLUT = getInput(2);
            Texture2D tex2DAverageLuminance = getInput(3);  // optional

            context.bindTexture(tex2DCamSpaceZ, RenderTechnique.TEX2D_CAM_SPACE, 0);
            context.bindTexture(tex2DColorBuffer, RenderTechnique.TEX2D_COLOR, 0);
            context.bindTexture(tex3DMultipleSctrLUT, RenderTechnique.TEX3D_MULTIPLE_LUT, 0);
            if (tex2DAverageLuminance != null)
                context.bindTexture(tex2DAverageLuminance, RenderTechnique.TEX2D_AVERAGE_LUMINACE, 0);
        }
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(m_fixInsctrAtDepthBreaksProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            m_fixInsctrAtDepthBreaksProgram = m_sharedData.getFixInsctrAtDepthBreaksProgram();
        }

        Texture2D output = getOutputTexture(0);

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(m_fixInsctrAtDepthBreaksProgram);
        SMiscDynamicParams MiscDynamicParams = m_sharedData.m_MiscDynamicParams;
        MiscDynamicParams.fMaxStepsAlongRay = m_uiMaxStepsAlongRay;
        m_sharedData.setUniforms(m_fixInsctrAtDepthBreaksProgram, MiscDynamicParams);
        bindTextures(context);

        context.setBlendState(null);
        m_sharedData.m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilRef = 0;
        m_sharedData.m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilRef = 0;
        context.setDepthStencilState(m_sharedData.m_pNoDepth_StEqual_KeepStencilDS);
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
        out.arraySize = m_sharedData.m_ScatteringInitAttribs.m_uiBackBufferWidth;
        out.sampleCount = m_sharedData.m_ScatteringInitAttribs.m_uiBackBufferHeight;
        out.format = GLenum.GL_RGBA8;

        super.computeOutDesc(index, out);
    }
}
