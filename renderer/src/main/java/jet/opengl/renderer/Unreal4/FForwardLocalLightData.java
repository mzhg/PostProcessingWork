package jet.opengl.renderer.Unreal4;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

public class FForwardLocalLightData implements Readable {

    public static final int SIZE = Vector4f.SIZE * 5;

    public final Vector4f LightPositionAndInvRadius = new Vector4f();
    public final Vector4f LightColorAndFalloffExponent = new Vector4f();
    public final Vector4f LightDirectionAndShadowMapChannelMask = new Vector4f();
    public final Vector4f SpotAnglesAndSourceRadiusPacked = new Vector4f();
    public final Vector4f LightTangentAndSoftSourceRadius = new Vector4f();

    public int sizeInBytes(){
        return SIZE;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        LightPositionAndInvRadius.store(buf);
        LightColorAndFalloffExponent.store(buf);
        LightDirectionAndShadowMapChannelMask.store(buf);
        SpotAnglesAndSourceRadiusPacked.store(buf);
        LightTangentAndSoftSourceRadius.store(buf);
        
        return buf;
    }
}
