package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

public class VaTriangleMeshConstructParams implements VaConstructorParamsBase {
    final int VertexStride;

    VaTriangleMeshConstructParams(int vertexStride){
        VertexStride = vertexStride;
    }
}
