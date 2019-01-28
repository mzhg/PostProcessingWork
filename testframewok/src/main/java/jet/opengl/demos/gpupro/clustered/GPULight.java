package jet.opengl.demos.gpupro.clustered;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

//Currently only used in the generation of SSBO's for light culling and rendering
//I think it potentially would be a good idea to just have one overall light struct for all light types
//and move all light related calculations to the gpu via compute or frag shaders. This should reduce the
//number of Api calls we're currently making and also unify the current lighting path that is split between
//compute shaders and application based calculations for the matrices.
class GPULight implements Readable {
    static final int SIZE = Vector4f.SIZE * 3;

    final Vector4f position = new Vector4f();
    final Vector4f color = new Vector4f();
    int enabled;
    float intensity;
    float range;
    float padding;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        position.store(buf);
        color.store(buf);
        buf.putInt(enabled);
        buf.putFloat(intensity);
        buf.putFloat(range);
        buf.putFloat(padding);
        return buf;
    }
}
