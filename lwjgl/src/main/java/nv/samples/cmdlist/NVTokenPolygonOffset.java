package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenPolygonOffset {
    public static final int   ID = GLenum.GL_POLYGON_OFFSET_COMMAND_NV;

    public final PolygonOffsetCommandNV  cmd = new PolygonOffsetCommandNV();

    NVTokenPolygonOffset() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
