package jet.opengl.demos.nvidia.cloth;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/8/5.
 */

final class Ellipsoid {
    final Vector4f[] Transform = new Vector4f[3];

    Ellipsoid(){
        Transform[0] = new Vector4f();
        Transform[1] = new Vector4f();
        Transform[2] = new Vector4f();
    }
}
