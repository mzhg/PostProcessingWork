package jet.opengl.demos.nvidia.waves.crest.helpers;

import org.lwjgl.util.vector.ReadableVector3f;

public interface IFloatingOrigin {
    /// <summary>
    /// Set a new origin. This is equivalent to subtracting the new origin position from any world position state.
    /// </summary>
    void SetOrigin(ReadableVector3f newOrigin);
}
