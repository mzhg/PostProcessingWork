package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class DrawCallArguments implements Readable {
    static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE;
    final Matrix4f world = new Matrix4f();
    final Matrix4f worldView = new Matrix4f();
    int meshIndex;
    //        uint32 pad[3];

    void set(DrawCallArguments o){
        this.world.load(o.world);
        this.worldView.load(o.worldView);
        this.meshIndex = o.meshIndex;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        world.store(buf);
        worldView.store(buf);
        buf.putInt(meshIndex);
        buf.putInt(0);
        buf.putLong(0);
        return buf;
    }

}
