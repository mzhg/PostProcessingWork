package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

final class DrawCommand implements Readable {
    static final int SIZE = DrawCallArguments.SIZE;

    final DrawCallArguments dcb = new DrawCallArguments();
    StaticMesh mesh;
    int drawCallId = -1;
    int firstTriangle;

    void set(DrawCommand o){
        this.dcb.set(o.dcb);
        this.mesh = o.mesh;
        this.drawCallId = o.drawCallId;
        this.firstTriangle = o.firstTriangle;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        return null;
    }
}
