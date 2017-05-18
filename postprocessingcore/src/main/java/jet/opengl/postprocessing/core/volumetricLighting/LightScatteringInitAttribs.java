package jet.opengl.postprocessing.core.volumetricLighting;

/**
 * Created by mazhen'gui on 2017/5/18.
 */

public class LightScatteringInitAttribs {

    public int m_uiInitialSampleStepInSlice = 16;
    public int m_uiNumEpipolarSlices = 512;
    public int m_uiMaxSamplesInSlice = 256;
    public boolean m_bOptimizeSampleLocations = true;
    public LightType m_uiLightType;
    public boolean m_bStainedGlass;
    public AccelStruct m_uiAccelStruct;
    public InscaterringIntegralEvalution m_uiInsctrIntglEvalMethod;
    public boolean m_bAnisotropicPhaseFunction;
    public int m_uiBackBufferWidth, m_uiBackBufferHeight;

    public int m_iDownscaleFactor = 1;
    public boolean m_bCorrectScatteringAtDepthBreaks = false;
    public boolean m_bShowSampling = false;
    public boolean m_bEnableEpipolarSampling = true;

    public void set(LightScatteringInitAttribs other){
        m_uiNumEpipolarSlices = other.m_uiNumEpipolarSlices;
        m_uiMaxSamplesInSlice = other.m_uiMaxSamplesInSlice;
        m_bOptimizeSampleLocations = other.m_bOptimizeSampleLocations;
        m_uiLightType = other.m_uiLightType;
        m_bStainedGlass = other.m_bStainedGlass;
        m_uiAccelStruct = other.m_uiAccelStruct;
        m_uiInsctrIntglEvalMethod = other.m_uiInsctrIntglEvalMethod;
        m_bAnisotropicPhaseFunction = other.m_bAnisotropicPhaseFunction;

        m_uiBackBufferWidth = other.m_uiBackBufferWidth;
        m_uiBackBufferHeight = other.m_uiBackBufferHeight;

        m_iDownscaleFactor = other.m_iDownscaleFactor;
        m_bCorrectScatteringAtDepthBreaks = other.m_bCorrectScatteringAtDepthBreaks;
        m_bShowSampling = other.m_bShowSampling;
        m_bEnableEpipolarSampling = other.m_bEnableEpipolarSampling;
        m_uiInitialSampleStepInSlice = other.m_uiInitialSampleStepInSlice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LightScatteringInitAttribs that = (LightScatteringInitAttribs) o;

        if (m_uiNumEpipolarSlices != that.m_uiNumEpipolarSlices) return false;
        if (m_uiMaxSamplesInSlice != that.m_uiMaxSamplesInSlice) return false;
        if (m_bOptimizeSampleLocations != that.m_bOptimizeSampleLocations) return false;
        if (m_bStainedGlass != that.m_bStainedGlass) return false;
        if (m_bAnisotropicPhaseFunction != that.m_bAnisotropicPhaseFunction) return false;
        if (m_uiLightType != that.m_uiLightType) return false;
        if (m_uiAccelStruct != that.m_uiAccelStruct) return false;
        if (m_uiBackBufferWidth != that.m_uiBackBufferWidth) return false;
        if (m_uiBackBufferHeight != that.m_uiBackBufferHeight) return false;

        if (m_iDownscaleFactor != that.m_iDownscaleFactor) return false;
        if (m_bCorrectScatteringAtDepthBreaks != that.m_bCorrectScatteringAtDepthBreaks) return false;
        if (m_bShowSampling != that.m_bShowSampling) return false;
        if (m_bEnableEpipolarSampling != that.m_bEnableEpipolarSampling) return false;
        if (m_uiInitialSampleStepInSlice != that.m_uiInitialSampleStepInSlice) return false;
        return m_uiInsctrIntglEvalMethod == that.m_uiInsctrIntglEvalMethod;
    }

    @Override
    public int hashCode() {
        int result = m_uiNumEpipolarSlices;
        result = 31 * result + m_uiMaxSamplesInSlice;
        result = 31 * result + m_uiBackBufferWidth;
        result = 31 * result + m_uiBackBufferHeight;
        result = 31 * result + m_iDownscaleFactor;
        result = 31 * result + m_uiInitialSampleStepInSlice;
        result = 31 * result + (m_bCorrectScatteringAtDepthBreaks ? 1 : 0);
        result = 31 * result + (m_bShowSampling ? 1 : 0);
        result = 31 * result + (m_bOptimizeSampleLocations ? 1 : 0);
        result = 31 * result + (m_bEnableEpipolarSampling ? 1 : 0);
        result = 31 * result + m_uiLightType.hashCode();
        result = 31 * result + (m_bStainedGlass ? 1 : 0);
        result = 31 * result + m_uiAccelStruct.hashCode();
        result = 31 * result + m_uiInsctrIntglEvalMethod.hashCode();
        result = 31 * result + (m_bAnisotropicPhaseFunction ? 1 : 0);
        return result;
    }
}
