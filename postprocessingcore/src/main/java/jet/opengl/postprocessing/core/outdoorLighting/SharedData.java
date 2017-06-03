package jet.opengl.postprocessing.core.outdoorLighting;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import java.util.Arrays;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

final class SharedData {
    OutdoorLightScatteringFrameAttribs m_ScatteringFrameAttribs = null;
    OutdoorLightScatteringInitAttribs  m_ScatteringInitAttribs = null;

    final SAirScatteringAttribs m_MediaParams = new SAirScatteringAttribs();
    PostProcessingFrameAttribs  m_CommonFrameAttribs = null;
    private final SLightAttribs m_LightAttribs = new SLightAttribs();

    boolean m_bRecomputeSctrCoeffs;
    private Macro[] m_Macros = null;
    private Macro[] m_MacrosWithFrag = null;

    int m_psamLinearClamp = 0;
    int m_psamLinearBorder0 = 0;
    int m_psamLinearUClampVWrap = 0;
    int m_psamComparison = 0;

    int m_uiSampleRefinementCSThreadGroupSize = 0;
    // Using small group size is inefficient because a lot of SIMD lanes become
    // idle
    int m_uiSampleRefinementCSMinimumThreadGroupSize = 128;

    final BlendState m_additiveBlendBS;
    // stencil state
    final DepthStencilState m_pNoDepth_StEqual_KeepStencilDS;
    final DepthStencilState m_pDisableDepthTestIncrStencilDS;
    final DepthStencilState m_pNoDepth_StEqual_IncrStencilDS;

    private RenderTechnique m_ReconstrCamSpaceZTech;
    private RenderTechnique m_RendedSliceEndpointsTech;
    private RenderTechnique m_RendedCoordTexTech;
    private RenderTechnique m_RefineSampleLocationsTech;
    private RenderTechnique m_RenderCoarseUnshadowedInsctrTech;
    private RenderTechnique m_MarkRayMarchingSamplesInStencilTech;
    private RenderTechnique m_RenderSliceUVDirInSMTech;
    private RenderTechnique m_InitializeMinMaxShadowMapTech;
    private RenderTechnique m_ComputeMinMaxSMLevelTech;
    private final RenderTechnique m_DoRayMarchTech[/* 2 */];
    private RenderTechnique m_InterpolateIrradianceTech;
    private RenderTechnique m_UnwarpEpipolarSctrImgTech;
    private RenderTechnique m_UnwarpAndRenderLuminanceTech;
    private RenderTechnique m_UpdateAverageLuminanceTech;
    private final RenderTechnique m_FixInsctrAtDepthBreaksTech[/* 4 */];
    private RenderTechnique m_RenderSampleLocationsTech;

    private Texture2D m_ptex2DEpipolarImageDSV;

    SharedData(){

        m_additiveBlendBS = new BlendState();
        m_additiveBlendBS.blendEnable = true;
        m_additiveBlendBS.srcBlend = GLenum.GL_ONE;
        m_additiveBlendBS.srcBlendAlpha = GLenum.GL_ONE;
        m_additiveBlendBS.destBlend = GLenum.GL_ONE;
        m_additiveBlendBS.destBlendAlpha = GLenum.GL_ONE;

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

        m_pNoDepth_StEqual_KeepStencilDS = new DepthStencilState();
        m_pNoDepth_StEqual_KeepStencilDS.depthEnable = false;
        m_pNoDepth_StEqual_KeepStencilDS.stencilEnable = true;
        m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilDepthFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilPassOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilFunc = GLenum.GL_EQUAL;
        m_pNoDepth_StEqual_KeepStencilDS.backFace.stencilRef = 1;

        m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilDepthFailOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilPassOp = GLenum.GL_KEEP;
        m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilFunc = GLenum.GL_EQUAL;
        m_pNoDepth_StEqual_KeepStencilDS.frontFace.stencilRef = 1;

        m_DoRayMarchTech = new RenderTechnique[2];
        m_FixInsctrAtDepthBreaksTech = new RenderTechnique[4];
    }

    void prepare(PostProcessingFrameAttribs commonAttribs, OutdoorLightScatteringInitAttribs initAttribs, OutdoorLightScatteringFrameAttribs frameAttribs){
        boolean bReconstructMacros = false;
        if(m_ScatteringInitAttribs == null){
            m_ScatteringInitAttribs = new OutdoorLightScatteringInitAttribs();
            bReconstructMacros = true;
        }

        if(bReconstructMacros){
            m_ScatteringInitAttribs.set(initAttribs);

            m_Macros = new Macro[10];
            m_Macros[0] = new Macro("NUM_EPIPOLAR_SLICES", initAttribs.m_uiNumEpipolarSlices);
            m_Macros[1] = new Macro("MAX_SAMPLES_IN_SLICE", initAttribs.m_uiMaxSamplesInSlice);
            m_Macros[2] = new Macro("OPTIMIZE_SAMPLE_LOCATIONS", initAttribs.m_bOptimizeSampleLocations?1:0);
            m_Macros[3] = new Macro("USE_COMBINED_MIN_MAX_TEXTURE", initAttribs.m_bUseCombinedMinMaxTexture?1:0);
            m_Macros[4] = new Macro("EXTINCTION_EVAL_MODE", initAttribs.m_bExtinctionEvalMode?1:0 );
            m_Macros[5] = new Macro("ENABLE_LIGHT_SHAFTS", initAttribs.m_bEnableLightShafts?1:0);
            m_Macros[6] = new Macro("MULTIPLE_SCATTERING_MODE", initAttribs.m_uiMultipleScatteringMode.ordinal());
            m_Macros[7] = new Macro("SINGLE_SCATTERING_MODE", initAttribs.m_uiSingleScatteringMode.ordinal());
            m_Macros[8] = new Macro("SCREEN_RESLOUTION", "float2("+initAttribs.m_uiBackBufferWidth+","+initAttribs.m_uiBackBufferHeight+")");
            m_Macros[9] = new Macro("PRECOMPUTED_SCTR_LUT_DIM", "float4("+initAttribs.m_iPrecomputedSctrUDim+","+initAttribs.m_iPrecomputedSctrVDim+
                    ","+initAttribs.m_iPrecomputedSctrWDim+","+initAttribs.m_iPrecomputedSctrQDim+")");

            m_MacrosWithFrag = Arrays.copyOf(m_Macros, m_Macros.length + 1);
            m_MacrosWithFrag[m_Macros.length] = new Macro("__FRAG_SHADER__", 1);
        }

        m_bRecomputeSctrCoeffs = false;
        if(m_ScatteringFrameAttribs == null){
            m_ScatteringFrameAttribs = new OutdoorLightScatteringFrameAttribs();
            m_bRecomputeSctrCoeffs = true;
        }

        final boolean bRecalculateScatteringTable = (!m_ScatteringFrameAttribs.f2ParticleScaleHeight.equals(frameAttribs.f2ParticleScaleHeight)) ||
                                                      m_ScatteringFrameAttribs.fEarthRadius != frameAttribs.fEarthRadius ||
                                                      m_ScatteringFrameAttribs.fAtmTopHeight != frameAttribs.fAtmTopHeight;

        if(!m_bRecomputeSctrCoeffs) {
            m_bRecomputeSctrCoeffs = m_ScatteringFrameAttribs.m_fAerosolDensityScale != frameAttribs.m_fAerosolDensityScale ||
                    m_ScatteringFrameAttribs.m_bUseCustomSctrCoeffs !=frameAttribs.m_bUseCustomSctrCoeffs ||
                    (!m_ScatteringFrameAttribs.m_bUseCustomSctrCoeffs && (m_ScatteringFrameAttribs.m_fAerosolAbsorbtionScale != frameAttribs.m_fAerosolAbsorbtionScale ||
                            m_ScatteringFrameAttribs.fTurbidity != frameAttribs.fTurbidity))||
                    (m_ScatteringFrameAttribs.m_bUseCustomSctrCoeffs && (!m_ScatteringFrameAttribs.m_f4CustomMieBeta.equals(frameAttribs.m_f4CustomMieBeta) ||
                            !m_ScatteringFrameAttribs.m_f4CustomRlghBeta.equals(frameAttribs.m_f4CustomRlghBeta)));
        }

        m_ScatteringFrameAttribs.set(frameAttribs);
        if(m_bRecomputeSctrCoeffs){
            computeScatteringCoefficients(frameAttribs);
        }

        m_bRecomputeSctrCoeffs = (m_bRecomputeSctrCoeffs|bRecalculateScatteringTable);

        createSamplers();
        calculateLightAttribs(commonAttribs, frameAttribs);
    }

    private void calculateLightAttribs(PostProcessingFrameAttribs commonAttribs, OutdoorLightScatteringFrameAttribs frameAttribs){

        Vector4f vDirOnLight = m_LightAttribs.f4DirOnLight;
        vDirOnLight.w = 0;
        vDirOnLight.set(commonAttribs.lightDirection);

        // Calculate location of the sun on the screen
        Matrix4f cameraViewProjMat = commonAttribs.getViewProjMatrix();
        Vector4f f4LightPosPS = m_LightAttribs.f4LightScreenPos;
        Vector4f vLightDir = m_LightAttribs.f4DirOnLight;
        Matrix4f.transform(cameraViewProjMat, vLightDir, f4LightPosPS);
        f4LightPosPS.x /= f4LightPosPS.w;
        f4LightPosPS.y /= f4LightPosPS.w;
        f4LightPosPS.z /= f4LightPosPS.w;

        float fDistToLightOnScreen = Vector2f.length(f4LightPosPS );
        float fMaxDist = 100;
        if( fDistToLightOnScreen > fMaxDist ){
            float f = fMaxDist/fDistToLightOnScreen;
            f4LightPosPS.x *= f;
            f4LightPosPS.y *= f;
        }

        m_LightAttribs.bIsLightOnScreen = Math.abs(f4LightPosPS.x) <= 1.0f && Math.abs(f4LightPosPS.y) <= 1.0f;
    }

    Macro[] getMacros() { return getMacros(false);}
    Macro[] getMacros(boolean includeFrag) { return includeFrag ? m_MacrosWithFrag:m_Macros;}

    private void computeScatteringCoefficients(OutdoorLightScatteringFrameAttribs m_PostProcessingAttribs){
        final double D3DX_PI = Math.PI;
        float aerosolDensityScale = 1.0f;
        float aerosolAbsorbtionScale = 0.1f;
        if(m_PostProcessingAttribs != null){
            aerosolDensityScale = m_PostProcessingAttribs.m_fAerosolDensityScale;
            aerosolAbsorbtionScale = m_PostProcessingAttribs.m_fAerosolAbsorbtionScale;
        }
        // For details, see "A practical Analytic Model for Daylight" by Preetham & Hoffman, p.23

        // Wave lengths
        // [BN08] follows [REK04] and gives the following values for Rayleigh scattering coefficients:
        // RayleighBetha(lambda = (680nm, 550nm, 440nm) ) = (5.8, 13.5, 33.1)e-6
        final double dWaveLengths[] =
                {
                        680e-9,     // red
                        550e-9,     // green
                        440e-9      // blue
                };

        // Calculate angular and total scattering coefficients for Rayleigh scattering:
        {
            final Vector4f f4AngularRayleighSctrCoeff = m_MediaParams.f4AngularRayleighSctrCoeff;
            final Vector4f f4TotalRayleighSctrCoeff = m_MediaParams.f4TotalRayleighSctrCoeff;
            final Vector4f f4RayleighExtinctionCoeff = m_MediaParams.f4RayleighExtinctionCoeff;

            double n = 1.0003;    // - Refractive index of air in the visible spectrum
            double N = 2.545e+25; // - Number of molecules per unit volume
            double Pn = 0.035;    // - Depolarization factor for air which exoresses corrections
            //   due to anisotropy of air molecules

            double dRayleighConst = 8.0*D3DX_PI*D3DX_PI*D3DX_PI * (n*n - 1.0) * (n*n - 1.0) / (3.0 * N) * (6.0 + 3.0*Pn) / (6.0 - 7.0*Pn);
            for(int WaveNum = 0; WaveNum < 3; WaveNum++)
            {
                double dSctrCoeff;
                if( m_PostProcessingAttribs != null && m_PostProcessingAttribs.m_bUseCustomSctrCoeffs ){
                    dSctrCoeff = /*f4TotalRayleighSctrCoeff[WaveNum] =*/ m_PostProcessingAttribs.m_f4CustomRlghBeta.get(WaveNum);
                    f4TotalRayleighSctrCoeff.setValue(WaveNum, (float)dSctrCoeff);
                }
                else
                {
                    double Lambda2 = dWaveLengths[WaveNum] * dWaveLengths[WaveNum];
                    double Lambda4 = Lambda2 * Lambda2;
                    dSctrCoeff = dRayleighConst / Lambda4;
                    // Total Rayleigh scattering coefficient is the integral of angular scattering coefficient in all directions
                    //f4TotalRayleighSctrCoeff[WaveNum] = static_cast<float>( dSctrCoeff );
                    f4TotalRayleighSctrCoeff.setValue(WaveNum, (float)dSctrCoeff);
                }
                // Angular scattering coefficient is essentially volumetric scattering coefficient multiplied by the
                // normalized phase function
                // p(Theta) = 3/(16*Pi) * (1 + cos^2(Theta))
                // f4AngularRayleighSctrCoeff contains all the terms exepting 1 + cos^2(Theta):
                //f4AngularRayleighSctrCoeff[WaveNum] = static_cast<float>( 3.0 / (16.0*D3DX_PI) * dSctrCoeff );
                f4AngularRayleighSctrCoeff.setValue(WaveNum, (float)( 3.0 / (16.0*D3DX_PI) * dSctrCoeff ));
                // f4AngularRayleighSctrCoeff[WaveNum] = f4TotalRayleighSctrCoeff[WaveNum] * p(Theta)
            }
            // Air molecules do not absorb light, so extinction coefficient is only caused by out-scattering
            f4RayleighExtinctionCoeff.set(f4TotalRayleighSctrCoeff);
        }

        // Calculate angular and total scattering coefficients for Mie scattering:
        {
            final Vector4f f4AngularMieSctrCoeff = m_MediaParams.f4AngularMieSctrCoeff;
            final Vector4f f4TotalMieSctrCoeff = m_MediaParams.f4TotalMieSctrCoeff;
            final Vector4f f4MieExtinctionCoeff = m_MediaParams.f4MieExtinctionCoeff;

            if( m_PostProcessingAttribs != null && m_PostProcessingAttribs.m_bUseCustomSctrCoeffs )
            {
                //f4TotalMieSctrCoeff = m_PostProcessingAttribs.m_f4CustomMieBeta * m_PostProcessingAttribs.m_fAerosolDensityScale;
                Vector4f.scale(m_PostProcessingAttribs.m_f4CustomMieBeta, m_PostProcessingAttribs.m_fAerosolDensityScale, f4TotalMieSctrCoeff);
            }
            else
            {
                boolean bUsePreethamMethod = false;
                if( bUsePreethamMethod )
                {
                    // Values for K came from the table 2 in the "A practical Analytic Model
                    // for Daylight" by Preetham & Hoffman, p.28
                    double K[] =
                            {
                                    0.68455,                //  K[650nm]
                                    0.678781,               //  K[570nm]
                                    (0.668532+0.669765)/2.0 // (K[470nm]+K[480nm])/2
                            };

                    //assert( m_MediaParams.fTurbidity >= 1.f );
                    if(m_PostProcessingAttribs.fTurbidity < 1.0f){
                        throw new IllegalArgumentException();
                    }

                    // Beta is an Angstrom's turbidity coefficient and is approximated by:
                    //float beta = 0.04608365822050f * m_fTurbidity - 0.04586025928522f; ???????

                    double c = (0.6544*m_PostProcessingAttribs.fTurbidity - 0.6510)*1E-16; // concentration factor
                    final double v = 4; // Junge's exponent

                    double dTotalMieBetaTerm = 0.434 * c * D3DX_PI * Math.pow(2.0*D3DX_PI, v-2);

                    for(int WaveNum = 0; WaveNum < 3; WaveNum++)
                    {
                        double Lambdav_minus_2 = Math.pow( dWaveLengths[WaveNum], v-2);
                        double dTotalMieSctrCoeff = dTotalMieBetaTerm * K[WaveNum] / Lambdav_minus_2;
                        //f4TotalMieSctrCoeff[WaveNum]   = static_cast<float>( dTotalMieSctrCoeff );
                        f4TotalMieSctrCoeff.setValue(WaveNum, (float)dTotalMieSctrCoeff);
                    }

                    //AtmScatteringAttribs.f4AngularMieSctrCoeff *= 0.02f;
                    //AtmScatteringAttribs.f4TotalMieSctrCoeff *= 0.02f;
                }
                else
                {
                    // [BN08] uses the following value (independent of wavelength) for Mie scattering coefficient: 2e-5
                    // For g=0.76 and MieBetha=2e-5 [BN08] was able to reproduce the same luminance as given by the
                    // reference CIE sky light model
                    final float fMieBethaBN08 = 2e-5f * aerosolDensityScale;
                    m_MediaParams.f4TotalMieSctrCoeff.set(fMieBethaBN08, fMieBethaBN08, fMieBethaBN08, 0);
                }
            }

            for(int WaveNum = 0; WaveNum < 3; WaveNum++)
            {
                // Normalized to unity Cornette-Shanks phase function has the following form:
                // F(theta) = 1/(4*PI) * 3*(1-g^2) / (2*(2+g^2)) * (1+cos^2(theta)) / (1 + g^2 - 2g*cos(theta))^(3/2)
                // The angular scattering coefficient is the volumetric scattering coefficient multiplied by the phase
                // function. 1/(4*PI) is baked into the f4AngularMieSctrCoeff, the other terms are baked into f4CS_g
                //f4AngularMieSctrCoeff[WaveNum] = f4TotalMieSctrCoeff[WaveNum]  / static_cast<float>(4.0 * D3DX_PI);
                f4AngularMieSctrCoeff.setValue(WaveNum, f4TotalMieSctrCoeff.get(WaveNum)/(float)(4.0 * D3DX_PI));
                // [BN08] also uses slight absorption factor which is 10% of scattering
                //f4MieExtinctionCoeff[WaveNum] = f4TotalMieSctrCoeff[WaveNum] * (1.f + m_PostProcessingAttribs.m_fAerosolAbsorbtionScale);
                f4MieExtinctionCoeff.setValue(WaveNum, f4TotalMieSctrCoeff.get(WaveNum) * (1.f + aerosolAbsorbtionScale));
            }
        }

        {
            // For g=0.76 and MieBetha=2e-5 [BN08] was able to reproduce the same luminance as is given by the
            // reference CIE sky light model
            // Cornette phase function (see Nishita et al. 93):
            // F(theta) = 1/(4*PI) * 3*(1-g^2) / (2*(2+g^2)) * (1+cos^2(theta)) / (1 + g^2 - 2g*cos(theta))^(3/2)
            // 1/(4*PI) is baked into the f4AngularMieSctrCoeff
            final Vector4f f4CS_g = m_MediaParams.f4CS_g;
            float f_g = m_PostProcessingAttribs.m_fAerosolPhaseFuncG;
            f4CS_g.x = 3*(1.f - f_g*f_g) / ( 2*(2.f + f_g*f_g) );
            f4CS_g.y = 1.f + f_g*f_g;
            f4CS_g.z = -2.f*f_g;
            f4CS_g.w = 1.f;
        }

        //m_MediaParams.f4TotalExtinctionCoeff = m_MediaParams.f4RayleighExtinctionCoeff + m_MediaParams.f4MieExtinctionCoeff;
        Vector4f.add(m_MediaParams.f4RayleighExtinctionCoeff, m_MediaParams.f4MieExtinctionCoeff, m_MediaParams.f4TotalExtinctionCoeff);
    }

    void setUniforms(RenderTechnique shader, SMiscDynamicParams attribs){
        shader.setupUniforms(m_MediaParams);
        shader.setupUniforms(m_LightAttribs);
        shader.setupUniforms(m_CommonFrameAttribs);
        shader.setupUniforms(m_ScatteringFrameAttribs);
        shader.setupUniforms(m_ScatteringInitAttribs);
        if(attribs != null)
            shader.setupUniforms(attribs);
    }

    void setUniforms(RenderTechnique shader){
        setUniforms(shader, null);
    }

    RenderTechnique getRenderSliceEndpointsProgram(){
        if(m_RendedSliceEndpointsTech == null){
            m_RendedSliceEndpointsTech = new RenderTechnique("GenerateSliceEndpoints.frag", getMacros());
        }

        return m_RendedSliceEndpointsTech;
    }

    RenderTechnique getRenderCoordinateTextureProgram(){
        if(m_RendedCoordTexTech == null){
            m_RendedCoordTexTech = new RenderTechnique("GenerateCoordinateTexture.frag", getMacros());
        }

        return m_RendedCoordTexTech;
    }

    RenderTechnique getRenderSliceUVDirInSMProgram(){
        if(m_RenderSliceUVDirInSMTech == null){
            m_RenderSliceUVDirInSMTech = new RenderTechnique("RenderSliceUVDirInShadowMapTexture.frag", getMacros());
        }

        return m_RenderSliceUVDirInSMTech;
    }

    RenderTechnique getMarkRayMarchingSamplesInStencilProgram(){
        if(m_MarkRayMarchingSamplesInStencilTech == null){
            m_MarkRayMarchingSamplesInStencilTech = new RenderTechnique("MarkRayMarchingSamplesInStencil.frag", getMacros());
        }

        return m_MarkRayMarchingSamplesInStencilTech;
    }

    RenderTechnique getRefineSampleLocationsProgram(){
        if(m_RefineSampleLocationsTech == null){
            Macro[] macros = getMacros();
            int length = macros.length;
            macros = Arrays.copyOf(macros, length + 4);

            // Thread group size must be at least as large as initial sample step
            m_uiSampleRefinementCSThreadGroupSize = Math.max( m_uiSampleRefinementCSMinimumThreadGroupSize, m_ScatteringInitAttribs.m_uiInitialSampleStepInSlice );
            // Thread group size cannot be larger than the total number of samples in slice
            m_uiSampleRefinementCSThreadGroupSize = Math.min( m_uiSampleRefinementCSThreadGroupSize, m_ScatteringInitAttribs.m_uiMaxSamplesInSlice );

            macros[length++] = new Macro("INITIAL_SAMPLE_STEP", m_ScatteringInitAttribs.m_uiInitialSampleStepInSlice);
            macros[length++] = new Macro("THREAD_GROUP_SIZE"  , m_uiSampleRefinementCSThreadGroupSize );
            macros[length++] = new Macro("REFINEMENT_CRITERION", m_ScatteringInitAttribs.m_bRefinementCriterionInsctrDiff ? 1:0 );
            macros[length++] = new Macro("AUTO_EXPOSURE",        m_ScatteringInitAttribs.m_bAutoExposure?1:0);

            m_RefineSampleLocationsTech = new RenderTechnique(null, "RefineSampleLocations.frag", macros);
        }

        return m_RefineSampleLocationsTech;
    }

    RenderTechnique getRenderCoarseUnshadowedInsctrProgram(){
        if(m_RenderCoarseUnshadowedInsctrTech == null){
            m_RenderCoarseUnshadowedInsctrTech = new RenderTechnique("RenderCoarseUnshadowedInsctr.frag", getMacros());
        }

        return m_RenderCoarseUnshadowedInsctrTech;
    }

    Texture2D getEpipolarImageDSV(){
        if(m_ptex2DEpipolarImageDSV == null){
            Texture2DDesc desc = new Texture2DDesc(m_ScatteringInitAttribs.m_uiMaxSamplesInSlice, m_ScatteringInitAttribs.m_uiNumEpipolarSlices, GLenum.GL_DEPTH24_STENCIL8);
            m_ptex2DEpipolarImageDSV = TextureUtils.createTexture2D(desc, null);
        }

        return m_ptex2DEpipolarImageDSV;
    }

    void dispose(){

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
}
