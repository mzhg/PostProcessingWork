package jet.opengl.demos.nvidia.waves.ocean;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.shader.GLSLProgram;

public class Technique extends GLSLProgram {
    protected final RasterizerState mRaster = new RasterizerState();
    protected final DepthStencilState mDepthStencil = new DepthStencilState();
    protected final BlendState  mBlend = new BlendState();

    @Override
    public void enable() {
        GLStateTracker stateTracker = GLStateTracker.getInstance();
        stateTracker.setRasterizerState(mRaster);
        stateTracker.setDepthStencilState(mDepthStencil);
        stateTracker.setBlendState(mBlend);

        super.enable();
    }

    /** Apply the params to the program and setup all of render states. Override this method should call super implements at the last line. */
    public void apply(TechniqueParams params){
        this.enable();
    }
}
