package jet.opengl.renderer.assimp;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Transform;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.postprocessing.util.Numeric;

public class AssimpAnimation {
    float mDuration;
    float mTicksPerSecond;

    int mBoneName;
    VectorKey[] mPositionKeys;
    VectorKey[] mScalingKeys;
    QuatKey[]   mRotationKeys;

    private static long binarySearch(float timeline, GetTime[] vectors){
        int l = 0;
        int r = vectors.length;

        int left = -1;
        int right = -1;

        while (l!=r){
            int middle = (l+r)/2;
            if(timeline < vectors[middle].getTime()){
                if(middle == 0){
                    left = -1;
                    right = 0;
                    break;
                }else if(timeline >= vectors[middle-1].getTime()){
                    left = middle-1;
                    right = middle;
                    break;
                }else{
                    r = middle;
                }
            }else{
                if(middle == vectors.length - 1){
                    left = vectors.length-1;
                    right = -1;
                    break;
                }else if(timeline<vectors[middle+1].getTime()){
                    left = middle;
                    right = middle+1;
                    break;
                }else{
                    l = middle;
                }
            }
        }

        return Numeric.encode(left, right);
    }

    private static void interpolateVectors(float timeline, VectorKey[] vectors, final Vector3f out){
        if(vectors != null){
            if(vectors.length == 1){
                out.set(vectors[0].mValue);
            }else{
                long value = binarySearch(timeline, vectors);
                int left = Numeric.decodeFirst(value);
                int right = Numeric.decodeSecond(value);

                if(left == -1 && right == -1)
                    throw new IllegalStateException("Inner error!");

                if(left == -1){
                    out.set(vectors[0].mValue);
                }else if(right == -1){
                    out.set(vectors[vectors.length-1].mValue);
                }else{
                    VectorKey lv = vectors[left];
                    VectorKey rv = vectors[right];
                    float factor = (timeline - lv.mTime)/(rv.mTime-lv.mTime);
                    Vector3f.mix(lv.mValue, rv.mValue, factor, out);
                }
            }
        }
    }

    void interpolate(float timeline, Transform transform){
        if(mPositionKeys == null && mRotationKeys == null && mScalingKeys == null){
            transform.setIdentity();
            return;
        }

        timeline = (timeline * 24.f) % mDuration;
        Vector3f out = new Vector3f();

        interpolateVectors(timeline, mPositionKeys, out);  transform.setPosition(out.x, out.y, out.z);
        interpolateVectors(timeline, mScalingKeys, out);  transform.setScale(out.x, out.y, out.z);

        if(mRotationKeys != null){
            if(mRotationKeys.length == 1){
                transform.setRotation(mRotationKeys[0].mValue);
            }else{
                long value = binarySearch(timeline, mRotationKeys);
                int left = Numeric.decodeFirst(value);
                int right = Numeric.decodeSecond(value);

                if(left == -1 && right == -1)
                    throw new IllegalStateException("Inner error!");

                if(left == -1){
                    out.set(mRotationKeys[0].mValue);
                }else if(right == -1){
                    out.set(mRotationKeys[mRotationKeys.length-1].mValue);
                }else{
                    QuatKey lv = mRotationKeys[left];
                    QuatKey rv = mRotationKeys[right];
                    float factor = (timeline - lv.mTime)/(rv.mTime-lv.mTime);
                    Quaternion rotationb = Quaternion.slerp(lv.mValue, rv.mValue, factor, null);
                    transform.setRotation(rotationb);
                }
            }
        }
    }
}
