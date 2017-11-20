package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public class ShaderSimpleShadowsGlobalConstants implements Readable{
    public static final int SIZE = Matrix4f.SIZE * 6 + Vector4f.SIZE;

    public final Matrix4f View =new Matrix4f();
    public final Matrix4f Proj = new Matrix4f();
    public final Matrix4f ViewProj = new Matrix4f();

    public final Matrix4f CameraViewToShadowView = new Matrix4f();
    public final Matrix4f CameraViewToShadowViewProj = new Matrix4f();
    public final Matrix4f CameraViewToShadowUVNormalizedSpace = new Matrix4f();

    public float               ShadowMapRes;
    public float               OneOverShadowMapRes;
//    public float               Dummy2;
//    public float               Dummy3;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        View.store(buf);
        Proj.store(buf);
        ViewProj.store(buf);

        CameraViewToShadowView.store(buf);
        CameraViewToShadowViewProj.store(buf);
        CameraViewToShadowUVNormalizedSpace.store(buf);

        buf.putFloat(ShadowMapRes);
        buf.putFloat(OneOverShadowMapRes);
        buf.position(buf.position() + 8); // skip the padding.
        return buf;
    }
}
