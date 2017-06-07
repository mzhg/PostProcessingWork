package com.nvidia.developer.opengl.demos.os;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

final class AnimatorController {
    private final List<Animator> m_animators = new LinkedList<>();

    void add(Animator animator){
        m_animators.add(animator);
    }

    void play(Drawable drawable, float dt){
        if(m_animators.isEmpty())
            return;

        Iterator<Animator> iterator = m_animators.iterator();
        while (iterator.hasNext()){
            Animator animator = iterator.next();
            if(animator.isFinished()){
                iterator.remove();
            }

            animator.play(drawable.transform, dt);
        }
    }
}
