package jet.opengl.demos.os;

import org.lwjgl.util.vector.Quaternion;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

class Drawable {
    final Transform transform = new Transform();
    GLSLProgram program;
    Texture2D texture;
    VertexArrayObject buffer;

    private boolean m_visible = true;
    private AnimatorController controller;

    void setVisible(boolean visible) { m_visible = visible;}
    boolean isVisible()              { return m_visible;}

    void addAnimationEventListener(AnimationEventListener listener){
        if(controller == null)
            controller = new AnimatorController();

        controller.addEventListener(listener);
    }

    void addScaleAnimation(float destScaleX, float destScaleY, float destScaleZ, float duration){
        addScaleAnimation(destScaleX, destScaleY, destScaleZ, duration, 0);
    }

    void addScaleAnimation(float destScaleX, float destScaleY, float destScaleZ, float duration, int id){
        if(controller == null)
            controller = new AnimatorController();

        ScaleAnimator animator = new ScaleAnimator();

        animator.setID(id);
        animator.setStartScale(transform.scale.x, transform.scale.y, transform.scale.z);
        animator.setStopScale(destScaleX, destScaleY,destScaleZ);
        animator.setDuration(duration);

        controller.add(animator);
    }

    void addTraslationAnimation(float destX, float destY, float destZ, float duration){
        addTraslationAnimation(destX, destY, destZ, duration, 0);
    }

    void addTraslationAnimation(float destX, float destY, float destZ, float duration, int id){
        if(controller == null)
            controller = new AnimatorController();

        TranslationAnimator animator = new TranslationAnimator();

        animator.setID(id);
        animator.setStartPos(transform.translate);
        animator.setStopPos(destX, destY,destZ);
        animator.setDuration(duration);

        controller.add(animator);
    }

    void addRotationAnimation(Quaternion dest, float duration){
        addRotationAnimation(dest, duration, 0);
    }

    void addRotationAnimation(Quaternion dest, float duration, int id){
        if(controller == null)
            controller = new AnimatorController();

        RotationAnimator animator = new RotationAnimator();

        animator.setID(id);
        animator.setStartRotation(transform.rotation);
        animator.setStopRotation(dest);
        animator.setDuration(duration);

        controller.add(animator);
    }

    void update(float dt){
        if(controller != null){
            controller.play(this, dt);
        }
    }
}
