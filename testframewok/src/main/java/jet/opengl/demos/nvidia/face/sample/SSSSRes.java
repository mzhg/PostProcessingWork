package jet.opengl.demos.nvidia.face.sample;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

final class SSSSRes implements Readable {
    static final int SIZE = Matrix4f.SIZE * 4 + Vector4f.SIZE * 4;
    final Matrix4f worldViewProjection = new Matrix4f();
    final Matrix4f world = new Matrix4f();
    final Matrix4f worldInverseTranspose = new Matrix4f();
    final Matrix4f lightViewProjectionNDC = new Matrix4f();

    final Vector4f lightPos = new Vector4f();
    final Vector4f lightDir = new Vector4f();
    float falloffAngle;
    float spotExponent;
    float lightAttenuation;
    float lightRange;
    final Vector4f cameraPosition = new Vector4f();

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        worldViewProjection.store(buf);
        world.store(buf);
        worldInverseTranspose.store(buf);
        lightViewProjectionNDC.store(buf);

        lightPos.store(buf);
        lightDir.store(buf);
        buf.putFloat(falloffAngle);
        buf.putFloat(spotExponent);
        buf.putFloat(lightAttenuation);
        buf.putFloat(lightRange);

        cameraPosition.store(buf);
        return buf;
    }
}
