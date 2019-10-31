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

    public void reset(){
        stencilFailOp = GLenum.GL_KEEP;
        stencilDepthFailOp = GLenum.GL_KEEP;
        stencilPassOp = GLenum.GL_KEEP;
        stencilFunc = GLenum.GL_ALWAYS;
        stencilWriteMask = 0xFF;
        stencilMask = 0xFF;
        stencilRef = 0;
    }

    public void set(StencilState o){
        stencilFailOp = o.stencilFailOp;
        stencilDepthFailOp = o.stencilDepthFailOp;
        stencilPassOp = o.stencilPassOp;
        stencilFunc = o.stencilFunc;
        stencilWriteMask = o.stencilWriteMask;
        stencilMask = o.stencilMask;
        stencilRef = o.stencilRef;
    }

    @Override
    public String toString() {
        return "StencilState{" +
                "stencilFailOp=" + getStencilOpName(stencilFailOp) +
                ", stencilDepthFailOp=" + getStencilOpName(stencilDepthFailOp) +
                ", stencilPassOp=" + getStencilOpName(stencilPassOp) +
                ", stencilFunc=" + getStencilFuncName(stencilFunc) +
                ", stencilWriteMask=" + stencilWriteMask +
                ", stencilMask=" + stencilMask +
                ", stencilRef=" + stencilRef +
                '}';
    }

    public static String getStencilOpName(int op){
        switch(op){
            case GLenum.GL_KEEP: return "GL_KEEP";
            case GLenum.GL_ZERO: return "GL_ZERO";
            case GLenum.GL_REPLACE: return "GL_REPLACE";
            case GLenum.GL_INCR: return "GL_INCR";
            case GLenum.GL_INCR_WRAP: return "GL_INCR_WRAP";
            case GLenum.GL_DECR: return "GL_DECR";
            case GLenum.GL_DECR_WRAP: return "GL_DECR_WRAP";
            case GLenum.GL_INVERT: return "GL_INVERT";
            default: return "Unkonw(" + Integer.toHexString(op) + ")";
        }
    }

    public static String getStencilFuncName(int func){
        switch(func){
            case GLenum.GL_NEVER: return "GL_NEVER";
            case GLenum.GL_LESS: return "GL_LESS";
            case GLenum.GL_LEQUAL: return "GL_LEQUAL";
            case GLenum.GL_GREATER: return "GL_GREATER";
            case GLenum.GL_GEQUAL: return "GL_GEQUAL";
            case GLenum.GL_EQUAL: return "GL_EQUAL";
            case GLenum.GL_NOTEQUAL: return "GL_NOTEQUAL";
            case GLenum.GL_ALWAYS: return "GL_ALWAYS";
            default: return "Unkonw(" + Integer.toHexString(func) + ")";
        }
    }
}
