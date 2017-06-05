package jet.opengl.postprocessing.core.outdoorLighting;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

public class OutdoorLightScatteringInitAttribs {
    public static final int TONE_MAPPING_MODE_EXP = 0;
    public static final int TONE_MAPPING_MODE_REINHARD = 1;
    public static final int TONE_MAPPING_MODE_REINHARD_MOD = 2;
    public static final int TONE_MAPPING_MODE_UNCHARTED2 = 3;
    public static final int TONE_MAPPING_FILMIC_ALU = 4;
    public static final int TONE_MAPPING_LOGARITHMIC = 5;
    public static final int TONE_MAPPING_ADAPTIVE_LOG = 6;

    public int m_uiInitialSampleStepInSlice = 16;
    public int m_uiNumEpipolarSlices = 512;
    public int m_uiMaxSamplesInSlice = 256;
    public boolean m_bOptimizeSampleLocations = true;
    public boolean m_bEnableLightShafts = true;
    public boolean m_bUseCombinedMinMaxTexture = true;
    public boolean m_bLightAdaptation = false;
    /**
     * <b>True</b>: Render extinction in epipolar space and perform bilateral filtering in the same manner as for inscattering<p>
     * <b>False</b>:Evaluate extinction for each pixel using analytic formula by Eric Bruneton
     */
    public boolean m_bExtinctionEvalMode = true;
    public boolean m_bRefinementCriterionInsctrDiff = true;
    public MultipleSctrMode m_uiMultipleScatteringMode = MultipleSctrMode.UNOCCLUDED;
    public SingleSctrMode m_uiSingleScatteringMode = SingleSctrMode.LUT;
    public int m_uiCascadeProcessingMode;  // TODO
    public int m_uiBackBufferWidth, m_uiBackBufferHeight;
    public int m_uiToneMappingMode = TONE_MAPPING_MODE_UNCHARTED2;

    public boolean m_bCorrectScatteringAtDepthBreaks = false;
    public boolean m_bShowSampling = false;
    public boolean m_bEnableEpipolarSampling = true;
    public boolean m_bAutoExposure = true;
    public boolean m_bIs32BitMinMaxMipMap = false;
    public boolean m_bUse1DMinMaxTree = true;

    public int m_iPrecomputedSctrUDim = 32;
    public int m_iPrecomputedSctrVDim = 128;
    public int m_iPrecomputedSctrWDim = 64;
    public int m_iPrecomputedSctrQDim = 16;
    public int m_iFirstCascade;

    /** output texture */
    //    public int m_iAmbientSkyLightTexDim = 1024;
    public Texture2D m_ptex2DAmbientSkyLight;

    public void set(OutdoorLightScatteringInitAttribs other){
        m_uiInitialSampleStepInSlice = other.m_uiInitialSampleStepInSlice;
        m_uiNumEpipolarSlices = other.m_uiNumEpipolarSlices;
        m_uiMaxSamplesInSlice = other.m_uiMaxSamplesInSlice;

        m_bOptimizeSampleLocations = other.m_bOptimizeSampleLocations;
        m_bEnableLightShafts = other.m_bEnableLightShafts;
        m_bUseCombinedMinMaxTexture = other.m_bUseCombinedMinMaxTexture;
        m_bLightAdaptation = other.m_bLightAdaptation;

        m_bExtinctionEvalMode = other.m_bExtinctionEvalMode;
        m_bRefinementCriterionInsctrDiff = other.m_bRefinementCriterionInsctrDiff;
        m_uiMultipleScatteringMode = other.m_uiMultipleScatteringMode;
        m_uiSingleScatteringMode = other.m_uiSingleScatteringMode;
        m_uiCascadeProcessingMode = other.m_uiCascadeProcessingMode;
        m_uiBackBufferWidth = other.m_uiBackBufferWidth;
        m_uiBackBufferHeight = other.m_uiBackBufferHeight;
        m_uiToneMappingMode = other.m_uiToneMappingMode;

        m_bCorrectScatteringAtDepthBreaks = other.m_bCorrectScatteringAtDepthBreaks;
        m_bShowSampling = other.m_bShowSampling;
        m_bEnableEpipolarSampling = other.m_bEnableEpipolarSampling;
        m_bAutoExposure = other.m_bAutoExposure;
        m_bIs32BitMinMaxMipMap = other.m_bIs32BitMinMaxMipMap;
        m_bUse1DMinMaxTree = other.m_bUse1DMinMaxTree;

        m_iPrecomputedSctrUDim = other.m_iPrecomputedSctrUDim;
        m_iPrecomputedSctrVDim = other.m_iPrecomputedSctrVDim;
        m_iPrecomputedSctrWDim = other.m_iPrecomputedSctrWDim;
        m_iPrecomputedSctrQDim = other.m_iPrecomputedSctrQDim;
        m_iFirstCascade = other.m_iFirstCascade;

        m_ptex2DAmbientSkyLight = other.m_ptex2DAmbientSkyLight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OutdoorLightScatteringInitAttribs that = (OutdoorLightScatteringInitAttribs) o;

        if (m_uiInitialSampleStepInSlice != that.m_uiInitialSampleStepInSlice) return false;
        if (m_uiNumEpipolarSlices != that.m_uiNumEpipolarSlices) return false;
        if (m_uiMaxSamplesInSlice != that.m_uiMaxSamplesInSlice) return false;
        if (m_bOptimizeSampleLocations != that.m_bOptimizeSampleLocations) return false;
        if (m_bEnableLightShafts != that.m_bEnableLightShafts) return false;
        if (m_bUseCombinedMinMaxTexture != that.m_bUseCombinedMinMaxTexture) return false;
        if (m_bLightAdaptation != that.m_bLightAdaptation) return false;
        if (m_bExtinctionEvalMode != that.m_bExtinctionEvalMode) return false;
        if (m_bRefinementCriterionInsctrDiff != that.m_bRefinementCriterionInsctrDiff) return false;
        if (m_uiCascadeProcessingMode != that.m_uiCascadeProcessingMode) return false;
        if (m_uiBackBufferWidth != that.m_uiBackBufferWidth) return false;
        if (m_uiBackBufferHeight != that.m_uiBackBufferHeight) return false;
        if (m_uiToneMappingMode != that.m_uiToneMappingMode) return false;
        if (m_bCorrectScatteringAtDepthBreaks != that.m_bCorrectScatteringAtDepthBreaks)
            return false;
        if (m_bShowSampling != that.m_bShowSampling) return false;
        if (m_bEnableEpipolarSampling != that.m_bEnableEpipolarSampling) return false;
        if (m_bAutoExposure != that.m_bAutoExposure) return false;
        if (m_bIs32BitMinMaxMipMap != that.m_bIs32BitMinMaxMipMap) return false;
        if (m_bUse1DMinMaxTree != that.m_bUse1DMinMaxTree) return false;
        if (m_iPrecomputedSctrUDim != that.m_iPrecomputedSctrUDim) return false;
        if (m_iPrecomputedSctrVDim != that.m_iPrecomputedSctrVDim) return false;
        if (m_iPrecomputedSctrWDim != that.m_iPrecomputedSctrWDim) return false;
        if (m_iPrecomputedSctrQDim != that.m_iPrecomputedSctrQDim) return false;
        if (m_iFirstCascade != that.m_iFirstCascade) return false;
        if (m_uiMultipleScatteringMode != that.m_uiMultipleScatteringMode) return false;
        if (m_uiSingleScatteringMode != that.m_uiSingleScatteringMode) return false;
        return CommonUtil.equals(m_ptex2DAmbientSkyLight, that.m_ptex2DAmbientSkyLight);

    }

    @Override
    public int hashCode() {
        int result = m_uiInitialSampleStepInSlice;
        result = 31 * result + m_uiNumEpipolarSlices;
        result = 31 * result + m_uiMaxSamplesInSlice;
        result = 31 * result + (m_bOptimizeSampleLocations ? 1 : 0);
        result = 31 * result + (m_bEnableLightShafts ? 1 : 0);
        result = 31 * result + (m_bUseCombinedMinMaxTexture ? 1 : 0);
        result = 31 * result + (m_bLightAdaptation ? 1 : 0);
        result = 31 * result + (m_bExtinctionEvalMode ? 1 : 0);
        result = 31 * result + (m_bRefinementCriterionInsctrDiff ? 1 : 0);
        result = 31 * result + m_uiMultipleScatteringMode.hashCode();
        result = 31 * result + m_uiSingleScatteringMode.hashCode();
        result = 31 * result + m_uiCascadeProcessingMode;
        result = 31 * result + m_uiBackBufferWidth;
        result = 31 * result + m_uiBackBufferHeight;
        result = 31 * result + m_uiToneMappingMode;
        result = 31 * result + (m_bCorrectScatteringAtDepthBreaks ? 1 : 0);
        result = 31 * result + (m_bShowSampling ? 1 : 0);
        result = 31 * result + (m_bEnableEpipolarSampling ? 1 : 0);
        result = 31 * result + (m_bAutoExposure ? 1 : 0);
        result = 31 * result + (m_bIs32BitMinMaxMipMap ? 1 : 0);
        result = 31 * result + (m_bUse1DMinMaxTree ? 1 : 0);
        result = 31 * result + m_iPrecomputedSctrUDim;
        result = 31 * result + m_iPrecomputedSctrVDim;
        result = 31 * result + m_iPrecomputedSctrWDim;
        result = 31 * result + m_iPrecomputedSctrQDim;
        result = 31 * result + m_iFirstCascade;
        result = 31 * result + (m_ptex2DAmbientSkyLight != null ? m_ptex2DAmbientSkyLight.hashCode():0);
        return result;
    }
}
