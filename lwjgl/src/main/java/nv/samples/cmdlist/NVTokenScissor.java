package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenScissor {
    public static final int   ID = GLenum.GL_SCISSOR_COMMAND_NV;

    public final ScissorCommandNV  cmd = new ScissorCommandNV();

    NVTokenScissor() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
