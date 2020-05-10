package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenViewport {
    public static final int   ID = GLenum.GL_VIEWPORT_COMMAND_NV;

    public final ViewportCommandNV cmd = new ViewportCommandNV();

    NVTokenViewport() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
