package jet.opengl.demos.nvidia.rain;

import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/6/30.
 */

final class RainVertex {
    static final int SIZE = Vector3f.SIZE * 3 + 4 + 4;

    final Vector3f pos = new Vector3f();
    final Vector3f seed = new Vector3f();
    final Vector3f speed= new Vector3f();
    float random;
    int  Type;

    void store(ByteBuffer buffer){
        pos.store(buffer);
        seed.store(buffer);
        speed.store(buffer);

        buffer.putFloat(random);
        buffer.putInt(Type);
    }
}
