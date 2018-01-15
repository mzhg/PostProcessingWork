package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2018/1/15.
 */

public class CPUTFrameConstantBuffer {
    public static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE * 3;

    public final Matrix4f View = new Matrix4f();
    public final Matrix4f Projection = new Matrix4f();
    public final Vector4f AmbientColor = new Vector4f();
    public final Vector4f LightColor = new Vector4f();
    public final Vector4f TotalSeconds = new Vector4f();
}
