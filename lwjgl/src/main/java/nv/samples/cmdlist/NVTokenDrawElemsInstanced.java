package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenDrawElemsInstanced {

    public static final int   ID = GLenum.GL_DRAW_ELEMENTS_INSTANCED_COMMAND_NV;

    public final DrawElementsInstancedCommandNV   cmd = new DrawElementsInstancedCommandNV();

    public NVTokenDrawElemsInstanced() {
        cmd.mode = GLenum.GL_TRIANGLES;
        cmd.baseInstance = 0;
        cmd.baseVertex = 0;
        cmd.firstIndex = 0;
        cmd.count = 0;
        cmd.instanceCount = 1;

        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }

    public void setMode(int primmode) {
        cmd.mode = primmode;
    }

    public void setParams(int count, int firstIndex/*=0*/, int baseVertex/*=0*/)
    {
        cmd.count = count;
        cmd.firstIndex = firstIndex;
        cmd.baseVertex = baseVertex;
    }

    public void setInstances(int count, int baseInstance/*=0*/){
        cmd.baseInstance  = baseInstance;
        cmd.instanceCount = count;
    }
}