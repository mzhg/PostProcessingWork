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

    public AttribDesc(){}

    public AttribDesc(int index, int size, int type, boolean normalized, int stride, int offset) {
        this.index = index;
        this.size = size;
        this.type = type;
        this.normalized = normalized;
        this.stride = stride;
        this.offset = offset;
    }
}
