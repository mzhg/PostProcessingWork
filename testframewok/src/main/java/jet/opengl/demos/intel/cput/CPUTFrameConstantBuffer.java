package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2018/1/15.
 */

public class CPUTFrameConstantBuffer implements Readable{
    public static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE * 3;

    public final Matrix4f View = new Matrix4f();
    public final Matrix4f Projection = new Matrix4f();
    public final Vector4f AmbientColor = new Vector4f();
    public final Vector4f LightColor = new Vector4f();
    public final Vector4f TotalSeconds = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        View.store(buf);
        Projection.store(buf);
        AmbientColor.store(buf);
        LightColor.store(buf);
        TotalSeconds.store(buf);
        return buf;
    }
}
