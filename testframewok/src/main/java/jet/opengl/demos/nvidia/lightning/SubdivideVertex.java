package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.buffer.AttribDesc;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class SubdivideVertex implements Readable, Writable{
    static final int SIZE = Vector3f.SIZE * 3 + 4;

    final Vector3f  Start= new Vector3f();
    final Vector3f	End = new Vector3f();
    final Vector3f	Up = new Vector3f();
    int Level;

    SubdivideVertex(Vector3f start, Vector3f end, Vector3f up){
        Start.set(start);
        End.set(end);
        Up.set(up);
    }

    static AttribDesc[] GetLayout(){
        return CommonUtil.toArray(
                new AttribDesc(0, 3, GLenum.GL_FLOAT, false, SIZE, 0),  // Start
                new AttribDesc(1, 3, GLenum.GL_FLOAT, false, SIZE, 12), // End
                new AttribDesc(2, 3, GLenum.GL_FLOAT, false, SIZE, 24), // Up
                new AttribDesc(3, 1, GLenum.GL_UNSIGNED_INT, false, SIZE, 36) // Level
        );
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        Start.store(buf);
        End.store(buf);
        Up.store(buf);
        buf.putInt(Level);
        return buf;
    }

    @Override
    public Writable load(ByteBuffer buf) {
        Start.load(buf);
        End.load(buf);
        Up.load(buf);
        Level = buf.getInt();
        return this;
    }
}
