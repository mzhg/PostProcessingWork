package jet.opengl.postprocessing.common;

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

    public int cullMode = GLenum.GL_BACK;
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
}
