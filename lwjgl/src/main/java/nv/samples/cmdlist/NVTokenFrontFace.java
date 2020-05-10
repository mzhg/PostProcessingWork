package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenFrontFace {
    public static final int   ID = GLenum.GL_FRONT_FACE_COMMAND_NV;

    FrontFaceCommandNV  cmd;

    NVTokenFrontFace() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }

    void setFrontFace(int winding){
        cmd.frontFace = winding == GLenum.GL_CCW ? 1 : 0;
    }
}
