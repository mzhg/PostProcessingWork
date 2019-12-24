package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;

final class InstanceData {
    Matrix4f mWorld;
    int mMaterialID;
    int mMeshIndex;  // the index for the ExpandMeshes in Scene.
}
