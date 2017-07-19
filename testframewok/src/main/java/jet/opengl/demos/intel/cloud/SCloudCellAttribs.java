package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class SCloudCellAttribs {
    static final int SIZE = 20 * 4;

    final Vector3f f3Center = new Vector3f();
    float fSize;

    final Vector3f f3Normal = new Vector3f();
    int uiNumActiveLayers;

    final Vector3f f3Tangent = new Vector3f();
    float fDensity;

    final Vector3f f3Bitangent = new Vector3f();
    float fMorphFadeout;

    int uiPackedLocation;
    int pad0;
    int pad1;
    int pad2;
}
