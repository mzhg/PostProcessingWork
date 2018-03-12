package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.RasterizerState;

/**
 * Created by mazhen'gui on 2018/3/12.
 */

public class RenderStateS {
    public final BlendState mBlendState = new BlendState();
    public final DepthStencilState mDepthStencilState = new DepthStencilState();
    public final RasterizerState mRasterizerState = new RasterizerState();
    public final MaterialProperties  mMaterialProperties = new MaterialProperties();
    public final Matrix4f mTransforms = new Matrix4f();
}
