package nv.samples.cmdlist;

import jet.opengl.postprocessing.util.Numeric;

public class DrawElementsCommandNV {
    static final int SIZE = 16;

    int  header;
    int  count;
    int  firstIndex;
    int  baseVertex;

    int load(byte[] data, int position){
        header = Numeric.getInt(data, position);  position+=4;
        count = Numeric.getInt(data, position);  position+=4;
        firstIndex = Numeric.getInt(data, position);  position+=4;
        baseVertex = Numeric.getInt(data, position);  position+=4;

        return position;
    }

    int store(byte[] data, int position){
        position = Numeric.getBytes(header, data, position);
        position = Numeric.getBytes(count, data, position);
        position = Numeric.getBytes(firstIndex, data, position);
        position = Numeric.getBytes(baseVertex, data, position);

        return position;
    }

}
