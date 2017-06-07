package com.nvidia.developer.opengl.demos.os;

import org.lwjgl.util.vector.Quaternion;

import jet.opengl.postprocessing.buffer.VertexArrayObject;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/6/5.
 */

final class Drawable {
    final Transform transform = new Transform();
    GLSLProgram program;
    Texture2D texture;
    VertexArrayObject buffer;

    private AnimatorController controller;

    void addScaleAnimation(float destScaleX, float destScaleY, float destScaleZ, float duration){
        if(controller == null)
            controller = new AnimatorController();

        ScaleAnimator animator = new ScaleAnimator();

        animator.setStartScale(transform.scale.x, transform.scale.y, transform.scale.z);
        animator.setStopScale(destScaleX, destScaleY,destScaleZ);
        animator.setDuration(duration);

        controller.add(animator);
    }

    void addTraslationAnimation(float destX, float destY, float destZ, float duration){
        if(controller == null)
            controller = new AnimatorController();

        TranslationAnimator animator = new TranslationAnimator();

        animator.setStartPos(transform.translate);
        animator.setStopPos(destX, destY,destZ);
        animator.setDuration(duration);

        controller.add(animator);
    }

    void addRotationAnimation(Quaternion dest, float duration){
        if(controller == null)
            controller = new AnimatorController();

        RotationAnimator animator = new RotationAnimator();

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
