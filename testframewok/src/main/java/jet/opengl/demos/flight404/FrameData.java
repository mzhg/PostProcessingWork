package jet.opengl.demos.flight404;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2018/2/10.
 */

final class FrameData {
    float screenWidth, screenHeight;
    float mouseX, mouseY;

    final Matrix4f view = new Matrix4f();
    final Matrix4f proj = new Matrix4f();
    final Matrix4f viewProj = new Matrix4f();
    final Vector3f cameraPos = new Vector3f();
}
