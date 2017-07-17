package jet.opengl.demos.demos.os;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

final class TranslationAnimator extends Animator{
    private final Vector3f startPos = new Vector3f();
    private final Vector3f stopPos = new Vector3f();

    void setStartPos(ReadableVector3f pos) {startPos.set(pos);}
    void setStopPos(float x, float y, float z)  {stopPos.set(x,y,z);}

    @Override
    void apply(Transform transform) {
        float time = time();

        Vector3f.linear(startPos, 1.0f - time, stopPos, time, transform.translate);
    }
}
