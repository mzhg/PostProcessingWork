package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Transform {
    final Vector3f translate = new Vector3f();
    final Quaternion rotation = new Quaternion();
    final Vector3f scale = new Vector3f();

    void set(Transform other){
        translate.set(other.translate);
        rotation.set(other.rotation);
        scale.set(other.scale);
    }
}
