package jet.opengl.demos.gpupro.clustered;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

final class PointLight extends BaseLight{
    final Vector3f position = new Vector3f(0.0f);
    final Matrix4f[] lookAtPerFace = new Matrix4f[6];
}
