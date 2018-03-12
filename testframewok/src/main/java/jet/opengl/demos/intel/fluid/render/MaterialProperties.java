package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2018/3/12.
 */

public class MaterialProperties {
    public final Vector4f mDiffuseColor = new Vector4f(0.8f , 0.8f , 0.8f , 1.0f);
    public final Vector4f mAmbientColor = new Vector4f(0.2f , 0.2f , 0.2f , 1.0f);
    public final Vector4f mSpecularColor = new Vector4f(0.0f , 0.0f , 0.0f , 1.0f);
    public final Vector4f mEmissiveColor = new Vector4f(0.0f , 0.0f , 0.0f , 1.0f);
    public float          mSpecularPower;
}
