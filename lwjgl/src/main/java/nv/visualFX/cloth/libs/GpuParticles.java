package nv.visualFX.cloth.libs;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.buffer.BufferGL;

/**
 * Created by mazhen'gui on 2017/9/9.
 */

public class GpuParticles {
    FloatBuffer mCurrent;
    FloatBuffer mPrevious;

    int mCurrentOffset;
    int mPreviousOffset;

    BufferGL mBuffer;

    public GpuParticles(int currentOffset, int previousOffset, BufferGL buffer){
        mCurrentOffset = currentOffset;
        mPreviousOffset = previousOffset;
        mBuffer = buffer;
    }
}
