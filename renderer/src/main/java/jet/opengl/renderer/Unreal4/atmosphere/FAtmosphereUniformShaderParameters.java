package jet.opengl.renderer.Unreal4.atmosphere;

import org.lwjgl.util.vector.Vector4f;

/** Use as a global shader parameter struct and also the CPU structure representing the atmosphere it self.
 This is static for a version of a component. When a component is changed/tweaked, it is recreated. */
public class FAtmosphereUniformShaderParameters {
    public float MultiScatteringFactor;
    public float BottomRadius;
    public float TopRadius;
    public float RayleighDensityExpScale;
    public int RayleighScattering;
    public int MieScattering;
    public float MieDensityExpScale;
    public int MieExtinction;
    public float MiePhaseG;
    public int MieAbsorption;
    public float AbsorptionDensity0LayerWidth;
    public float AbsorptionDensity0ConstantTerm;
    public float AbsorptionDensity0LinearTerm;
    public float AbsorptionDensity1ConstantTerm;
    public float AbsorptionDensity1LinearTerm;
    public int AbsorptionExtinction;
    public final Vector4f GroundAlbedo = new Vector4f();
}
