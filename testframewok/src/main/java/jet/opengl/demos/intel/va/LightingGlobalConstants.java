package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class LightingGlobalConstants implements Readable{
    public static final int SIZE = Vector4f.SIZE * 6;
    public final Vector4f DirectionalLightWorldDirection = new Vector4f();
    public final Vector4f DirectionalLightViewspaceDirection = new Vector4f();
    public final Vector4f DirectionalLightIntensity = new Vector4f();
    public final Vector4f AmbientLightIntensity = new Vector4f();

    public final Vector4f FogColor = new Vector4f();
    public float FogDistanceMin;
    public float FogDensity;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        DirectionalLightWorldDirection.store(buf);
        DirectionalLightViewspaceDirection.store(buf);
        DirectionalLightIntensity.store(buf);
        AmbientLightIntensity.store(buf);
        FogColor.store(buf);

        buf.putFloat(FogDistanceMin);
        buf.putFloat(FogDensity);
        buf.putLong(0);
        return buf;
    }

    void zeros(){
        DirectionalLightWorldDirection.set(0,0,0,0);
        DirectionalLightViewspaceDirection.set(0,0,0,0);
        DirectionalLightIntensity.set(0,0,0,0);
        AmbientLightIntensity.set(0,0,0,0);
        FogColor.set(0,0,0,0);

        FogDistanceMin = 0;
        FogDensity = 0;
    }
//    float                   FogDummy0;
//    float                   FogDummy1;


}
