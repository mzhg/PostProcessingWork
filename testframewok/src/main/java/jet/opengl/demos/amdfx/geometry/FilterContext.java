package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

final class FilterContext {
    GeometryFX_FilterRenderOptions options;
    final Matrix4f view = new Matrix4f();
    final Matrix4f projection = new Matrix4f();
    final Vector3f eye = new Vector3f();
    int windowWidth;
    int windowHeight;
}
