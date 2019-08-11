package nv.visualFX.cloth.libs;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.BoundingBox;

/**
 * class representing a rigid euclidean transform as a quaternion and a vector<p></p>
 * Created by mazhen'gui on 2017/9/13.
 */

public class PxTransform {
    public final Vector3f p = new Vector3f();
    public final Quaternion q = new Quaternion();

    public void set(PxTransform other){
        p.set(other.p);
        q.set(other.q);
    }

    public void setIdentity(){
        q.setIdentity();
        p.set(0,0,0);
    }

    public static Vector3f transform(PxTransform left, ReadableVector3f right, Vector3f dest){
        if(dest == null)
            dest = new Vector3f();

        Quaternion.transform(left.q, right, dest);
        Vector3f.add(dest, left.p, dest);
        return dest;
    }

    public static BoundingBox transform(PxTransform left, BoundingBox right, BoundingBox dest ){
        if(dest == null){
            dest = new BoundingBox();
        }

        transform(left, right._max, dest._max);
        transform(left, right._min, dest._min);

        return dest;
    }

    public Matrix4f toMat(Matrix4f out){
        if(out == null)
            out = new Matrix4f();

        q.toMatrix(out);
        out.m30  = p.x;
        out.m31  = p.y;
        out.m32  = p.z;
        out.m33  = 1;

        return out;
    }

    public static PxTransform invert(PxTransform src, PxTransform out){
        if(out == null)
            out = new PxTransform();

        Quaternion.invert(src.q, out.q);
        Vector3f.scale(src.p, -1, out.p);

        return out;
    }
}
