package jet.opengl.demos.nvidia.waves.crest.gpureadback;

import org.lwjgl.util.vector.Vector2f;

/** Interface for an object that will provide min and max resolutions that should be read back. */
public interface IReadbackSettingsProvider {
    void GetMinMaxGridSizes(Vector2f gridSize);
}
