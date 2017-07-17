package jet.opengl.demos.demos.os;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

final class ScaleAnimator extends Animator{
    final Vector3f startScale = new Vector3f(1,1,1);
    final Vector3f endScale = new Vector3f(1,1,1);

    void setStartScale(float scaleX, float scaleY, float scaleZ) {startScale.set(scaleX, scaleY, scaleZ);}
    void setStopScale(float scaleX, float scaleY, float scaleZ) {endScale.set(scaleX, scaleY, scaleZ);}

    @Override
    void apply(Transform transform) {
        float time = time();

        Vector3f.linear(startScale, 1.0f - time, endScale, time, transform.scale);
    }
}
