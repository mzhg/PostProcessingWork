package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/15.
 */

final class D3D11_BUFFER_DESC {
    int ByteWidth;
    int Usage;
    int BindFlags;
    int CPUAccessFlags;
    int MiscFlags;
    int StructureByteStride;

    void zeros(){
        ByteWidth = 0;
        Usage = 0;
        BindFlags = 0;
        CPUAccessFlags = 0;
        MiscFlags = 0;
        StructureByteStride = 0;
    }
}
