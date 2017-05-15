package jet.opengl.postprocessing.common;

import jet.opengl.postprocessing.texture.TextureUtils;

/**
 * Created by mazhen'gui on 2017/4/20.
 */

public class DepthStencilState {

    /** Defualt value, readonly, don't modify it. */
    public static final DepthStencilState g_DefaultDSState = new DepthStencilState();

    public boolean depthEnable = false;
    public boolean depthWriteMask = true;
    public int depthFunc = GLenum.GL_LESS;
    public boolean stencilEnable = false;
    public final StencilState frontFace = new StencilState();
    public final StencilState backFace = new StencilState();

    public void set(DepthStencilState o){
        depthEnable = o.depthEnable;
        depthWriteMask = o.depthWriteMask;
        depthFunc = o.depthFunc;
        stencilEnable = o.stencilEnable;
        frontFace.set(o.frontFace);
        backFace.set(o.backFace);
    }

    @Override
    public String toString() {
        return "DepthStencilState{" +
                "depthEnable=" + depthEnable +
                ", depthWriteMask=" + depthWriteMask +
                ", depthFunc=" + TextureUtils.getCompareModeName(depthFunc) +
                ", stencilEnable=" + stencilEnable +
                ", frontFace=" + frontFace +
                ", backFace=" + backFace +
                '}';
    }
}
