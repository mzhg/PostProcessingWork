package jet.opengl.postprocessing.core.volumetricLighting;

import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Input0: camera space z<br>
 * Input1: coordinates<br>
 * Input2: light space shadow map<br>
 * Input3: min max light space depth<br>
 * Input4: slice-uv-dir and origin<p>
 * Input5: precomputed point light insctr<br>
 * Output: InitialScatteredLight<p>
 * Created by mazhen'gui on 2017-05-19 09:48:42.
 */

final class PostProcessingRayMarchingPass extends PostProcessingRenderPass{

    private VolumetricLightingProgram g_DoRayMarchProgram = null;
    private SharedData m_sharedData;
    private final SMiscDynamicParams m_MiscDynamicParams = new SMiscDynamicParams();
    private final Texture2D[] m_RenderTargets = new Texture2D[2];

    public PostProcessingRayMarchingPass(SharedData sharedData) {
        super("DoRayMarching");

        m_sharedData = sharedData;

//        Uniform [sampler2D name=g_tex2DCamSpaceZ, location=12, value = 1]
//        Uniform [sampler2D name=g_tex2DCoordinates, location=13, value = 3]
//        Uniform [sampler2DShdow name=g_tex2DLightSpaceDepthMap, location=14, value = 6]
//        Uniform [sampler2D name=g_tex2DMinMaxLightSpaceDepth, location=15, value = 8]
//        Uniform [sampler2D name=g_tex2DPrecomputedPointLightInsctr, location=16, value = 14]
//        Uniform [sampler2D name=g_tex2DSliceUVDirAndOrigin, location=17, value = 7]
        // no inputs.
        int inputCount = sharedData.m_ScatteringInitAttribs.m_uiLightType == LightType.DIRECTIONAL ? 5 : 6;
        set(inputCount, 1);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        if(g_DoRayMarchProgram == null /*|| g_GenerateSliceEndpointsProgram.getProgram() == 0*/){
            g_DoRayMarchProgram = m_sharedData.getRayMarchProgram();
        }

        Texture2D output = getOutputTexture(0);
//        m_ptex2DCameraSpaceZSRV.bind(RenderTechnique.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
//        m_ptex2DCoordianteTextureSRV.bind(RenderTechnique.TEX2D_COORDINATES, m_psamLinearClamp);
//        m_ptex2DSliceUVDirAndOriginSRV.bind(RenderTechnique.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
//        frameAttribs.ptex2DShadowMapSRV.bind(RenderTechnique.TEX2D_SHADOWM_MAP, m_psamComparison);
//        m_ptex2DMinMaxShadowMapSRV[0].bind(RenderTechnique.TEX2D_MIN_MAX_DEPTH, m_psamLinearClamp);
//        if(frameAttribs.ptex2DStainedGlassSRV != null)
//            frameAttribs.ptex2DStainedGlassSRV.bind(RenderTechnique.TEX2D_STAINED_DEPTH, m_psamLinearClamp);
//        if(m_ptex2DPrecomputedPointLightInsctrSRV != null)
//            m_ptex2DPrecomputedPointLightInsctrSRV.bind(RenderTechnique.TEX2D_PRECOMPUTED_INSCTR, m_psamLinearClamp);
        Texture2D ptex2DCameraSpaceZSRV = getInput(0);
        Texture2D ptex2DCoordianteTextureSRV = getInput(1);
        Texture2D ptex2DSliceUVDirAndOriginSRV = getInput(4);
        Texture2D ptex2DShadowMapSRV = getInput(2);
        Texture2D ptex2DMinMaxShadowMapSRV = getInput(3);

        // binding textures.
        int m_psamLinearClamp = m_sharedData.m_psamLinearClamp;
        context.bindTexture(ptex2DCameraSpaceZSRV, VolumetricLightingProgram.TEX2D_CAM_SPACEZ, m_psamLinearClamp);
        context.bindTexture(ptex2DCoordianteTextureSRV, VolumetricLightingProgram.TEX2D_COORDINATES, m_psamLinearClamp);
        context.bindTexture(ptex2DSliceUVDirAndOriginSRV, VolumetricLightingProgram.TEX2D_SLICE_UV_ORIGIN, m_psamLinearClamp);
        context.bindTexture(ptex2DShadowMapSRV, VolumetricLightingProgram.TEX2D_SHADOWM_MAP, m_sharedData.m_psamComparison);
        context.bindTexture(ptex2DMinMaxShadowMapSRV, VolumetricLightingProgram.TEX2D_MIN_MAX_DEPTH, m_psamLinearClamp);

        if(m_sharedData.m_ScatteringInitAttribs.m_uiLightType != LightType.DIRECTIONAL) {
            Texture2D ptex2DPrecomputedPointLightInsctrSRV = getInput(5);
            context.bindTexture(ptex2DPrecomputedPointLightInsctrSRV, VolumetricLightingProgram.TEX2D_PRECOMPUTED_INSCTR, m_psamLinearClamp);
        }

        SMiscDynamicParams MiscDynamicParams = m_MiscDynamicParams;
        MiscDynamicParams.fMaxStepsAlongRay = m_sharedData.m_LightAttribs.m_uiShadowMapResolution;

        output.setName("RayMarchingTexture");
        context.setViewport(0,0, output.getWidth(), output.getHeight());
//        context.setViewport(0,0, 1280, 720);
        context.setVAO(null);
        context.setProgram(g_DoRayMarchProgram);
        m_sharedData.setUniforms(g_DoRayMarchProgram, MiscDynamicParams);

        context.setBlendState(null);
        m_RenderTargets[0] = output;
        m_RenderTargets[1] = m_sharedData.getEpipolarImageDSV();
        context.setRenderTargets(m_RenderTargets);

        // Depth stencil view now contains 2 for these pixels, for which ray marchings is to be performed
        // Depth stencil state is configured to pass only these pixels and discard the rest
        m_sharedData.m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilRef = 2;
        m_sharedData.m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilRef = 2;
        context.setDepthStencilState(m_sharedData.m_pNoDepth_StEqual_IncrStencilDS);
        context.setRasterizerState(null);

        context.drawFullscreenQuad();
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        out.arraySize = 1;
        out.sampleCount = 1;
        out.width = m_sharedData.getEpipolarImageDSV().getWidth();
        out.height = m_sharedData.getEpipolarImageDSV().getHeight();
        out.format = GLenum.GL_RGB16F;
    }
}
