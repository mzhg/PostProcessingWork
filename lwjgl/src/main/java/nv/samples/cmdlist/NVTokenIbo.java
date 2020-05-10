package nv.samples.cmdlist;

import jet.opengl.postprocessing.common.GLenum;

public class NVTokenIbo {
    public static final int   ID = GLenum.GL_ELEMENT_ADDRESS_COMMAND_NV;

    public final ElementAddressCommandNV     cmd = new ElementAddressCommandNV();
    public final ElementAddressCommandEMU    cmdEMU = new ElementAddressCommandEMU();

    void setType(int type){
        if (type == GLenum.GL_UNSIGNED_BYTE){
            cmd.typeSizeInByte = 1;
        }
        else if (type == GLenum.GL_UNSIGNED_SHORT){
            cmd.typeSizeInByte = 2;
        }
        else if (type == GLenum.GL_UNSIGNED_INT){
            cmd.typeSizeInByte = 4;
        }
        else{
//            assert(0 && "illegal type");
            throw new IllegalStateException("illegal type");
        }
    }

    void setBuffer(int buffer, long address)
    {
        if (NvToken.s_nvcmdlist_bindless){
            cmd.addressLo = (int)(address & 0xFFFFFFFF);
            cmd.addressHi = (int)(address >> 32);
        }
        else{
            cmdEMU.buffer = buffer;
            cmdEMU._pad   = 0;
        }
    }

    NVTokenIbo() {
        cmd.header  = NvToken.s_nvcmdlist_header[ID];
    }
}
