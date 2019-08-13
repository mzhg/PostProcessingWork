package jet.opengl.renderer.assimp;

import org.lwjgl.util.vector.Quaternion;

final class QuatKey implements Comparable<QuatKey>, GetTime{
    float mTime;
    final Quaternion mValue = new Quaternion();


    @Override
    public int compareTo(QuatKey quatKey) {
        return Float.compare(mTime, quatKey.mTime);
    }

    @Override
    public float getTime() {
        return mTime;
    }
}
