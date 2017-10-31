package com.nvidia.developer.opengl.models;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/10/30.
 */

public class QuadricDisk implements QuadricGenerator {

    private float innerRadius = 0.3f;
    private float outerRadius = 1.0f;

    public QuadricDisk(){}

    public QuadricDisk(float innerRadius, float outerRadius) {
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public void setOuterRadius(float outerRadius) {
        this.outerRadius = outerRadius;
    }

    @Override
    public void genVertex(float s, float t, Vector3f position, Vector3f normal, Vector2f texCoord, Vector4f color) {
        if(innerRadius < 0.0f){
            throw new IllegalArgumentException("The 'innerRadius' can't be less than 0. innerRadius = " + innerRadius);
        }

        if(outerRadius < 0.0f){
            throw new IllegalArgumentException("The 'outerRadius' can't be less than 0. outerRadius = " + outerRadius);
        }

        if(outerRadius < innerRadius){
            throw new IllegalArgumentException("The 'outerRadius' can't be less than the 'innerRadius'.");
        }

        double theta = Math.PI * 2 * s;

        position.x = (float) Math.cos(theta);
        position.y = (float) Math.sin(theta);
        position.z = 0;

        float startX = position.x * innerRadius;
        float startY = position.y * innerRadius;
        float length = (outerRadius - innerRadius) * t;

        position.x = startX + position.x * length;
        position.y = startY + position.y * length;

        if(normal != null){
            normal.set(0,1,0);
        }

        if(texCoord != null){
            float texCoordX = (position.x + outerRadius)/(2.0f * outerRadius);
            float texCoordY = (position.y + outerRadius)/(2.0f * outerRadius);
            texCoord.set(texCoordX, texCoordY);
        }

        if(color != null){
            color.set(1,1,1,1); // pure white.
        }
    }
}
