package jet.opengl.demos.nvidia.cloth;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/8/5.
 */

final class MeshVertex {
    final Vector3f Position = new Vector3f();
    final Vector3f Normal = new Vector3f();
    final Vector3f TangentX = new Vector3f();
    final Vector2f TexCoord = new Vector2f();
}
