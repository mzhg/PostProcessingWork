package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/11.
 */

final class CPUTVertexElementDesc {
    int mVertexElementSemantic;     // what is is , position, UV's, etc.
    int     mVertexElementType; // what is the data type, floats, ints, etc.
    int                          mElementSizeInBytes;   // # bytes of this element
    int                          mOffset;   // what is the offset within the vertex data

    int Read(byte[] data, int position){
        mVertexElementSemantic = Numeric.getInt(data, position); position+=4;
        mVertexElementType = Numeric.getInt(data, position); position+=4;
        mElementSizeInBytes = Numeric.getInt(data, position); position+=4;
        mOffset = Numeric.getInt(data, position); position+=4;

        return position;
    }
}
