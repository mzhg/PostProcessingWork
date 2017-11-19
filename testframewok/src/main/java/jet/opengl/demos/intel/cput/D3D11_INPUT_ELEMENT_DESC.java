package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2017/11/15.
 */

public final class D3D11_INPUT_ELEMENT_DESC {
    String SemanticName;
    int SemanticIndex;
    int Format;
    int InputSlot;
    int AlignedByteOffset;
    int InputSlotClass;
    int InstanceDataStepRate;

    public void zeros(){
        SemanticName = null;
        SemanticIndex = 0;
        Format = 0;
        InputSlot = 0;
        AlignedByteOffset = 0;
        InputSlotClass = 0;
        InstanceDataStepRate = 0;
    }
}
