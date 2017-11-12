package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2017/11/12 0012.
 */

final class VS_INPUT_GRID_STRUCT implements Readable{
    static final int SIZE = Vector3f.SIZE * 2;

    VS_INPUT_GRID_STRUCT(){}

    VS_INPUT_GRID_STRUCT(float x,float y, float z, float s, float t, float r){
        pos.set(x,y,z);
        Tex.set(s,t,r);
    }

    final Vector3f pos = new Vector3f(); // Clip space position for slice vertices
    final Vector3f Tex = new Vector3f(); // Cell coordinates in 0-"texture dimension" range

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        pos.store(buf);
        Tex.store(buf);
        return buf;
    }
}
