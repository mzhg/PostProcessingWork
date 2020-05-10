package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenDrawArraysInstanced {
    public static final int   ID = GLenum.GL_DRAW_ARRAYS_INSTANCED_COMMAND_NV;

    public final DrawArraysInstancedCommandNV          cmd = new DrawArraysInstancedCommandNV();

    public NVTokenDrawArraysInstanced() {
        cmd.mode = GLenum.GL_TRIANGLES;
        cmd.baseInstance = 0;
        cmd.first = 0;
        cmd.count = 0;
        cmd.instanceCount = 1;

        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }

    public void setMode(int primmode) {
        cmd.mode = primmode;
    }

    public void setParams(int count, int first/*=0*/)
    {
        cmd.count = count;
        cmd.first = first;
    }

    public void setInstances(int count, int baseInstance/*=0*/){
        cmd.baseInstance  = baseInstance;
        cmd.instanceCount = count;
    }
}
