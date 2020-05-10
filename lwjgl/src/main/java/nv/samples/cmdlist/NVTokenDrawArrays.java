package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenDrawArrays {
    public static final int   ID = GLenum.GL_DRAW_ARRAYS_COMMAND_NV;

    public final DrawArraysCommandNV   cmd = new DrawArraysCommandNV();

    NVTokenDrawArrays() {
        cmd.first = 0;
        cmd.count = 0;

        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }

    void setParams(int count, int first/*=0*/)
    {
        cmd.count = count;
        cmd.first = first;
    }

    void setMode(int primmode) {
        assert(primmode != GLenum. GL_TRIANGLE_FAN && /* primmode != GL_POLYGON  && */ primmode != GLenum.GL_LINE_LOOP);

        if (primmode == GLenum.GL_LINE_STRIP || primmode == GLenum.GL_TRIANGLE_STRIP || /* primmode == GL_QUAD_STRIP || */
                primmode == GLenum.GL_LINE_STRIP_ADJACENCY || primmode == GLenum.GL_TRIANGLE_STRIP_ADJACENCY)
        {
            cmd.header = NvToken.s_nvcmdlist_header[GLenum.GL_DRAW_ARRAYS_STRIP_COMMAND_NV];
        }
        else
        {
            cmd.header = NvToken.s_nvcmdlist_header[GLenum.GL_DRAW_ARRAYS_COMMAND_NV];
        }
    }
}
