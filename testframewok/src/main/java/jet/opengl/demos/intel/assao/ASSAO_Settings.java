package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

final class ASSAO_Settings implements Readable, Writable{
    static final int SIZE = 56;

	float       Radius;                             // [0.0,  ~ ] World (view) space size of the occlusion sphere.
    float       ShadowMultiplier;                   // [0.0, 5.0] Effect strength linear multiplier
    float       ShadowPower;                        // [0.5, 5.0] Effect strength pow modifier
    float       ShadowClamp;                        // [0.0, 1.0] Effect max limit (applied after multiplier but before blur)
    float       HorizonAngleThreshold;              // [0.0, 0.2] Limits self-shadowing (makes the sampling area less of a hemisphere, more of a spherical cone, to avoid self-shadowing and various artifacts due to low tessellation and depth buffer imprecision, etc.)
    float       FadeOutFrom;                        // [0.0,  ~ ] Distance to start start fading out the effect.
    float       FadeOutTo;                          // [0.0,  ~ ] Distance at which the effect is faded out.
    int         QualityLevel;                       // [ -1,  3 ] Effect quality; -1 - lowest (low, half res checkerboard), 0 - low, 1 - medium, 2 - high, 3 - very high / adaptive; each quality level is roughly 2x more costly than the previous, except the q3 which is variable but, in general, above q2.
    float       AdaptiveQualityLimit;               // [0.0, 1.0] (only for Quality Level 3)
    int         BlurPassCount;                      // [  0,   6] Number of edge-sensitive smart blur passes to apply. Quality 0 is an exception with only one 'dumb' blur pass used.
    float       Sharpness;                          // [0.0, 1.0] (How much to bleed over edges; 1: not at all, 0.5: half-half; 0.0: completely ignore edges)
    float       TemporalSupersamplingAngleOffset;   // [0.0,  PI] Used to rotate sampling kernel; If using temporal AA / supersampling, suggested to rotate by ( (frame%3)/3.0*PI ) or similar. Kernel is already symmetrical, which is why we use PI and not 2*PI.
    float       TemporalSupersamplingRadiusOffset;  // [0.0, 2.0] Used to scale sampling kernel; If using temporal AA / supersampling, suggested to scale by ( 1.0f + (((frame%3)-1.0)/3.0)*0.1 ) or similar.
    float       DetailShadowStrength;               // [0.0, 5.0] Used for high-res detail AO using neighboring depth pixels: adds a lot of detail but also reduces temporal stability (adds aliasing).

    ASSAO_Settings( )
    {
        Radius                              = 1.2f;
        ShadowMultiplier                    = 1.0f;
        ShadowPower                         = 1.50f;
        ShadowClamp                         = 0.98f;
        HorizonAngleThreshold               = 0.06f;
        FadeOutFrom                         = 50.0f;
        FadeOutTo                           = 300.0f;
        AdaptiveQualityLimit                = 0.45f;
        QualityLevel                        = 2;
        BlurPassCount                       = 2;
        Sharpness                           = 0.98f;
        TemporalSupersamplingAngleOffset    = 0.0f;
        TemporalSupersamplingRadiusOffset   = 1.0f;
        DetailShadowStrength                = 0.5f;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putFloat(Radius);
        buf.putFloat(ShadowMultiplier);
        buf.putFloat(ShadowPower);
        buf.putFloat(ShadowClamp);
        buf.putFloat(HorizonAngleThreshold);
        buf.putFloat(FadeOutFrom);
        buf.putFloat(FadeOutTo);
        buf.putInt(QualityLevel);
        buf.putFloat(AdaptiveQualityLimit);
        buf.putInt(BlurPassCount);
        buf.putFloat(Sharpness);
        buf.putFloat(TemporalSupersamplingAngleOffset);
        buf.putFloat(TemporalSupersamplingRadiusOffset);
        buf.putFloat(DetailShadowStrength);
        return buf;
    }

    @Override
    public Writable load(ByteBuffer buf) {
        Radius = buf.getFloat();
        ShadowMultiplier = buf.getFloat();
        ShadowPower = buf.getFloat();
        ShadowClamp = buf.getFloat();
        HorizonAngleThreshold = buf.getFloat();
        FadeOutFrom = buf.getFloat();
        FadeOutTo = buf.getFloat();
        QualityLevel = buf.getInt();
        AdaptiveQualityLimit = buf.getFloat();
        BlurPassCount = buf.getInt();
        Sharpness = buf.getFloat();
        TemporalSupersamplingAngleOffset = buf.getFloat();
        TemporalSupersamplingRadiusOffset = buf.getFloat();
        DetailShadowStrength = buf.getFloat();
        return this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ASSAO_Settings{");
        sb.append("\nRadius=").append(Radius);
        sb.append(", \nShadowMultiplier=").append(ShadowMultiplier);
        sb.append(", \nShadowPower=").append(ShadowPower);
        sb.append(", \nShadowClamp=").append(ShadowClamp);
        sb.append(", \nHorizonAngleThreshold=").append(HorizonAngleThreshold);
        sb.append(", \nFadeOutFrom=").append(FadeOutFrom);
        sb.append(", \nFadeOutTo=").append(FadeOutTo);
        sb.append(", \nQualityLevel=").append(QualityLevel);
        sb.append(", \nAdaptiveQualityLimit=").append(AdaptiveQualityLimit);
        sb.append(", \nBlurPassCount=").append(BlurPassCount);
        sb.append(", \nSharpness=").append(Sharpness);
        sb.append(", \nTemporalSupersamplingAngleOffset=").append(TemporalSupersamplingAngleOffset);
        sb.append(", \nTemporalSupersamplingRadiusOffset=").append(TemporalSupersamplingRadiusOffset);
        sb.append(", \nDetailShadowStrength=").append(DetailShadowStrength);
        sb.append('\n').append('}');
        return sb.toString();
    }
}
