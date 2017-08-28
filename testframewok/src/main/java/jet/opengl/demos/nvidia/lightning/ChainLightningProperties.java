package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/8/28.
 */

final class ChainLightningProperties {
    static final int MaxTargets = 8;

    final Vector3f ChainSource = new Vector3f();
    float Dummy0;

    final Vector4f[] ChainTargetPositions = new Vector4f[MaxTargets];
    int			NumTargets;
}
