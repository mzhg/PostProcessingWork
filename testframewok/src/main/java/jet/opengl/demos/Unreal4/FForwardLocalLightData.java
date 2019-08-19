package jet.opengl.demos.Unreal4;

import org.lwjgl.util.vector.Vector4f;

public class FForwardLocalLightData {

    public static final int SIZE = Vector4f.SIZE * 5;

    public final Vector4f LightPositionAndInvRadius = new Vector4f();
    public final Vector4f LightColorAndFalloffExponent = new Vector4f();
    public final Vector4f LightDirectionAndShadowMapChannelMask = new Vector4f();
    public final Vector4f SpotAnglesAndSourceRadiusPacked = new Vector4f();
    public final Vector4f LightTangentAndSoftSourceRadius = new Vector4f();

    public int sizeInBytes(){
        return SIZE;
    }
}
