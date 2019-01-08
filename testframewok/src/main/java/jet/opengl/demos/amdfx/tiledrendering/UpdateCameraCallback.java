package jet.opengl.demos.amdfx.tiledrendering;

import org.lwjgl.util.vector.Matrix4f;

interface UpdateCameraCallback {
    void onUpdateCamera(Matrix4f viewProj);
}
