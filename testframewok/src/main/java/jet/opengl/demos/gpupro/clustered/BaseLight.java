package jet.opengl.demos.gpupro.clustered;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

class BaseLight {
    final Vector3f color = new Vector3f(1.0f, 1, 1);
    final Matrix4f shadowProjectionMat = new Matrix4f();

    boolean  changed = false;

    float strength = 1.0f;
    float zNear    = 1.0f;
    float zFar     = 2000.0f;

    int shadowRes = 1024;
    int depthMapTextureID = 0;
}
