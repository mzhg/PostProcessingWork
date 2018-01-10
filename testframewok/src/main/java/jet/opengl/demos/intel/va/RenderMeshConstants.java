package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public class RenderMeshConstants implements Readable{
    public static final int SIZE = Matrix4f.SIZE * 3 + Vector4f.SIZE;

    public final Matrix4f World = new Matrix4f();
    public final Matrix4f WorldView = new Matrix4f();
    public final Matrix4f ShadowWorldViewProj = new Matrix4f();
    public final Vector4f Color = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        World.store(buf);
        WorldView.store(buf);
        ShadowWorldViewProj.store(buf);
        Color.store(buf);
        return buf;
    }
}
