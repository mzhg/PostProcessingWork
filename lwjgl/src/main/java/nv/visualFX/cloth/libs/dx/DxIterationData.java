package nv.visualFX.cloth.libs.dx;

/**
 * per-iteration data (stored in pinned memory)<p></p>
 * Created by mazhen'gui on 2017/9/12.
 */

final class DxIterationData {
    final float[] mIntegrationTrafo = new float[24];
    final float[] mWind = new float[3];
    int mIsTurning;

    /*cloth::DxIterationData::DxIterationData(const IterationState<Simd4f>& state)
    {
        mIntegrationTrafo[0] = array(state.mPrevBias)[0];
        mIntegrationTrafo[1] = array(state.mPrevBias)[1];
        mIntegrationTrafo[2] = array(state.mPrevBias)[2];

        mIntegrationTrafo[3] = array(state.mCurBias)[0];
        mIntegrationTrafo[4] = array(state.mCurBias)[1];
        mIntegrationTrafo[5] = array(state.mCurBias)[2];

        copySquareTransposed(mIntegrationTrafo + 6, array(*state.mPrevMatrix));
        copySquareTransposed(mIntegrationTrafo + 15, array(*state.mCurMatrix));

        mIsTurning = uint32_t(state.mIsTurning);

        mWind[0] = array(state.mWind)[0];
        mWind[1] = array(state.mWind)[1];
        mWind[2] = array(state.mWind)[2];
    }*/
}
