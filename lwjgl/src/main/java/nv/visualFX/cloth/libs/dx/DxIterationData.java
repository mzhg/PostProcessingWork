package nv.visualFX.cloth.libs.dx;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.util.CacheBuffer;
import nv.visualFX.cloth.libs.IterationState;

/**
 * per-iteration data (stored in pinned memory)<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxIterationData implements Readable{
    static final int SIZE = (24+3+1)*4;
    final float[] mIntegrationTrafo = new float[24];
    final float[] mWind = new float[3];
    int mIsTurning;

    static void copySquareTransposed(float[] dst, int dst_offset, Matrix4f src, int src_offset)
    {
        final boolean tranposed = true;
        dst[0 + dst_offset] = src.get(src_offset+0, tranposed);
        dst[1 + dst_offset] = src.get(src_offset+4, tranposed);
        dst[2 + dst_offset] = src.get(src_offset+8, tranposed);
        dst[3 + dst_offset] = src.get(src_offset+1, tranposed);
        dst[4 + dst_offset] = src.get(src_offset+5, tranposed);
        dst[5 + dst_offset] = src.get(src_offset+9, tranposed);
        dst[6 + dst_offset] = src.get(src_offset+2, tranposed);
        dst[7 + dst_offset] = src.get(src_offset+6, tranposed);
        dst[8 + dst_offset] = src.get(src_offset+10, tranposed);
    }

    static void copySquareTransposed(float[] dst, int dst_offset, float[] src, int src_offset)
    {
        dst[0 + dst_offset] = src[src_offset+0];
        dst[1 + dst_offset] = src[src_offset+4];
        dst[2 + dst_offset] = src[src_offset+8];
        dst[3 + dst_offset] = src[src_offset+1];
        dst[4 + dst_offset] = src[src_offset+5];
        dst[5 + dst_offset] = src[src_offset+9];
        dst[6 + dst_offset] = src[src_offset+2];
        dst[7 + dst_offset] = src[src_offset+6];
        dst[8 + dst_offset] = src[src_offset+10];
    }

    static float[] array(Vector4f[] srcs){
        float[] results = new float[srcs.length];
        int position = 0;
        for(int i = 0; i < srcs.length; i++){
            srcs[i].store(results, position);
            position += 4;
        }

        return results;
    }

    DxIterationData(IterationState state)
    {
        mIntegrationTrafo[0] = state.mPrevBias.x;
        mIntegrationTrafo[1] = state.mPrevBias.y;
        mIntegrationTrafo[2] = state.mPrevBias.z;

        mIntegrationTrafo[3] = state.mCurBias.x;
        mIntegrationTrafo[4] = state.mCurBias.y;
        mIntegrationTrafo[5] = state.mCurBias.z;

        copySquareTransposed(mIntegrationTrafo, 6, array(state.mPrevMatrix),0);
        copySquareTransposed(mIntegrationTrafo, 15, array(state.mCurMatrix),0);

        mIsTurning = state.mIsTurning ? 1:0;

        mWind[0] = state.mWind.x;
        mWind[1] = state.mWind.y;
        mWind[2] = state.mWind.z;
    }

    @Override
    public ByteBuffer store(ByteBuffer buf) {
        CacheBuffer.put(buf, mIntegrationTrafo);
        CacheBuffer.put(buf, mWind);
        buf.putInt(mIsTurning);
        return buf;
    }
}
