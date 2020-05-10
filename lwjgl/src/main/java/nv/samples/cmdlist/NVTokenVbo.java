package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenVbo {
    public static final int   ID = GLenum.GL_ATTRIBUTE_ADDRESS_COMMAND_NV;

    public final AttributeAddressCommandNV   cmd = new AttributeAddressCommandNV();
    public final AttributeAddressCommandEMU  cmdEMU = new AttributeAddressCommandEMU();

    void setBinding(int idx){
        cmd.index = idx;
    }

    void setBuffer(int buffer, long address, int offset)
    {
        if (NvToken.s_nvcmdlist_bindless){
            address += offset;
            cmd.addressLo = (int)(address & 0xFFFFFFFF);
            cmd.addressHi = (int)(address >> 32);
        }
        else{
            cmdEMU.buffer = buffer;
            cmdEMU.offset = offset;
        }
    }

    NVTokenVbo() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
