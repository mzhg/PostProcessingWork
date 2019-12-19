package jet.opengl.demos.gpupro.culling;

import com.nvidia.developer.opengl.models.GLVAO;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.util.BoundingBox;

final class Mesh {
    BufferGL mVertexs;
    BufferGL mIndices;
    int mPology;
    int count;

    GLVAO mVao;

    MeshType mType;

    final BoundingBox mAABB = new BoundingBox();
    final Matrix4f mWorld = new Matrix4f();
}
