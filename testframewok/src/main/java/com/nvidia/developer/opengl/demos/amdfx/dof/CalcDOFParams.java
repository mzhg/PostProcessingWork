package com.nvidia.developer.opengl.demos.amdfx.dof;

import org.lwjgl.util.vector.Readable;

import java.nio.ByteBuffer;

/**
 * Created by mazhen'gui on 2017/6/29.
 */

final class CalcDOFParams implements Readable{
    static final int SIZE = 12 * 4;

    int          ScreenParamsX;
    int          ScreenParamsY;
    //  int padding0
    //  int
    float        zNear;
    float        zFar;
    float        focusDistance;
    float        fStop;
    float        focalLength;
    float        maxRadius;
    float        forceCoc;

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        buf.putInt(ScreenParamsX);
        buf.putInt(ScreenParamsY);
        buf.putInt(0);
        buf.putInt(0);

        buf.putFloat(zNear);
        buf.putFloat(zFar);
        buf.putFloat(focusDistance);
        buf.putFloat(fStop);

        buf.putFloat(focalLength);
        buf.putFloat(maxRadius);
        buf.putFloat(forceCoc);
        buf.putFloat(0);

        return buf;
    }
}
