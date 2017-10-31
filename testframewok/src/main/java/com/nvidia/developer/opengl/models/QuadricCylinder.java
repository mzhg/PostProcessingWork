package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/10/30.
 */

public class QuadricCylinder implements QuadricGenerator {
    /** The radius of the cylinder at the z = 0. */
    private float baseRadius = 1;
    /** The radius of the cylinder at z = height. */
    private float topRadius = 1;
    /** The height of the cylinder. */
    private float height = 1;

    public QuadricCylinder() {}

    public QuadricCylinder(float baseRadius, float topRadius, float height) {
        this.baseRadius = baseRadius;
        this.topRadius = topRadius;
        this.height = height;
    }

    @Override
    public void genVertex(float s, float t, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color) {
        final double theta = Math.PI * 2 * s;
        final double x = Math.cos(theta);
        final double y = Math.sin(theta);
        final float currentRadius =Numeric.mix(baseRadius, topRadius, t);

        position.x = (float) (x * currentRadius);
        position.y = (float) (y * currentRadius);
        position.z = t * height;

        if(normal != null){
            float nz = (baseRadius - topRadius) / height;
            normal.set((float)x, (float)y, nz);
            normal.normalise();
        }

        if(texCoord != null){
            texCoord.set(s, t);
            texCoord.x = evoluteTexcoordS(s);
        }

        if(color != null){
            color.set(1,1,1,1); // pure white.
        }
    }

    private static float evoluteTexcoordS(float s){
        if(s <= 0.25f){
            return -s + 0.25f;
        }else if(s < 1.0f){
            return 1.25f - s;
        }else{  // s == 1.0
            return 1.0f;
        }
    }
}
