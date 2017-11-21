package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public class SimpleSkyConstants implements Readable{
    public static final int SIZE = Matrix4f.SIZE + Vector4f.SIZE * (5+2);
    public final Matrix4f       ProjToWorld = new Matrix4f();

    public final Vector4f       SunDir = new Vector4f();

    public final Vector4f       SkyColorLow = new Vector4f();
    public final Vector4f       SkyColorHigh = new Vector4f();

    public final Vector4f       SunColorPrimary = new Vector4f();
    public final Vector4f       SunColorSecondary = new Vector4f();

    public float                SkyColorLowPow;
    public float                SkyColorLowMul;

    public float                SunColorPrimaryPow;
    public float                SunColorPrimaryMul;
    public float                SunColorSecondaryPow;
    public float                SunColorSecondaryMul;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        ProjToWorld.store(buf);
        SunDir.store(buf);
        SkyColorLow.store(buf);
        SkyColorHigh.store(buf);
        SunColorPrimary.store(buf);
        SunColorSecondary.store(buf);

        buf.putFloat(SkyColorLowPow);
        buf.putFloat(SkyColorLowMul);

        buf.putFloat(SunColorPrimaryPow);
        buf.putFloat(SunColorPrimaryMul);
        buf.putFloat(SunColorSecondaryPow);
        buf.putFloat(SunColorSecondaryMul);

        buf.position(buf.position() + 8);
        return buf;
    }

//    float               Dummy0;
//    float               Dummy1;
}
