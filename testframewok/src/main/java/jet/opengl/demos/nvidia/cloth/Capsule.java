package jet.opengl.demos.nvidia.cloth;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/8/5.
 */

final class Capsule {
    Vector3f Origin;
    float Length;
    Vector4f Axis;
    Vector2f Radius;
//    float2 padding;

    public Capsule(Vector3f origin, float length, Vector4f axis, Vector2f radius) {
        Origin = origin;
        Length = length;
        Axis = axis;
        Radius = radius;
    }
}
