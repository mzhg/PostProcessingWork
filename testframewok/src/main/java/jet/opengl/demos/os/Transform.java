package jet.opengl.demos.os;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Transform {
    final Vector3f translate = new Vector3f();
    final Quaternion rotation = new Quaternion();
    final Vector3f scale = new Vector3f();

//    private final Vector3f temp = new Vector3f();

    void set(Transform other){
        translate.set(other.translate);
        rotation.set(other.rotation);
        scale.set(other.scale);
    }

    void toMatrix(Matrix4f mat){
        mat.setIdentity();
        mat.m30 = translate.x;
        mat.m31 = translate.y;
        mat.m32 = translate.z;

        rotation.toMatrix(mat);
        mat.scale(scale);
    }
}
