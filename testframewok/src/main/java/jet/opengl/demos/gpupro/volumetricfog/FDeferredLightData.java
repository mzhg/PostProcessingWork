package jet.opengl.demos.gpupro.volumetricfog;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.DebugTools;

final class FDeferredLightData implements Readable {
    static final int SIZE = 9 * Vector4f.SIZE;

    final Vector3f Position = new Vector3f();
    float  InvRadius;

    final Vector3f Color = new Vector3f();
    float  FalloffExponent;

    final Vector3f Direction = new Vector3f();
    float VolumetricScatteringIntensity;

    final Vector3f Tangent = new Vector3f();
    float SoftSourceRadius;

    final Vector2f SpotAngles = new Vector2f();
    float SourceRadius;
    float SourceLength;

    float SpecularScale;
    float ContactShadowLength;
    final Vector2f DistanceFadeMAD = new Vector2f();

    final Vector4f ShadowMapChannelMask = new Vector4f();
    /** Whether ContactShadowLength is in World Space or in Screen Space. */
    boolean ContactShadowLengthInWS;
    /** Whether to use inverse squared falloff. */
    boolean bInverseSquared;
    /** Whether this is a light with radial attenuation, aka point or spot light. */
    boolean bRadialLight;
    /** Whether this light needs spotlight attenuation. */
    boolean bSpotLight;

    boolean bRectLight;
    /** Whether the light should apply shadowing. */
    int ShadowedBits;
    float RectLightBarnCosAngle;
    float RectLightBarnLength;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        Position.store(buf);
        buf.putFloat(InvRadius);
        Color.store(buf);
        buf.putFloat(FalloffExponent);
        Direction.store(buf);
        buf.putFloat(VolumetricScatteringIntensity);
        Tangent.store(buf);
        buf.putFloat(SoftSourceRadius);
        SpotAngles.store(buf);
        buf.putFloat(SourceRadius);
        buf.putFloat(SourceLength);
        buf.putFloat(SpecularScale);
        buf.putFloat(ContactShadowLength);
        DistanceFadeMAD.store(buf);
        ShadowMapChannelMask.store(buf);
        buf.putInt(ContactShadowLengthInWS?1:0);
        buf.putInt(bInverseSquared?1:0);
        buf.putInt(bRadialLight?1:0);
        buf.putInt(bSpotLight?1:0);
        buf.putInt(bRectLight?1:0);
        buf.putInt(ShadowedBits);
        buf.putFloat(RectLightBarnCosAngle);
        buf.putFloat(RectLightBarnLength);
        return buf;
    }

    public static void main(String[] args){
        DebugTools.genStoreBytebuffer(FDeferredLightData.class);
    }
}
