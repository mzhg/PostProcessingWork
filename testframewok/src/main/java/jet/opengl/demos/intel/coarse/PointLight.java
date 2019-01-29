package jet.opengl.demos.intel.coarse;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

// NOTE: Must match shader equivalent structure
public final class PointLight implements Readable {
    public static final int SIZE = Vector4f.SIZE * 2;

    public final Vector3f positionView = new Vector3f();
    public float attenuationBegin;
    public final Vector3f color = new Vector3f();
    public float attenuationEnd;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        positionView.store(buf);
        buf.putFloat(attenuationBegin);

        color.store(buf);
        buf.putFloat(attenuationEnd);
        return buf;
    }
}
