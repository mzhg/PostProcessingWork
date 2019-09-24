package jet.opengl.loader.assimp;

import org.lwjgl.util.vector.Vector3f;

final class VectorKey implements Comparable<VectorKey>, GetTime{
    final Vector3f mValue = new Vector3f();
    float mTime;

    @Override
    public int compareTo(VectorKey vectorKey) {
        return Float.compare(mTime, vectorKey.mTime);
    }

    @Override
    public float getTime() {
        return mTime;
    }
}
