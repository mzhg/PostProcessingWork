package jet.opengl.demos.nvidia.illumination;

import org.lwjgl.util.vector.Vector4f;

/**
 * values needed in the propagation step of the LPV
 * these values let us save time on the propapation step
 * xyz give the normalized direction to each of the 6 pertinent faces of the 6 neighbors of a cube
 * solid angle gives the fractional solid angle that that face subtends<p></p>
 * Created by Administrator on 2017/11/13 0013.
 */

final class PropagateConsts {
    static final int SIZE = Vector4f.SIZE * 2;

    final Vector4f neighborOffset = new Vector4f();
    float solidAngle;
    float x;
    float y;
    float z;
}
