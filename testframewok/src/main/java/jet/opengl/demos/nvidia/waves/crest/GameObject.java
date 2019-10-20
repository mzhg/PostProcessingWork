package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Transform;

public class GameObject{
    private String name;
    public int layer;
    public final Transform transform = new Transform();
    public Transform parent;

    public OceanChunkRenderer renderer;
    public Mesh mesh;

    public int sortingOrder;

    public GameObject(String name){
        this.name = name;
    }
}
