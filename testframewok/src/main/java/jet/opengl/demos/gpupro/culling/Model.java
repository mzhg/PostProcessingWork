package jet.opengl.demos.gpupro.culling;

import org.lwjgl.util.vector.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.BoundingBox;

final class Model {
    final List<Mesh> mMeshes = new ArrayList<>();
    Material mMaterial;

    final BoundingBox mAABB = new BoundingBox();
    final Matrix4f mWorld = new Matrix4f();
}
