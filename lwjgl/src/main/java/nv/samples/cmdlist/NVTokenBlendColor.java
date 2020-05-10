package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenBlendColor {
    public static final int   ID = GLenum.GL_BLEND_COLOR_COMMAND_NV;

    /*BlendColorCommandNV*/int     cmd = NvToken.s_nvcmdlist_header[ID];

    /*NVTokenBlendColor() {
        cmd.header  = s_nvcmdlist_header[ID];
    }*/
}
