package jet.opengl.demos.intel.cput;

import jet.opengl.postprocessing.shader.Macro;

/**
 * Global Asset Library<p></p>
 *
 * The purpose of this library is to keep a copy of all loaded assets and
 * provide a one-stop-loading system.  All assets that are loaded into the
 * system via the Getxxx() operators stays in the library.  Further Getxxx()
 * operations on an already loaded object will addref and return the previously
 * loaded object.<p></p>
 *-----------------------------------------------------------------------------
 * node that holds a single library object<br>
 * Created by mazhen'gui on 2017/11/14.
 */

final class CPUTAssetListEntry {
    String             name;
    Object             pData;
    CPUTAssetListEntry pNext;

    // new properties.
    CPUTModel          pModel;
    int                meshIndex;
    String             fileName;     // potentially non-unique
    Macro[]            pShaderMacros;
}
