package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenDrawElemsStrip {
    public static final int   ID = GLenum.GL_DRAW_ELEMENTS_STRIP_COMMAND_NV;

    public final DrawElementsCommandNV   cmd = new DrawElementsCommandNV();

    NVTokenDrawElemsStrip() {
        cmd.baseVertex = 0;
        cmd.firstIndex = 0;
        cmd.count = 0;

        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }

    void setParams(int count, int firstIndex, int baseVertex)
    {
        cmd.count = count;
        cmd.firstIndex = firstIndex;
        cmd.baseVertex = baseVertex;
    }
}
