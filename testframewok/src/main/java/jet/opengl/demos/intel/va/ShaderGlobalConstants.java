package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2018/1/5.
 */

final class ShaderGlobalConstants {
    static final int SIZE = LightingGlobalConstants.SIZE + Matrix4f.SIZE * 3 + Vector2f.SIZE * 8 + Vector4f.SIZE;

    final LightingGlobalConstants Lighting = new LightingGlobalConstants();

    Matrix4f View;
    Matrix4f Proj;
    final Matrix4f ViewProj = new Matrix4f();

    final Vector2f ViewportSize = new Vector2f();           // ViewportSize.x, ViewportSize.y
    final Vector2f ViewportPixelSize = new Vector2f();      // 1.0 / ViewportSize.x, 1.0 / ViewportSize.y

    final Vector2f ViewportHalfSize = new Vector2f();         // ViewportSize.x * 0.5, ViewportSize.y * 0.5
    final Vector2f ViewportPixel2xSize =new Vector2f();    // 2.0 / ViewportSize.x, 2.0 / ViewportSize.y

    final Vector2f DepthUnpackConsts = new Vector2f();
    final Vector2f CameraTanHalfFOV = new Vector2f();
    final Vector2f CameraNearFar = new Vector2f();
    final Vector2f Dummy = new Vector2f();

    float                   TransparencyPass;
    float                   WireframePass;
    float                   Dummy0;
    float                   Dummy1;
}
