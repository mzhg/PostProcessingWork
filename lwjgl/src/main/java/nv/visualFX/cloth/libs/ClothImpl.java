package nv.visualFX.cloth.libs;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Created by mazhen'gui on 2017/9/13.
 */

public abstract class ClothImpl implements Cloth {
    protected static final int FLT_MAX_EXP     =128;
    public final Vector3f mParticleBoundsCenter = new Vector3f();
    public final Vector3f mParticleBoundsHalfExtent = new Vector3f();

    public final Vector3f mGravity = new Vector3f();
    public final Vector3f mLogDamping = new Vector3f();
    public final Vector3f mLinearLogDrag = new Vector3f();
    public final Vector3f mAngularLogDrag = new Vector3f();
    public final Vector3f mLinearInertia = new Vector3f();
    public final Vector3f mAngularInertia = new Vector3f();
    public final Vector3f mCentrifugalInertia = new Vector3f();
    public float mSolverFrequency;
    public float mStiffnessFrequency;

    public final PxTransform mTargetMotion = new PxTransform();
    public final PxTransform mCurrentMotion = new PxTransform();
    public final Vector3f mLinearVelocity = new Vector3f();
    public final Vector3f mAngularVelocity = new Vector3f();

    public float mPrevIterDt;
    public MovingAverage mIterDtAvg;

    // wind
    public final Vector3f mWind = new Vector3f();
    public float mDragLogCoefficient;
    public float mLiftLogCoefficient;
    public float mFluidDensity;

    // sleeping
    public int mSleepTestInterval; // how often to test for movement
    public int mSleepAfterCount;   // number of tests to pass before sleep
    public float mSleepThreshold;       // max movement delta to pass test
    public int mSleepPassCounter;  // how many tests passed
    public int mSleepTestCounter;  // how many iterations since tested

    @Override
    public void setTranslation(ReadableVector3f trans) {
//        physx::PxVec3 t = reinterpret_cast<const physx::PxVec3&>(trans);
        if (mTargetMotion.p.equals(trans))
            return;

        mTargetMotion.p.set(trans);
        wakeUp();
    }

    @Override
    public void setRotation(Quaternion rot) {
        if (/*(q - mTargetMotion.q).magnitudeSquared()*/Vector4f.distanceSquare(rot, mTargetMotion.q) == 0.0f)
            return;

        mTargetMotion.q.set(rot);
        wakeUp();
    }

    @Override
    public ReadableVector3f getTranslation() {
        return mTargetMotion.p;
    }

    @Override
    public Quaternion getRotation() {
        return mTargetMotion.q;
    }

    @Override
    public void clearInertia() {
        mCurrentMotion.set(mTargetMotion);
        mLinearVelocity.set(0,0,0);
        mAngularVelocity.set(0,0,0);

        wakeUp();
    }

    @Override
    public void teleport(ReadableVector3f delta) {
//        mCurrentMotion.p += delta;
//        mTargetMotion.p += delta;
        Vector3f.add(mCurrentMotion.p, delta, mCurrentMotion.p);
        Vector3f.add(mTargetMotion.p, delta, mTargetMotion.p);
    }

    @Override
    public float getPreviousIterationDt() {
        return mPrevIterDt;
    }

    @Override
    public void setGravity(ReadableVector3f gravity) {
//        physx::PxVec3 value = gravity;
        if (mGravity.equals(gravity))
            return;

        mGravity.set(gravity);
        wakeUp();
    }

    protected static float safeLog2(float x)
    {
//        NV_CLOTH_ASSERT_WITH_MESSAGE("safeLog2",x >= 0.0f);
        return x > 0 ? (float) (Math.log(x) / 0.693147180559945309417) : -FLT_MAX_EXP;
    }

    private static void safeLog2( Vector3f v, Vector3f out)
    {
        out.set( safeLog2(v.x), safeLog2(v.y), safeLog2(v.z) );
    }

    protected static float safeExp2(float x)
    {
        if (x <= -FLT_MAX_EXP)
            return 0.0f;
        else
//            return physx::shdfnd::exp2(x);
            return (float) Math.exp((x * 0.693147180559945309417));
    }

    private static void safeExp2(Vector3f v, Vector3f out)
    {
        out.set( safeExp2(v.x), safeExp2(v.y), safeExp2(v.z) );
    }

    @Override
    public ReadableVector3f getGravity() {
        return mGravity;
    }

    @Override
    public void setDamping(ReadableVector3f damping) {
        final Vector3f value = Vector3f.sub(1.f, damping, null);

        safeLog2(value, value);
        if (value.equals(mLogDamping))
            return;

        mLogDamping.set(value);
        wakeUp();
    }

    @Override
    public ReadableVector3f getDamping() {
//        return physx::PxVec3(1.f) - safeExp2(mLogDamping);
        Vector3f result = new Vector3f();
        safeExp2(mLogDamping, result);
        return Vector3f.sub(1.f, result, result);
    }

    @Override
    public void setLinearDrag(ReadableVector3f drag) {
//        physx::PxVec3 value = safeLog2(physx::PxVec3(1.f) - drag);
        final Vector3f value = new Vector3f();
        Vector3f.sub(1.f, drag, value);
        safeLog2(value, value);
        if (value.equals(mLinearLogDrag))
            return;

        mLinearLogDrag.set(value);
        wakeUp();
    }

    @Override
    public ReadableVector3f getLinearDrag() {
//        return physx::PxVec3(1.f) - safeExp2(mLinearLogDrag);
        Vector3f result = new Vector3f();
        safeExp2(mLinearLogDrag, result);
        return Vector3f.sub(1.f, result, result);
    }

    @Override
    public void setAngularDrag(ReadableVector3f drag) {
//        physx::PxVec3 value = safeLog2(physx::PxVec3(1.f) - drag);
        final Vector3f value = Vector3f.sub(1.f,drag, null);
        safeLog2(value, value);
        if (value.equals(mAngularLogDrag))
            return;

        mAngularLogDrag.set(value);
        wakeUp();
    }

    @Override
    public ReadableVector3f getAngularDrag() {
//        return physx::PxVec3(1.f) - safeExp2(mAngularLogDrag);
        final Vector3f result = new Vector3f();
        safeExp2(mAngularLogDrag, result);
        return Vector3f.sub(1.f, result, result);
    }

    @Override
    public void setLinearInertia(ReadableVector3f inertia) {
//        physx::PxVec3 value = inertia;
        if (mLinearInertia.equals(inertia))
            return;

        mLinearInertia.set(inertia);
        wakeUp();
    }

    @Override
    public ReadableVector3f getLinearInertia() {
        return mLinearInertia;
    }

    @Override
    public void setAngularInertia(ReadableVector3f inertia) {
        if (mAngularInertia.equals(inertia))
            return;

        mAngularInertia.set(inertia);
        wakeUp();
    }

    @Override
    public ReadableVector3f getAngularInertia() {
        return mAngularInertia;
    }

    @Override
    public void setCentrifugalInertia(ReadableVector3f inertia) {
//        physx::PxVec3 value = inertia;
        if (mCentrifugalInertia.equals(inertia))
            return;

        mCentrifugalInertia.set(inertia);
        wakeUp();
    }

    @Override
    public ReadableVector3f getCentrifugalInertia() {
        return mCentrifugalInertia;
    }

    protected abstract void makeClothCostDirty();

    @Override
    public void setSolverFrequency(float frequency) {
        if (frequency == mSolverFrequency)
            return;

        mSolverFrequency = frequency;
//        getChildCloth().mClothCostDirty = true;
        makeClothCostDirty();
        mIterDtAvg.reset();
        wakeUp();
    }

    @Override
    public float getSolverFrequency() {
        return mSolverFrequency;
    }

    @Override
    public void setStiffnessFrequency(float frequency) {
        if (frequency == mStiffnessFrequency)
            return;

        mStiffnessFrequency = frequency;
        wakeUp();
    }

    @Override
    public float getStiffnessFrequency() {
        return mStiffnessFrequency;
    }

    @Override
    public void setAcceleationFilterWidth(int filterWidth) {
        mIterDtAvg.resize(filterWidth);
    }

    @Override
    public int getAccelerationFilterWidth() {
        return mIterDtAvg.size();
    }

    protected abstract void notifyChanged();

    @Override
    public void setWindVelocity(ReadableVector3f velocity) {
        if (mWind.equals(velocity))
            return;

        mWind.set(velocity);
        notifyChanged();
        wakeUp();
    }

    @Override
    public ReadableVector3f getWindVelocity() {
        return mWind;
    }

    @Override
    public void setDragCoefficient(float coefficient) {
        assert (coefficient < 1.f);

        float value = safeLog2(1.f - coefficient);
        if (value == mDragLogCoefficient)
            return;

        mDragLogCoefficient = value;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getDragCoefficient() {
        return 1.f - safeExp2(mDragLogCoefficient);
    }

    @Override
    public void setLiftCoefficient(float coefficient) {
        assert (coefficient < 1.f);

        float value = safeLog2(1.f - coefficient);
        if (value == mLiftLogCoefficient)
            return;

        mLiftLogCoefficient = value;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getLiftCoefficient() {
        return 1.f - safeExp2(mLiftLogCoefficient);
    }

    @Override
    public void setFluidDensity(float fluidDensity) {
        assert (fluidDensity < 0.f);
        if (fluidDensity == mFluidDensity)
            return;

        mFluidDensity = fluidDensity;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getFluidDensity() {
        return mFluidDensity;
    }

    @Override
    public void setSleepThreshold(float threshold) {
        if (threshold == mSleepThreshold)
            return;

        mSleepThreshold = threshold;
        notifyChanged();
        wakeUp();
    }

    @Override
    public float getSleepThreshold() {
        return mSleepThreshold;
    }

    @Override
    public void setSleepTestInterval(int interval) {
        if (interval == mSleepTestInterval)
            return;

        mSleepTestInterval = interval;
        notifyChanged();
        wakeUp();
    }

    @Override
    public int getSleepTestInterval() {
        return mSleepTestInterval;
    }

    @Override
    public void setSleepAfterCount(int afterCount) {
        if (afterCount == mSleepAfterCount)
            return;

        mSleepAfterCount = afterCount;
        notifyChanged();
        wakeUp();
    }

    @Override
    public int getSleepAfterCount() {
        return mSleepAfterCount;
    }

    @Override
    public int getSleepPassCount() {
        return mSleepPassCounter;
    }

    @Override
    public boolean isAsleep() {
        return isSleeping();
    }

    public boolean isSleeping()
    {
        return mSleepPassCounter >= mSleepAfterCount;
    }

    @Override
    public void putToSleep() {
        mSleepPassCounter = mSleepAfterCount;
    }

    @Override
    public void wakeUp() {
        mSleepPassCounter = 0;
    }
}
