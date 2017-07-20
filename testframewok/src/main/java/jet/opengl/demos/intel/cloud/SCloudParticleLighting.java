package jet.opengl.demos.intel.cloud;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/7/7.
 */

final class SCloudParticleLighting {
    static final int SIZE = 12 * 4;

    final Vector4f f4SunLight = new Vector4f();
    final Vector2f f2SunLightAttenuation = new Vector2f(); // x == Direct Sun Light Attenuation
    float pad0, pad1;
    // y == Indirect Sun Light Attenuation
    final Vector4f f4AmbientLight = new Vector4f();
}
