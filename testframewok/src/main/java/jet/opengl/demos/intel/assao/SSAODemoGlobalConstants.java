package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;

/**
 * Created by mazhen'gui on 2018/1/3.
 */

final class SSAODemoGlobalConstants {
    static final int SIZE = Matrix4f.SIZE * 2;

    final Matrix4f World = new Matrix4f();
    final Matrix4f WorldView = new Matrix4f();
}
