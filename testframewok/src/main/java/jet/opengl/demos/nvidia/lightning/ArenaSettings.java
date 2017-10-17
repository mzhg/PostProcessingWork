package jet.opengl.demos.nvidia.lightning;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/8/30.
 */

final class ArenaSettings {
    boolean	Fence=true;
    boolean	InterCoil=true;
    boolean	CoilHelix=true;
    boolean	Chain=true;
    boolean	Scene=true;
    boolean	Lines=false;

    float   AnimationSpeed=10.0f;
    boolean	Glow=true;
    final Vector3f BlurSigma = new Vector3f();

    final LightningAppearance Beam = new LightningAppearance();
}
