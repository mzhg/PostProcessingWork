package jet.opengl.postprocessing.common;

import static jet.opengl.postprocessing.common.GLenum.GL_BACK;

/**
 * Created by mazhen'gui on 2017/4/20.
 */

public class RasterizerState {
    public static final int MASK_RED = 1;
    public static final int MASK_GREEN = 2;
    public static final int MASK_BLUE = 4;
    public static final int MASK_ALPHA = 8;

    public static final int MASK_ALL = MASK_RED | MASK_GREEN | MASK_BLUE | MASK_ALPHA;

    /** Defualt value, readonly, don't modify it. */
    public static final RasterizerState g_DefaultRSState = new RasterizerState();

    public int cullMode = GL_BACK;
    public boolean frontCounterClockwise = false;
    public boolean cullFaceEnable = false;
    public boolean rasterizedDiscardEnable = false;
    public int colorWriteMask = MASK_ALL;
    public int fillMode = GLenum.GL_FILL;

    public void set(RasterizerState o){
        cullMode = o.cullMode;
        frontCounterClockwise = o.frontCounterClockwise;
        cullFaceEnable = o.cullFaceEnable;
        rasterizedDiscardEnable = o.rasterizedDiscardEnable;
        colorWriteMask = o.colorWriteMask;
        fillMode = o.fillMode;
    }

    @Override
    public String toString() {
        return "RasterizerState{" +
                "cullMode=" + getCullModeName(cullMode) +
                ", frontCounterClockwise=" + frontCounterClockwise +
                ", cullFaceEnable=" + cullFaceEnable +
                ", rasterizedDiscardEnable=" + rasterizedDiscardEnable +
                ", colorWriteMask=" + colorWriteMask +
                ", fillMode=" + getFillModeName(fillMode) +
                '}';
    }

    public static String getCullModeName(int mode){
        switch (mode){
            case GLenum.GL_FRONT: return "GL_FRONT";
            case GLenum.GL_BACK:  return "GL_BACK";
            case GLenum.GL_FRONT_AND_BACK: return "GL_FRONT_AND_BACK";
            default: return "Invalid mode: (" + Integer.toHexString(mode) + ")";
        }
    }

    public static String getFillModeName(int mode){
        switch (mode){
            case GLenum.GL_FILL: return "GL_FILL";
            case GLenum.GL_LINE: return "GL_LINE";
            case GLenum.GL_POINT: return "GL_POINT";
            default: return "Invalid mode: (" + Integer.toHexString(mode) + ")";
        }
    }
}
