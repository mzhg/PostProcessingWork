package jet.opengl.demos.demos.amdfx.dof;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector2i;
import org.lwjgl.util.vector.Vector4i;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/6/24.
 */

final class DofParams {
    static final int SIZE = 8 * 4 + Vector4i.SIZE * (9+4);

    final Vector2i sourceResolution = new Vector2i();
    final Vector2i bufferResolution = new Vector2i();
    final Vector2f invSourceResolution = new Vector2f();
    float  scale_factor;
    float  padding;
    final Vector4i[]   bartlettData = new Vector4i[9];
    final Vector4i[]   boxBartlettData = new Vector4i[4];

    DofParams(){
        for(int i = 0; i < bartlettData.length; i++)
            bartlettData[i] = new Vector4i();

        for(int i = 0; i < boxBartlettData.length; i++)
            boxBartlettData[i] = new Vector4i();
    }

    void store(ByteBuffer buf){
        buf.putInt(sourceResolution.x);
        buf.putInt(sourceResolution.y);

        buf.putInt(bufferResolution.x);
        buf.putInt(bufferResolution.y);

        invSourceResolution.store(buf);
        buf.putFloat(scale_factor);
        buf.putFloat(padding);

        for(int i = 0; i < bartlettData.length; i++){
            Vector4i v = bartlettData[i];
            v.store(buf);
        }

        for(int i = 0; i < boxBartlettData.length; i++){
            Vector4i v = boxBartlettData[i];
            v.store(buf);
        }
    }
}
