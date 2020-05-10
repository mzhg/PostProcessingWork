package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenDrawArraysStrip {
    public static final int  ID = GLenum. GL_DRAW_ARRAYS_STRIP_COMMAND_NV;

    public final DrawArraysCommandNV   cmd = new DrawArraysCommandNV();

    NVTokenDrawArraysStrip() {
        cmd.first = 0;
        cmd.count = 0;

        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }

    void setParams(int count, int first)
    {
        cmd.count = count;
        cmd.first = first;
    }
}
