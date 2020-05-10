package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenStencilRef {
    public static final int   ID = GLenum.GL_STENCIL_REF_COMMAND_NV;

    public final StencilRefCommandNV cmd = new StencilRefCommandNV();

    NVTokenStencilRef() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
