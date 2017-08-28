package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class LightningPathSegment {
    static final int SIZE = Vector3f.SIZE * 3;

    final Vector3f Start = new Vector3f();
    final Vector3f End = new Vector3f();
    final Vector3f Up = new Vector3f();

    LightningPathSegment(Vector3f	start, Vector3f end, Vector3f up)
    {
        Start.set(start);
        End.set(end);
        Up.set(up);
    }
    LightningPathSegment(float sx, float sy, float sz, float ex, float ey, float ez, float ux /*= 0*/, float uy /*= 0*/, float uz /*= 1*/)
    {
        Start.set(sx,sy,sz);
        End.set(ex, ey, ez);
        Up.set(ux, uy, uz);
    }

    void operator(SubdivideVertex result)
    {
        result.Start.set(Start);
        result.End.set(End);
        result.Up.set(Up);

        result.Level = 0;
    }
}
