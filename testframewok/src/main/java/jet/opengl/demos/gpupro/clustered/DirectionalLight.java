package jet.opengl.demos.gpupro.clustered;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

class DirectionalLight extends BaseLight{
    final Vector3f direction = new Vector3f(-1.0f);

    final Matrix4f lightView = new Matrix4f(0.0f);
    final Matrix4f lightSpaceMatrix = new Matrix4f(0.0f);

    float distance;
    float orthoBoxSize;
}
