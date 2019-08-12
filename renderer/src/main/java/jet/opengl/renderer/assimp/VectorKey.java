package jet.opengl.renderer.assimp;

import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;
import org.lwjgl.util.vector.Vector3f;

final class VectorKey implements Comparable<VectorKey>{
    final Vector3f mValue = new Vector3f();
    float mTime;

    @Override
    public int compareTo(VectorKey vectorKey) {
        return Float.compare(mTime, vectorKey.mTime);
    }
}
