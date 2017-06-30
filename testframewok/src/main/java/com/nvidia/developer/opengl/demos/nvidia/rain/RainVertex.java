package com.nvidia.developer.opengl.demos.nvidia.rain;

import org.lwjgl.util.vector.Vector3f;

/**
 * Created by mazhen'gui on 2017/6/30.
 */

final class RainVertex {
    final Vector3f pos = new Vector3f();
    final Vector3f seed = new Vector3f();
    final Vector3f speed= new Vector3f();
    float random;
    byte  Type;
}
