package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

final class CPUTLightParams {
    LightType   nLightType;
    final float[]       pColor = new float[3];
    float       fIntensity;
    float       fHotSpot;
    float       fConeAngle;
    float       fDecayStart;
    boolean        bEnableNearAttenuation;
    boolean        bEnableFarAttenuation;
    float       fNearAttenuationStart;
    float       fNearAttenuationEnd;
    float       fFarAttenuationStart;
    float       fFarAttenuationEnd;
}
