package jet.opengl.demos.intel.fluid.impulsion;

/**
 * Friction properties of a physical object.<p></p>

 Technically, friction is a property of pairs of objects in contact, not of
 individual objects.  This is a simplification.<p></p>
 * Created by Administrator on 2018/4/7 0007.
 */

public class FrictionProperties {
    public float   mSlidingFriction    ;
    public float   mStaticFriction     ;
    public float   mRollingFriction    ;
    public float   mRestitution        = 1.f;

    public void set(FrictionProperties ohs){
        mSlidingFriction = ohs.mSlidingFriction;
        mStaticFriction = ohs.mStaticFriction;
        mRollingFriction = ohs.mRollingFriction;
        mRestitution = ohs.mRestitution;
    }
}
