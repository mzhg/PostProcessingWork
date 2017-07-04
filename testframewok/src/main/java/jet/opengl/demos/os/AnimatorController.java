package jet.opengl.demos.os;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by mazhen'gui on 2017/6/6.
 */

final class AnimatorController {
    private final List<Animator> m_animators = new LinkedList<>();
    private final List<AnimationEventListener> m_listeners = new ArrayList<>();

    void addEventListener(AnimationEventListener listener) {
        if(listener == null || m_listeners.contains(listener))
            return;

        m_listeners.add(listener);
    }

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
                onFinished(animator);
            }

            animator.play(drawable.transform, dt);
        }
    }

    private void onFinished(Animator animator){
        for(AnimationEventListener listener : m_listeners){
            listener.onFinished(animator.getID());
        }
    }
}
