package jet.opengl.demos.intel.cput;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/13.
 */

final class CPUTModelConstantBuffer implements Readable{
    static final int SIZE = Matrix4f.SIZE * 5 + Vector4f.SIZE * 6;

    final Matrix4f World = new Matrix4f();
    final Matrix4f WorldViewProjection = new Matrix4f();
    final Matrix4f InverseWorld = new Matrix4f();
    final Vector3f LightDirection = new Vector3f();
    final Vector3f EyePosition = new Vector3f();
    final Matrix4f LightWorldViewProjection = new Matrix4f();
    final Matrix4f ViewProjection = new Matrix4f();
    final Vector3f BoundingBoxCenterWorldSpace = new Vector3f();
    final Vector3f BoundingBoxHalfWorldSpace = new Vector3f();
    final Vector3f BoundingBoxCenterObjectSpace = new Vector3f();
    final Vector3f BoundingBoxHalfObjectSpace = new Vector3f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        World.store(buf);
        WorldViewProjection.store(buf);
        InverseWorld.store(buf);
        LightDirection.store(buf); buf.putInt(0);  // skip the padding
        EyePosition.store(buf); buf.putInt(0);  // skip the padding
        LightWorldViewProjection.store(buf);
        ViewProjection.store(buf);
        BoundingBoxCenterWorldSpace.store(buf); buf.putInt(0);  // skip the padding
        BoundingBoxHalfWorldSpace.store(buf); buf.putInt(0);  // skip the padding
        BoundingBoxCenterObjectSpace.store(buf); buf.putInt(0);  // skip the padding
        BoundingBoxHalfObjectSpace.store(buf); buf.putInt(0);  // skip the padding

        return buf;
    }
}
