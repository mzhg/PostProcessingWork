package jet.opengl.demos.Unreal4;

import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector4f;

public class FPrimitiveAndInstance {
    public final Vector4f BoundingSphere = new Vector4f();
    public FPrimitiveSceneInfo Primitive;
    public int InstanceIndex;

    public FPrimitiveAndInstance(ReadableVector4f InBoundingSphere, FPrimitiveSceneInfo InPrimitive, int InInstanceIndex){
        BoundingSphere.set(InBoundingSphere);
        Primitive = InPrimitive;
        InstanceIndex = InInstanceIndex;
    }
}
