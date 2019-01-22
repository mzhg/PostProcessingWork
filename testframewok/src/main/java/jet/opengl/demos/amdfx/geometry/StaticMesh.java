package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;

final class StaticMesh {
    BufferGL vertexBuffer;
    BufferGL indexBuffer;
    BufferGL meshConstantsBuffer;

    int vertexCount;
    int faceCount;
    int indexCount;
    int meshIndex;

    int indexOffset;
    int vertexOffset;

    static final class Cluster
    {
        final Vector3f aabbMin = new Vector3f();
        final Vector3f aabbMax = new Vector3f();
        final Vector3f coneCenter = new Vector3f();
        final Vector3f coneAxis = new Vector3f();

        float coneAngleCosine;
        boolean valid;
    }

    Cluster[] clusters;

    StaticMesh( int vertexCount,  int indexCount,  int meshIndex){
        this.vertexCount = vertexCount;
        this.faceCount = indexCount/3;
        this.indexCount = indexCount;
        this.meshIndex = meshIndex;
    }
}
