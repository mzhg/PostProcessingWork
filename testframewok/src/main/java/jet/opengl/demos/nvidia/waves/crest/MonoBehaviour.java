package jet.opengl.demos.nvidia.waves.crest;

import org.lwjgl.util.vector.Transform;

public abstract class MonoBehaviour {

    public boolean enabled;

    public final Transform transform = new Transform();

    protected   void OnEnable(){}

    protected    void OnDisable(){}
}
