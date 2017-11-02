package jet.opengl.demos.intel.avsm;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/1.
 */

final class ParticlePerPassConstants implements Readable{
    static final int SIZE = Matrix4f.SIZE * 2 + Vector4f.SIZE * 2;

    final Matrix4f mParticleWorldViewProj = new Matrix4f();
    final Matrix4f mParticleWorldView = new Matrix4f();
    final Vector3f mEyeRight = new Vector3f();
    final Vector3f mEyeUp = new Vector3f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        mParticleWorldViewProj.store(buf);
        mParticleWorldView.store(buf);
        mEyeRight.store(buf); buf.putInt(0);
        mEyeUp.store(buf);buf.putInt(0);
        return buf;
    }
}
