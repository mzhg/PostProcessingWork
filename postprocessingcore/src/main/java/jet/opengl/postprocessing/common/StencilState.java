package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/20.
 */

public class StencilState {
    public int stencilFailOp = GLenum.GL_KEEP;
    public int stencilDepthFailOp = GLenum.GL_KEEP;
    public int stencilPassOp = GLenum.GL_KEEP;
    public int stencilFunc = GLenum.GL_ALWAYS;
    public int stencilWriteMask = 0xFF;
    public int stencilMask = 0xFF;
    public int stencilRef = 0;

    public void set(StencilState o){
        stencilFailOp = o.stencilFailOp;
        stencilDepthFailOp = o.stencilDepthFailOp;
        stencilPassOp = o.stencilPassOp;
        stencilFunc = o.stencilFunc;
        stencilWriteMask = o.stencilWriteMask;
        stencilMask = o.stencilMask;
        stencilRef = o.stencilRef;
    }
}
