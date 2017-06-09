package jet.opengl.postprocessing.core.outdoorLighting;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/6/2.
 */

public class OutdoorLightScatteringFrameAttribs {
    public boolean m_bUseCustomSctrCoeffs = false;
    public float m_fAerosolDensityScale = 1.f;
    public float m_fAerosolAbsorbtionScale = 0.1f;

    public final Vector4f m_f4CustomRlghBeta = new Vector4f(5.8e-6f, 13.5e-6f, 33.1e-6f, 0.f);
    public final Vector4f m_f4CustomMieBeta = new Vector4f(2.0e-5f, 2.0e-5f, 2.0e-5f, 0.f);
    public int m_uiEpipoleSamplingDensityFactor = 2;
    public float m_fRefinementThreshold = 0.03f;
    /** The variable can be calculating in runtime. */
    @Deprecated
    public float m_fMaxShadowMapStep = 256.0f;
    public final Vector4f f4ExtraterrestrialSunColor = new Vector4f();

    /**
     * Air molecules and aerosols are assumed to be distributed
     * between 6360 km and 6420 km
     */
    public float fEarthRadius = 6360000.f;
    public float fAtmTopHeight = 80000.f;
    public final Vector2f f2ParticleScaleHeight = new Vector2f(5994.f, 1200.f);

    public float fTurbidity = 1.02f;
//    public float fAtmTopRadius = fEarthRadius + fAtmTopHeight;
    public float m_fAerosolPhaseFuncG = 0.76f;

    public void set(OutdoorLightScatteringFrameAttribs other) {
        m_bUseCustomSctrCoeffs = other.m_bUseCustomSctrCoeffs;
        m_fAerosolDensityScale = other.m_fAerosolDensityScale;
        m_fAerosolAbsorbtionScale = other.m_fAerosolAbsorbtionScale;

        m_f4CustomRlghBeta.set(other.m_f4CustomRlghBeta);
        m_f4CustomMieBeta.set(other.m_f4CustomMieBeta);

        m_uiEpipoleSamplingDensityFactor = other.m_uiEpipoleSamplingDensityFactor;
        m_fRefinementThreshold = other.m_fRefinementThreshold;
        m_fMaxShadowMapStep = other.m_fMaxShadowMapStep;

        f4ExtraterrestrialSunColor.set(other.f4ExtraterrestrialSunColor);

        fEarthRadius = other.fEarthRadius;
        fAtmTopHeight = other.fAtmTopHeight;
        fTurbidity = other.fTurbidity;
        m_fAerosolPhaseFuncG = other.m_fAerosolPhaseFuncG;

        f2ParticleScaleHeight.set(other.f2ParticleScaleHeight);
    }
}
