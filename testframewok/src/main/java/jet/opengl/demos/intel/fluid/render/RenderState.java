package jet.opengl.demos.intel.fluid.render;

import org.lwjgl.util.vector.Matrix4f;


/**
 * Created by mazhen'gui on 2018/3/12.
 */

public class RenderState {
    public final BlendState          mBlendState         = new BlendState();
    public final DepthState          mDepthState         = new DepthState();
    public final AlphaState          mAlphaState         = new AlphaState();
    public final RasterState         mRasterState        = new RasterState();

    public final MaterialProperties  mMaterialProperties = new MaterialProperties();
    public final Matrix4f mTransforms = new Matrix4f();

    public void set(RenderState ohs){
        mBlendState.set(ohs.mBlendState);
        mDepthState.set(ohs.mDepthState);
        mAlphaState.set(ohs.mAlphaState);
        mRasterState.set(ohs.mRasterState);
        mMaterialProperties.set(ohs.mMaterialProperties);
        mTransforms.load(ohs.mTransforms);
    }
}
