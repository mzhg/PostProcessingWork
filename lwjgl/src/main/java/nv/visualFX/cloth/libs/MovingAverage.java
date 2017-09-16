package nv.visualFX.cloth.libs;

import java.nio.FloatBuffer;

import jet.opengl.postprocessing.util.BufferUtils;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

public class MovingAverage {
    private FloatBuffer mData; //Ring buffer
    private int mBegin; //Index to first element
    private int mCount; //current number of elements
    private int mSize; //max ringbuffer size

    public MovingAverage() { this(1);}

    public MovingAverage(int n ){
        mSize = n;
        mData = BufferUtils.createFloatBuffer(mSize);
    }

    public MovingAverage(MovingAverage other){
        mData = other.mData;
        mBegin = other.mBegin;
        mCount = other.mCount;
        mSize = other.mSize;
    }

    boolean empty()
    {
        return mCount == 0;
    }

    int size()
    {
        return mSize;
    }

    void resize(int n)
    {
//		float* newData = reinterpret_cast<float*>(NV_CLOTH_ALLOC(n * sizeof(float), "MovingAverage"));
        FloatBuffer newData = BufferUtils.createFloatBuffer(n);

		final int cutOffFront = Math.max(mCount - n, 0);
        int index = (mBegin + cutOffFront) % mSize;
        for(int i = 0; i < n; i++)
        {
//            newData[i] = mData[index];
            newData.put(i, mData.get(index));
            index = (index + 1) % mSize;
        }

        mCount -= cutOffFront;

//        NV_CLOTH_FREE(mData);

        mSize = n;
        mData = newData;
        mBegin = 0;
    }

    void reset()
    {
        mCount = 0;
        mBegin = 0;
    }

    void push(int n, float value)
    {
        n = Math.min(n, mSize);
		final int start = (mBegin + mCount) % mSize;
        final int end = start + n;
        final int end1 = Math.min(end, mSize);
        final int end2 = Math.max(end - end1, 0);
        for(int i = start; i < end1; i++)
        {
            mData.put(i, value);
        }
        for(int i = 0; i < end2; i++)
        {
            mData.put(i, value);
        }

        int newCount = Math.min(mCount + n, mSize);
        mBegin = (mBegin + n-(newCount-mCount))%mSize; //move mBegin by the amount of replaced elements
        mCount = newCount;
    }

    float average()
    {
        assert (!empty());

        float sum = 0.0f;
        int totalWeight = 0;
        {
            int count = 0;
            int end = Math.min(mBegin + mCount, mSize);
            int rampSize = Math.max(1,mCount / 8);
            for(int i = mBegin; i < end; i++)
            {
                //ramp weight /''''''\ .
                int weight = Math.min(
                        Math.min(count+1, rampSize), //left ramp /'''
                        Math.min(mCount-(count), rampSize)); //right ramp  '''\ .
                sum += mData.get(i) * weight;
                totalWeight += weight;
                count++;
            }
            int leftOver = mCount-(end - mBegin);
            for(int i = 0; i < leftOver; i++)
            {
                int weight = Math.min(Math.min(count + 1, rampSize), Math.min(mCount - (count), rampSize));
                sum += mData.get(i) * weight;
                totalWeight += weight;
                count++;
            }
            assert (count == mCount);
        }

        return sum /totalWeight;
    }
}
