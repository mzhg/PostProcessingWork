package jet.opengl.demos.intel.fluid.render;

/**
 * Created by Administrator on 2018/3/13 0013.
 */

public class TextureStage {
    public final SamplerState mSamplerState  = new SamplerState();   ///< Sampler settings.
    public TextureBase mTexture;   ///< Address of texture that this sampler accesses.
}
