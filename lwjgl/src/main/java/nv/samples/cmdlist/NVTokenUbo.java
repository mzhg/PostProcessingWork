package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenUbo {
    public static final int   ID = GLenum.GL_UNIFORM_ADDRESS_COMMAND_NV;

    public final UniformAddressCommandNV   cmd = new UniformAddressCommandNV();
    public final UniformAddressCommandEMU  cmdEMU = new UniformAddressCommandEMU();

    void setBuffer(int buffer, long address, int offset, int size)
    {
        assert(size % 4 == 0 && offset % 256 == 0);
        if (NvToken.s_nvcmdlist_bindless){
            address += offset;
            cmd.addressLo = (int)(address & 0xFFFFFFFF);
            cmd.addressHi = (int)(address >> 32);
        }
        else{
            cmdEMU.buffer = buffer;
            cmdEMU.offset256 = (short) (offset / 256);
            cmdEMU.size4     = (short) (size / 4);
        }
    }

    void setBinding(int idx, NVTokenShaderStage stage){
        cmd.index = (short) idx;
        cmd.stage = (short) NvToken.s_nvcmdlist_stages[stage.ordinal()];
    }

    NVTokenUbo() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
