package intel.avsm;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Writable;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * We need two structures for the particles: one for simulation, and another for rendering.
 * The simulation structure contains state for each particle.<br>
 * The drawing strcuture contains data for each of the particle's vertices.<br>
 * We currently store six vertices for each particle - 2 triangles * 3 vertices per.
 * We could reduce this to four with a "mini" index buffer - each particle follows the
 * same vertex-reference pattern: 0,1,2, 1,2,3.  There could also be a lrb-specific benefit
 * to directly rasterizing the particles, maybe even rasterizing circles instead of quads.<p></p>
 * Created by mazhen'gui on 2017/9/29.
 */

final class SimpleVertex implements Readable, Writable{
    static final int SIZE = 4 * 7;

    final float[] mpPos = new float[3];
    final float[] mpUV = new float[2];
    float mSize;
    float mOpacity;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        CacheBuffer.put(buf, mpPos);
        CacheBuffer.put(buf, mpUV);
        buf.putFloat(mSize);
        buf.putFloat(mOpacity);
        return buf;
    }

    @Override
    public SimpleVertex load(ByteBuffer buf) {
        mpPos[0] = buf.getFloat();
        mpPos[1] = buf.getFloat();
        mpPos[2] = buf.getFloat();
        mpUV[0] = buf.getFloat();
        mpUV[1] = buf.getFloat();
        mSize = buf.getFloat();
        mOpacity = buf.getFloat();
        return this;
    }
}
