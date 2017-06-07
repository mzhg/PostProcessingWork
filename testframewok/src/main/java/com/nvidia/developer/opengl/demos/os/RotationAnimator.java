package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Quaternion;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

final class RotationAnimator extends Animator{
    final Quaternion m_startRotation = new Quaternion();
    final Quaternion m_stopRotation = new Quaternion();

    public void setStartRotation(Quaternion quaternion) {
        m_startRotation.set(quaternion);
    }

    public void setStopRotation(Quaternion quaternion) {
        m_stopRotation.set(quaternion);
    }

    @Override
    void apply(Transform transform) {
        float time = time();

        Quaternion.slerp(m_startRotation, m_stopRotation, time, transform.rotation);
    }
}
