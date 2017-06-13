package com.nvidia.developer.opengl.demos.os;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

abstract class Animator {
    private float m_duration;  // seconds
    private float m_elpsedTime;
    private int m_id;

    float time(){ return Numeric.clamp(m_elpsedTime / m_duration, 0, 1);}
    boolean isFinished() { return m_elpsedTime >= m_duration;}

    void setID(int id) { m_id = id;}
    int getID()   { return  m_id;}

    void play(Transform transform, float dt){
        if(isFinished())
            return;

        apply(transform);
        m_elpsedTime += dt;
    }

    void setDuration(float duration){
        this.m_duration = duration;
    }

    abstract void apply(Transform transform);
}
