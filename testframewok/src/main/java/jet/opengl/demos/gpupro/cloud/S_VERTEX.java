package jet.opengl.demos.gpupro.cloud;

import java.nio.FloatBuffer;

/**
 * Created by mazhen'gui on 2017/7/4.
 */

final class S_VERTEX {
    static final int SIZE = 10 * 4;

    final float[] fPos = new float[3];
    final float[] fNormal = new float[3];
    final float[] fTex = new float[4];

    void store(FloatBuffer buffer){
        buffer.put(fPos).put(fNormal).put(fTex);
    }
}
