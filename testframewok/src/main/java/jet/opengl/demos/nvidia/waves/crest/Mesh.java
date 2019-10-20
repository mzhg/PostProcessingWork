package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.BoundingBox;

public final class Mesh {
    public Vector3f[] vertices;
    public Vector3f[] normals;
    public Vector2f[] uv;
    public int[] indices;
    public int mode;

    public BoundingBox bounds;

    public String name;

    public void SetIndices(int[] arrI, int mode){
        this.indices = arrI;
        this.mode = mode;
    }

    public void RecalculateBounds(){
        throw new UnsupportedOperationException();
    }
}
