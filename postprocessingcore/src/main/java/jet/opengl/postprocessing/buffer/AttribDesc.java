package jet.opengl.postprocessing.buffer;

import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by mazhen'gui on 2017/4/15.
 */

public class AttribDesc {
    public int index;
    public int size = 4;
    public int type = GLenum.GL_FLOAT;
    public boolean normalized = false;
    public int stride = 0;
    public int divisor = 0;
    public int offset = 0;
}
