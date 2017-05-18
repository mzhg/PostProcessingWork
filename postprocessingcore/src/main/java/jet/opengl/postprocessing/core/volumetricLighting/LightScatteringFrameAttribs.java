package jet.opengl.postprocessing.core.volumetricLighting;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/5/18.
 */

public class LightScatteringFrameAttribs {
    // Note that sampling near the epipole is very cheap since only a few steps
    // required to perform ray marching
    public int m_uiEpipoleSamplingDensityFactor = 4;

    public float m_fRefinementThreshold = 1.2f;

    public boolean m_bShowDepthBreaks = false;
    public boolean m_bShowLightingOnly = false;

    public float m_fDistanceScaler = 1.f;
    public float m_fMaxTracingDistance = 20.0f;
    public int m_uiMaxShadowMapStep = 16;

    public float m_fExposure = 1.f;

    public final Vector4f m_f4RayleighBeta = new Vector4f(5.8e-6f, 13.5e-6f, 33.1e-6f, 0.f);
    public final Vector4f m_f4MieBeta = new Vector4f(2.0e-5f, 2.0e-5f, 2.0e-5f, 0.f);
    public final Vector4f m_f4LightColorAndIntensity = new Vector4f(); // TODO

    public LightScatteringFrameAttribs() {}

    public LightScatteringFrameAttribs(LightScatteringFrameAttribs o) {
        set(o);
    }

    public void set(LightScatteringFrameAttribs o){
//        m_uiInitialSampleStepInSlice = o.m_uiInitialSampleStepInSlice;
        m_uiEpipoleSamplingDensityFactor = o.m_uiEpipoleSamplingDensityFactor;
        m_fRefinementThreshold = o.m_fRefinementThreshold;
//        m_iDownscaleFactor = o.m_iDownscaleFactor;
//        m_bShowSampling = o.m_bShowSampling;
//        m_bCorrectScatteringAtDepthBreaks = o.m_bCorrectScatteringAtDepthBreaks;
        m_bShowDepthBreaks = o.m_bShowDepthBreaks;
        m_bShowLightingOnly = o.m_bShowLightingOnly;
        m_fDistanceScaler = o.m_fDistanceScaler;
        m_fMaxTracingDistance = o.m_fMaxTracingDistance;
        m_uiMaxShadowMapStep = o.m_uiMaxShadowMapStep;
//        m_f2ShadowMapTexelSize.set(o.m_f2ShadowMapTexelSize);
//        m_uiShadowMapResolution = o.m_uiShadowMapResolution;
//        m_uiMinMaxShadowMapResolution = o.m_uiMinMaxShadowMapResolution;
        m_fExposure = o.m_fExposure;
//        m_bEnableEpipolarSampling = o.m_bEnableEpipolarSampling;
//		m_f3Dummy.set(o.m_f3Dummy);
        m_f4RayleighBeta.set(o.m_f4RayleighBeta);
        m_f4MieBeta.set(o.m_f4MieBeta);
        m_f4LightColorAndIntensity.set(o.m_f4LightColorAndIntensity);
    }
}
