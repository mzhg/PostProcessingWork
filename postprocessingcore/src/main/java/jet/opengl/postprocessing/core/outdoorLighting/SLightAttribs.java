package jet.opengl.postprocessing.core.outdoorLighting;

import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/6/3.
 */

final class SLightAttribs {
    final Vector4f f4DirOnLight = new Vector4f();
    final Vector4f f4LightScreenPos = new Vector4f();
    boolean bIsLightOnScreen;
}
