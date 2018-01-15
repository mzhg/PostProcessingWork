package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/14.
 */

public abstract class CPUTBuffer implements Disposeable {

    CPUTMapType mMappedType;

    public CPUTBuffer(){mMappedType = CPUTMapType.CPUT_MAP_UNDEFINED;}
    public CPUTBuffer(String name) {mMappedType = CPUTMapType.CPUT_MAP_UNDEFINED;}

    public abstract String getName();
}
