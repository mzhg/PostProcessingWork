package jet.opengl.postprocessing.core.volumetricLighting;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.util.Arrays;

import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Hold the shared data for the rendering pass.
 *
 * Created by mazhen'gui on 2017/5/17.
 */

final class SharedData {
    LightScatteringFrameAttribs m_ScatteringFrameAttribs = null;
    LightScatteringInitAttribs  m_ScatteringInitAttribs = null;
    private PostProcessingFrameAttribs  m_CommonFrameAttribs = null;
    final SLightAttribs         m_LightAttribs = new SLightAttribs();
    private final SParticipatingMediaScatteringParams m_MediaParams = new SParticipatingMediaScatteringParams();

    private Macro[] m_Macros = null;
    private Macro[] m_MacrosWithFrag = null;

    // Shadred Stencil mask texture2D
    private Texture2D m_ptex2DEpipolarImageDSV;
    private Texture2D m_ptex2DDownsampleDSV;
    private Texture2D m_ptex2DDownsampleRTV;

    private Texture2D m_ptex2DScreenSizedDSV;
    private Texture2D m_ptex2DScreenSizedRTV;

    private VolumetricLightingProgram m_ReconstrCamSpaceZTech;
    private VolumetricLightingProgram m_RendedSliceEndpointsTech;
    private VolumetricLightingProgram m_RendedCoordTexTech;
    private VolumetricLightingProgram m_RefineSampleLocationsTech;
    private VolumetricLightingProgram m_MarkRayMarchingSamplesInStencilTech;
    private VolumetricLightingProgram m_RenderSliceUVDirInSMTech;
    private VolumetricLightingProgram m_InitializeMinMaxShadowMapTech;
    private VolumetricLightingProgram m_ComputeMinMaxSMLevelTech;
    /** 0 - min/max optimization disabled; 1 - min/max optimization enabled */
    private VolumetricLightingProgram[] m_DoRayMarchTech = new VolumetricLightingProgram[2];
    private VolumetricLightingProgram m_InterpolateIrradianceTech;
    /**
     * 0 - Unwarp inscattering image from epipolar coordinates only<br>
     * 1 - Unwarp inscattering image from epipolar coordinates to rectangular + apply it to attenuate background
     */
    private VolumetricLightingProgram[] m_UnwarpEpipolarSctrImgTech = new VolumetricLightingProgram[2];

    /**
     * 0 - Fix inscattering image at depth breaks by doing ray marching only<br>
     * 1 - Fix inscattering image + apply it to attenuate background
     */
    private VolumetricLightingProgram[] m_FixInsctrAtDepthBreaksTech = new VolumetricLightingProgram[2];
    private VolumetricLightingProgram m_UpscaleInsctrdRadianceTech;
    private VolumetricLightingProgram m_RenderSampleLocationsTech;

    int m_psamLinearClamp = 0;
    int m_psamLinearBorder0 = 0;
    int m_psamLinearUClampVWrap = 0;
    int m_psamComparison = 0;

    int m_uiSampleRefinementCSThreadGroupSize;
    int m_uiSampleRefinementCSMinimumThreadGroupSize = 128;

    boolean m_bRecomputeSctrCoeffs;

    // stencil state
    DepthStencilState m_pDisableDepthTestIncrStencilDS;
    DepthStencilState m_pNoDepth_StEqual_IncrStencilDS;

    SharedData(PostProcessingFrameAttribs frameAttribs){
        m_CommonFrameAttribs = frameAttribs;
        m_pDisableDepthTestIncrStencilDS = new DepthStencilState();
        m_pDisableDepthTestIncrStencilDS.depthEnable = false;
        m_pDisableDepthTestIncrStencilDS.stencilEnable = true;
        m_pDisableDepthTestIncrStencilDS.backFace.stencilFailOp = GLenum.GL_INCR;
        m_pDisableDepthTestIncrStencilDS.backFace.stencilDepthFailOp = GLenum.GL_INCR;
        m_pDisableDepthTestIncrStencilDS.backFace.stencilPassOp = GLenum.GL_INCR;
        m_pDisableDepthTestIncrStencilDS.backFace.stencilFunc = GLenum.GL_ALWAYS;

        m_pDisableDepthTestIncrStencilDS.frontFace.stencilFailOp = GLenum.GL_INCR;
        m_pDisableDepthTestIncrStencilDS.frontFace.stencilDepthFailOp = GLenum.GL_INCR;
        m_pDisableDepthTestIncrStencilDS.frontFace.stencilPassOp = GLenum.GL_INCR;
        m_pDisableDepthTestIncrStencilDS.frontFace.stencilFunc = GLenum.GL_ALWAYS;

        m_pNoDepth_StEqual_IncrStencilDS = new DepthStencilState();
        m_pNoDepth_StEqual_IncrStencilDS.depthEnable = false;
        m_pNoDepth_StEqual_IncrStencilDS.stencilEnable = true;
        m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilDepthFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilPassOp = GLenum.GL_INCR;
        m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilFunc = GLenum.GL_EQUAL;
        m_pNoDepth_StEqual_IncrStencilDS.backFace.stencilRef = 1;

        m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilDepthFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilPassOp = GLenum.GL_INCR;
        m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilFunc = GLenum.GL_EQUAL;
        m_pNoDepth_StEqual_IncrStencilDS.frontFace.stencilRef = 1;
    }

    /**
     * Prepare the parameters for the light rendering.
     * @param initAttribs
     * @param frameAttribs
     */
    void prepare(PostProcessingFrameAttribs commonAttribs, LightScatteringInitAttribs initAttribs, LightScatteringFrameAttribs frameAttribs){
        boolean bReconstructMacros = false;
        if(m_ScatteringInitAttribs == null){
            m_ScatteringInitAttribs = new LightScatteringInitAttribs();
            bReconstructMacros = true;
        }

        if(!bReconstructMacros){
            bReconstructMacros = (!initAttribs.equals(m_ScatteringInitAttribs));  // TODO  Redundant check
        }

        if(bReconstructMacros){
            m_ScatteringInitAttribs.set(initAttribs);

            m_Macros = new Macro[9];
            m_Macros[0] = new Macro("NUM_EPIPOLAR_SLICES", m_ScatteringInitAttribs.m_uiNumEpipolarSlices);
            m_Macros[1] = new Macro("MAX_SAMPLES_IN_SLICE", m_ScatteringInitAttribs.m_uiMaxSamplesInSlice);
            m_Macros[2] = new Macro("OPTIMIZE_SAMPLE_LOCATIONS", m_ScatteringInitAttribs.m_bOptimizeSampleLocations?1:0);
            m_Macros[3] = new Macro("LIGHT_TYPE", m_ScatteringInitAttribs.m_uiLightType.ordinal());
            m_Macros[4] = new Macro("STAINED_GLASS", m_ScatteringInitAttribs.m_bStainedGlass?1:0);
            m_Macros[5] = new Macro("ACCEL_STRUCT", m_ScatteringInitAttribs.m_uiAccelStruct.ordinal());
            m_Macros[6] = new Macro("INSCTR_INTGL_EVAL_METHOD", m_ScatteringInitAttribs.m_uiInsctrIntglEvalMethod.ordinal());
            m_Macros[7] = new Macro("ANISOTROPIC_PHASE_FUNCTION", m_ScatteringInitAttribs.m_bAnisotropicPhaseFunction?1:0);
            m_Macros[8] = new Macro("SCREEN_RESLOUTION", "float2("+m_ScatteringInitAttribs.m_uiBackBufferWidth+","+m_ScatteringInitAttribs.m_uiBackBufferHeight+")");

            m_MacrosWithFrag = Arrays.copyOf(m_Macros, m_Macros.length + 1);
            m_MacrosWithFrag[m_Macros.length] = new Macro("__FRAG_SHADER__", 1);
        }

        m_bRecomputeSctrCoeffs = false;

        if(m_ScatteringFrameAttribs == null){
            m_ScatteringFrameAttribs = new LightScatteringFrameAttribs();
            m_bRecomputeSctrCoeffs = true;
        }

        if(!m_bRecomputeSctrCoeffs) {
            m_bRecomputeSctrCoeffs = m_ScatteringFrameAttribs.m_fDistanceScaler != frameAttribs.m_fDistanceScaler ||
                    !m_ScatteringFrameAttribs.m_f4RayleighBeta.equals(frameAttribs.m_f4RayleighBeta) ||
                    !m_ScatteringFrameAttribs.m_f4MieBeta.equals(frameAttribs.m_f4MieBeta)
//                   || m_ScatteringInitAttribs.m_uiInsctrIntglEvalMethod != initAttribs.m_uiInsctrIntglEvalMethod ||  TODO  Redundant check
//                    m_ScatteringInitAttribs.m_bAnisotropicPhaseFunction != initAttribs.m_bAnisotropicPhaseFunction
                    ;
        }

        if(m_bRecomputeSctrCoeffs){
            computeScatteringCoefficients();
        }

        m_ScatteringFrameAttribs.set(frameAttribs);
        calculateLightAttribs(commonAttribs, frameAttribs);

        createSamplers();
    }

    private void createSamplers(){
        if(m_psamLinearBorder0 != 0)
            return;

        // Create samplers
        {
            SamplerDesc SamLinearBorder0Desc = new SamplerDesc
            (
                    GLenum.GL_LINEAR,
                    GLenum.GL_LINEAR,
                    GLenum.GL_CLAMP_TO_BORDER,
                    GLenum.GL_CLAMP_TO_BORDER,
                    GLenum.GL_CLAMP_TO_BORDER,
                    0,    // border color
                    0,    // MaxAnisotropy
                    0,    // ComparisonFunc
                    0     // ComparisonMode
            );

            m_psamLinearBorder0 = SamplerUtils.createSampler(SamLinearBorder0Desc);
        }

        {
            SamplerDesc SamLinearClampDesc = new SamplerDesc
            (
                    GLenum.GL_LINEAR,
                    GLenum.GL_LINEAR,
                    GLenum.GL_CLAMP_TO_EDGE,
                    GLenum.GL_CLAMP_TO_EDGE,
                    GLenum.GL_CLAMP_TO_EDGE,
                    0,    // border color
                    0,    // MaxAnisotropy
                    0,    // ComparisonFunc
                    0     // ComparisonMode
            );

            m_psamLinearClamp = SamplerUtils.createSampler(SamLinearClampDesc);
        }

        {
            SamplerDesc SamLinearUClampVWrapDesc = new SamplerDesc
            (
                    GLenum.GL_LINEAR,
                    GLenum.GL_LINEAR,
                    GLenum.GL_CLAMP_TO_EDGE,
                    GLenum.GL_REPEAT,
                    GLenum.GL_CLAMP_TO_EDGE,
                    0,    // border color
                    0,    // MaxAnisotropy
                    0,    // ComparisonFunc
                    0     // ComparisonMode
            );

            m_psamLinearUClampVWrap = SamplerUtils.createSampler(SamLinearUClampVWrapDesc);
        }

        {
            SamplerDesc SamComparisonDesc = new SamplerDesc
            (
                    GLenum.GL_LINEAR,
                    GLenum.GL_LINEAR,
                    GLenum.GL_CLAMP_TO_BORDER,
                    GLenum.GL_CLAMP_TO_BORDER,
                    GLenum.GL_CLAMP_TO_BORDER,
                    0,    // border color
                    0,    // MaxAnisotropy
                    GLenum.GL_LESS,    // ComparisonFunc
                    GLenum.GL_COMPARE_REF_TO_TEXTURE   // ComparisonMode
            );

            m_psamComparison = SamplerUtils.createSampler(SamComparisonDesc);
        }
    }

    void setUniforms(VolumetricLightingProgram program){
        setUniforms(program, null);
    }

    void setUniforms(VolumetricLightingProgram program, SMiscDynamicParams miscParams){
        program.setUniform(m_ScatteringFrameAttribs);
        program.setUniform(m_ScatteringInitAttribs);
        program.setUniform(m_LightAttribs);
        program.setUniform(m_MediaParams);
        program.setUniform(m_CommonFrameAttribs);
        if(miscParams !=null)
            program.setUniform(miscParams);
    }

    VolumetricLightingProgram getRenderSampleLocationsProgram(){
        if(m_RenderSampleLocationsTech == null){
            m_RenderSampleLocationsTech = new VolumetricLightingProgram("RenderSamplePositions.vert", "RenderSamplePositions.geom", "RenderSamplePositions.frag", getMacros());
        }

        return m_RenderSampleLocationsTech;
    }

    VolumetricLightingProgram getUpscaleInsctrdRadianceProgram(){
        if(m_UpscaleInsctrdRadianceTech == null){
            m_UpscaleInsctrdRadianceTech = new VolumetricLightingProgram("UpscaleInscatteredRadiance.frag", getMacros());
        }

        return m_UpscaleInsctrdRadianceTech;
    }

    VolumetricLightingProgram getFixInsctrAtDepthBreaksProgram(boolean bAttenuateBackground){
        VolumetricLightingProgram fixInsctrAtDepthBreaksTech = m_FixInsctrAtDepthBreaksTech[bAttenuateBackground ? 1 : 0];
        if(fixInsctrAtDepthBreaksTech == null){
            fixInsctrAtDepthBreaksTech = m_FixInsctrAtDepthBreaksTech[bAttenuateBackground ? 1 : 0]
                    = new VolumetricLightingProgram((bAttenuateBackground ? "FixAndApplyInscatteredRadiance.frag" : "FixInscatteredRadiance.frag"), getMacros());
        }

        return fixInsctrAtDepthBreaksTech;
    }

    VolumetricLightingProgram getRayMarchProgram(){
        final int slot = m_ScatteringInitAttribs.m_uiAccelStruct.ordinal() > 0 ? 1 : 0;
        VolumetricLightingProgram doRayMarchTech = m_DoRayMarchTech[slot];
        if(doRayMarchTech == null){
            String filename = slot > 0 ? "RayMarchMinMaxOpt.frag" : "RayMarch.frag";
            doRayMarchTech = m_DoRayMarchTech[slot] = new VolumetricLightingProgram(filename, getMacros());
        }

        return doRayMarchTech;
    }

    VolumetricLightingProgram getUnwarpEpipolarSctrImgTechProgram(){
        int iApplyBackground = (m_ScatteringInitAttribs.m_iDownscaleFactor == 1) ? 1 : 0;
        VolumetricLightingProgram unwarpEpipolarSctrImgTech = m_UnwarpEpipolarSctrImgTech[iApplyBackground];

        if(unwarpEpipolarSctrImgTech == null){
            String filename = iApplyBackground != 0 ? "ApplyInscatteredRadiance.frag" : "UnwarpEpipolarInsctrImage.frag";
            unwarpEpipolarSctrImgTech = m_UnwarpEpipolarSctrImgTech[iApplyBackground] = new VolumetricLightingProgram(filename, getMacros(true));
        }

        return unwarpEpipolarSctrImgTech;
    }

    VolumetricLightingProgram getInterpolateIrradianceProgram(){
        if(m_InterpolateIrradianceTech == null){
            m_InterpolateIrradianceTech = new VolumetricLightingProgram("InterpolateIrradiance.frag", getMacros());
        }

        return m_InterpolateIrradianceTech;
    }


    VolumetricLightingProgram getComputeMinMaxSMLevelProgram(){
        if(m_ComputeMinMaxSMLevelTech == null){
            m_ComputeMinMaxSMLevelTech = new VolumetricLightingProgram("ComputeMinMaxShadowMapLevel.frag", getMacros());
        }

        return m_ComputeMinMaxSMLevelTech;
    }

    VolumetricLightingProgram getInitializeMinMaxShadowMapProgram(){
        if(m_InitializeMinMaxShadowMapTech == null){
            m_InitializeMinMaxShadowMapTech = new VolumetricLightingProgram("InitializeMinMaxShadowMap.frag", getMacros());
        }

        return m_InitializeMinMaxShadowMapTech;
    }

    VolumetricLightingProgram getSliceUVDirInSMProgram(){
        if(m_RenderSliceUVDirInSMTech == null){
            m_RenderSliceUVDirInSMTech = new VolumetricLightingProgram("RenderSliceUVDirInShadowMapTexture.frag", getMacros());
        }

        return m_RenderSliceUVDirInSMTech;
    }

    VolumetricLightingProgram getRenderSliceEndpointsProgram(){
        if(m_RendedSliceEndpointsTech == null){
            m_RendedSliceEndpointsTech = new VolumetricLightingProgram("GenerateSliceEndpoints.frag", getMacros());
        }

        return m_RendedSliceEndpointsTech;
    }

    VolumetricLightingProgram getRenderCoordinateTextureProgram() {
        if(m_RendedCoordTexTech == null){
            m_RendedCoordTexTech = new VolumetricLightingProgram("GenerateCoordinateTexture.frag", getMacros());
        }

        return m_RendedCoordTexTech;
    }

    VolumetricLightingProgram getMarkRayMarchingSamplesInStencilProgram(){
        if(m_MarkRayMarchingSamplesInStencilTech == null){
            m_MarkRayMarchingSamplesInStencilTech = new VolumetricLightingProgram("MarkRayMarchingSamplesInStencil.frag", getMacros());
        }

        return m_MarkRayMarchingSamplesInStencilTech;
    }

    VolumetricLightingProgram getRefineSampleLocationsProgram(){
        if(m_RefineSampleLocationsTech == null){
            Macro[] macros = getMacros();
            int length = macros.length;
            macros = Arrays.copyOf(macros, length + 2);

            // Thread group size must be at least as large as initial sample step
            m_uiSampleRefinementCSThreadGroupSize = Math.max( m_uiSampleRefinementCSMinimumThreadGroupSize, m_ScatteringInitAttribs.m_uiInitialSampleStepInSlice );
            // Thread group size cannot be larger than the total number of samples in slice
            m_uiSampleRefinementCSThreadGroupSize = Math.min( m_uiSampleRefinementCSThreadGroupSize, m_ScatteringInitAttribs.m_uiMaxSamplesInSlice );
            macros[length+0] = new Macro("INITIAL_SAMPLE_STEP", m_ScatteringInitAttribs.m_uiInitialSampleStepInSlice);
            macros[length+1] = new Macro("THREAD_GROUP_SIZE", m_uiSampleRefinementCSThreadGroupSize);

            m_RefineSampleLocationsTech = new VolumetricLightingProgram((Void)null, "RefineSampleLocations.frag", macros);
        }

        return m_RefineSampleLocationsTech;
    }

    Texture2D getEpipolarImageDSV(){
        if(m_ptex2DEpipolarImageDSV == null){
            Texture2DDesc desc = new Texture2DDesc(m_ScatteringInitAttribs.m_uiMaxSamplesInSlice, m_ScatteringInitAttribs.m_uiNumEpipolarSlices, GLenum.GL_DEPTH24_STENCIL8);
            m_ptex2DEpipolarImageDSV = TextureUtils.createTexture2D(desc, null);
        }

        return m_ptex2DEpipolarImageDSV;
    }

    Texture2D getDownsampleDSV(){
        if(m_ptex2DDownsampleDSV == null){
            Texture2DDesc desc = new Texture2DDesc(m_ScatteringInitAttribs.m_uiBackBufferWidth/m_ScatteringInitAttribs.m_iDownscaleFactor,
                                                    m_ScatteringInitAttribs.m_uiBackBufferHeight/m_ScatteringInitAttribs.m_iDownscaleFactor,
                                                    GLenum.GL_DEPTH24_STENCIL8);
            m_ptex2DDownsampleDSV = TextureUtils.createTexture2D(desc, null);
        }

        return m_ptex2DDownsampleDSV;
    }

    Texture2D getDownsampleRTV(){
        if(m_ptex2DDownsampleRTV == null){
            Texture2DDesc desc = new Texture2DDesc(m_ScatteringInitAttribs.m_uiBackBufferWidth/m_ScatteringInitAttribs.m_iDownscaleFactor,
                    m_ScatteringInitAttribs.m_uiBackBufferHeight/m_ScatteringInitAttribs.m_iDownscaleFactor,
                    GLenum.GL_RGBA8);
            m_ptex2DDownsampleRTV = TextureUtils.createTexture2D(desc, null);
        }

        return m_ptex2DDownsampleRTV;
    }

    Texture2D getScreenSizedDSV(){
        if(m_CommonFrameAttribs.outputTexture != null &&
                m_CommonFrameAttribs.outputTexture.getWidth() == m_ScatteringInitAttribs.m_uiBackBufferWidth &&
                m_CommonFrameAttribs.outputTexture.getHeight() == m_ScatteringInitAttribs.m_uiBackBufferHeight) {
            return m_CommonFrameAttribs.outputTexture;
        }

        if(m_ptex2DScreenSizedDSV == null){
            Texture2DDesc desc = new Texture2DDesc(m_ScatteringInitAttribs.m_uiBackBufferWidth,
                                                    m_ScatteringInitAttribs.m_uiBackBufferHeight,
                                                    GLenum.GL_DEPTH24_STENCIL8);
            m_ptex2DScreenSizedDSV = TextureUtils.createTexture2D(desc, null);
        }

        return m_ptex2DScreenSizedDSV;
    }

    Texture2D getScreenSizedRTV(){
        if(m_ptex2DScreenSizedRTV == null){
            Texture2DDesc desc = new Texture2DDesc(m_ScatteringInitAttribs.m_uiBackBufferWidth,
                                                   m_ScatteringInitAttribs.m_uiBackBufferHeight,
                                                   GLenum.GL_RGBA8);
            m_ptex2DScreenSizedRTV = TextureUtils.createTexture2D(desc, null);
        }

        return m_ptex2DScreenSizedRTV;
    }

    Macro[] getMacros() { return getMacros(false);}
    Macro[] getMacros(boolean includeFrag) { return includeFrag ? m_MacrosWithFrag:m_Macros;}

    private void computeScatteringCoefficients(/*ID3D11DeviceContext *pDeviceCtx = NULL*/){
        m_MediaParams.f4TotalRayleighBeta.set(m_ScatteringFrameAttribs.m_f4RayleighBeta);
        // Scale scattering coefficients to match the scene scale
        final float fRayleighBetaMultiplier = m_ScatteringFrameAttribs.m_fDistanceScaler;
        m_MediaParams.f4TotalRayleighBeta.scale(fRayleighBetaMultiplier);

//        m_MediaParams.f4AngularRayleighBeta = m_MediaParams.f4TotalRayleighBeta * (3.0f/(16.0f*Numeric.PI));
        Vector4f.scale(m_MediaParams.f4TotalRayleighBeta, 3.0f/(16.0f* Numeric.PI), m_MediaParams.f4AngularRayleighBeta);

        m_MediaParams.f4TotalMieBeta.set(m_ScatteringFrameAttribs.m_f4MieBeta);
        final float fBetaMieMultiplier = 0.005f * m_ScatteringFrameAttribs.m_fDistanceScaler;
        m_MediaParams.f4TotalMieBeta.scale(fBetaMieMultiplier);
//        m_MediaParams.f4AngularMieBeta = m_MediaParams.f4TotalMieBeta / static_cast<float>(D3DX_PI*4);
        Vector4f.scale(m_MediaParams.f4TotalMieBeta, 1.0f/(Numeric.PI * 4), m_MediaParams.f4AngularMieBeta);

//        m_MediaParams.f4SummTotalBeta = m_MediaParams.f4TotalRayleighBeta + m_MediaParams.f4TotalMieBeta;
        Vector4f.add(m_MediaParams.f4TotalRayleighBeta, m_MediaParams.f4TotalMieBeta, m_MediaParams.f4SummTotalBeta);

        final float fGH_g = 0.98f;
        m_MediaParams.f4HG_g.x = 1 - fGH_g*fGH_g;
        m_MediaParams.f4HG_g.y = 1 + fGH_g*fGH_g;
        m_MediaParams.f4HG_g.z = -2*fGH_g;
        m_MediaParams.f4HG_g.w = 1.0f;
    }

    private void calculateLightAttribs(PostProcessingFrameAttribs commonAttribs, LightScatteringFrameAttribs frameAttribs){
        m_LightAttribs.f4DirOnLight.set(commonAttribs.lightDirection);  // TODO f4DirOnLight.w must be 0.
        m_LightAttribs.f4LightColorAndIntensity = frameAttribs.m_f4LightColorAndIntensity;
        if(m_LightAttribs.f4LightWorldPos == null){
            m_LightAttribs.f4LightWorldPos = new Vector4f(0,0,0,1);
        }
        m_LightAttribs.f4LightWorldPos.set(commonAttribs.lightPos);

        Matrix4f lightViewProjMat = commonAttribs.getLightViewProjMatrix();
        Matrix4f.transformCoord(lightViewProjMat, commonAttribs.getCameraPos(), m_LightAttribs.f4CameraUVAndDepthInShadowMap);
        m_LightAttribs.f4CameraUVAndDepthInShadowMap.x = 0.5f*m_LightAttribs.f4CameraUVAndDepthInShadowMap.x+0.5f;
        m_LightAttribs.f4CameraUVAndDepthInShadowMap.y = 0.5f*m_LightAttribs.f4CameraUVAndDepthInShadowMap.y+0.5f;
        m_LightAttribs.f4CameraUVAndDepthInShadowMap.z = 0.5f*m_LightAttribs.f4CameraUVAndDepthInShadowMap.z+0.5f;

        // Calculate location of the sun on the screen
        Matrix4f cameraViewProjMat = commonAttribs.getViewProjMatrix();
        Vector4f f4LightPosPS = m_LightAttribs.f4LightScreenPos;
        if(m_ScatteringInitAttribs.m_uiLightType == LightType.DIRECTIONAL){
            Vector4f vLightDir = m_LightAttribs.f4DirOnLight;
            Matrix4f.transform(cameraViewProjMat, vLightDir, f4LightPosPS);
            f4LightPosPS.x /= f4LightPosPS.w;
            f4LightPosPS.y /= f4LightPosPS.w;
            f4LightPosPS.z /= f4LightPosPS.w;
        }else{
            Matrix4f.transformCoord(cameraViewProjMat, m_LightAttribs.f4LightWorldPos, f4LightPosPS);
        }

        if(m_ScatteringInitAttribs.m_uiLightType == LightType.SPOT) {
            m_LightAttribs.f4SpotLightAxisAndCosAngle.set(m_LightAttribs.f4LightWorldPos);
            m_LightAttribs.f4SpotLightAxisAndCosAngle.w = 0;
            m_LightAttribs.f4SpotLightAxisAndCosAngle.normalise();

            double angle = Math.atan(commonAttribs.lightProjMat.m11);
            m_LightAttribs.f4SpotLightAxisAndCosAngle.w = (float) Math.cos(angle);
        }

        float fDistToLightOnScreen = Vector2f.length(f4LightPosPS );
        float fMaxDist = 100;
        if( fDistToLightOnScreen > fMaxDist ){
            // TODO I don't kown why
            float f = fMaxDist/fDistToLightOnScreen;
            f4LightPosPS.x *= f;
            f4LightPosPS.y *= f;
        }

        m_LightAttribs.bIsLightOnScreen = Math.abs(f4LightPosPS.x) <= 1.0f && Math.abs(f4LightPosPS.y) <= 1.0f;
        m_LightAttribs.m_f2ShadowMapTexelSize.set(1.0f/commonAttribs.shadowMapTexture.getWidth(), 1.0f/commonAttribs.shadowMapTexture.getHeight());
        m_LightAttribs.m_uiMinMaxShadowMapResolution = Math.max(commonAttribs.shadowMapTexture.getWidth(), commonAttribs.shadowMapTexture.getHeight());
        m_LightAttribs.m_uiShadowMapResolution = m_LightAttribs.m_uiMinMaxShadowMapResolution;

        m_LightAttribs.mWorldToLightProjSpaceT.load(lightViewProjMat);
    }

    void dispose(){
        if(m_ptex2DEpipolarImageDSV != null){
            m_ptex2DEpipolarImageDSV.dispose();
            m_ptex2DEpipolarImageDSV = null;
        }

        if(m_ReconstrCamSpaceZTech != null){
            m_ReconstrCamSpaceZTech.dispose();
            m_ReconstrCamSpaceZTech = null;
        }

        if(m_RendedSliceEndpointsTech != null){
            m_RendedSliceEndpointsTech.dispose();
            m_RendedSliceEndpointsTech = null;
        }

        if(m_RendedCoordTexTech != null){
            m_RendedCoordTexTech.dispose();
            m_RendedCoordTexTech = null;
        }

        if(m_RefineSampleLocationsTech != null){
            m_RefineSampleLocationsTech.dispose();
            m_RefineSampleLocationsTech = null;
        }

        if(m_MarkRayMarchingSamplesInStencilTech != null){
            m_MarkRayMarchingSamplesInStencilTech.dispose();
            m_MarkRayMarchingSamplesInStencilTech = null;
        }

        if(m_RenderSliceUVDirInSMTech != null){
            m_RenderSliceUVDirInSMTech.dispose();
            m_RenderSliceUVDirInSMTech = null;
        }

        if(m_InitializeMinMaxShadowMapTech != null){
            m_InitializeMinMaxShadowMapTech.dispose();
            m_InitializeMinMaxShadowMapTech = null;
        }

        if(m_ComputeMinMaxSMLevelTech != null){
            m_ComputeMinMaxSMLevelTech.dispose();
            m_ComputeMinMaxSMLevelTech = null;
        }

        for(int i = 0; i < m_DoRayMarchTech.length; i++){
            if(m_DoRayMarchTech[i] != null){
                m_DoRayMarchTech[i].dispose();
                m_DoRayMarchTech[i] = null;
            }
        }

        if(m_InterpolateIrradianceTech != null){
            m_InterpolateIrradianceTech.dispose();
            m_InterpolateIrradianceTech = null;
        }

        for(int i = 0; i < m_UnwarpEpipolarSctrImgTech.length; i++){
            if(m_UnwarpEpipolarSctrImgTech[i] != null){
                m_UnwarpEpipolarSctrImgTech[i].dispose();
                m_UnwarpEpipolarSctrImgTech[i] = null;
            }
        }

        for(int i = 0; i < m_FixInsctrAtDepthBreaksTech.length; i++){
            if(m_FixInsctrAtDepthBreaksTech[i] != null){
                m_FixInsctrAtDepthBreaksTech[i].dispose();
                m_FixInsctrAtDepthBreaksTech[i] = null;
            }
        }

        if(m_UpscaleInsctrdRadianceTech != null){
            m_UpscaleInsctrdRadianceTech.dispose();
            m_UpscaleInsctrdRadianceTech = null;
        }

        if(m_RenderSampleLocationsTech != null){
            m_RenderSampleLocationsTech.dispose();
            m_RenderSampleLocationsTech = null;
        }

        if(m_ptex2DDownsampleDSV != null){
            m_ptex2DDownsampleDSV.dispose();
            m_ptex2DDownsampleDSV = null;
        }

        if(m_ptex2DDownsampleRTV != null){
            m_ptex2DDownsampleRTV.dispose();
            m_ptex2DDownsampleRTV = null;
        }

        if(m_ptex2DScreenSizedRTV != null){
            m_ptex2DScreenSizedRTV.dispose();
            m_ptex2DScreenSizedRTV = null;
        }

        if(m_ptex2DScreenSizedDSV != null){
            m_ptex2DScreenSizedDSV.dispose();
            m_ptex2DScreenSizedDSV = null;
        }
    }
}
