package jet.opengl.demos.amdfx.geometry;

final class MeshHandle {
    MeshHandle(){}

    MeshHandle(int index)
    {
        this.index = index;
    }

    int index = -1;
    StaticMesh mesh;
}
