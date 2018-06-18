package jet.opengl.demos.intel.fluid.impulsion;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.List;

/**
 * Rigid body for physics simulation.<p></p>
 * Created by Administrator on 2018/4/7 0007.
 */

public class RigidBody {
    protected final Vector3f mPosition = new Vector3f()           ;   ///< Position in world-space of center-of-mass.
    protected final Quaternion mOrientation = new Quaternion()        ;   ///< Orientation in world-space.  Assumes body at zero rotation has diagonal inertia tensor.
    protected final Vector3f    mMomentum  = new Vector3f()          ;   ///< Linear momentum in world units.
    protected final Vector3f    mAngularMomentum = new Vector3f()    ;   ///< Angular momentum in world units.
    protected float             mReciprocalMass =1     ;   ///< One over mass.  Converts momentum to linear velocity.
    protected final Matrix3f mInvInertiaTensor=new Matrix3f()   ;   ///< Inverse of inertia tensor.  Converts angular momentum to angular velocity.
    protected final Vector3f    mNetForces = new Vector3f()          ;   ///< Net forces to apply during Update.  Accumulator.
    protected final Vector3f    mNetTorque = new Vector3f()          ;   ///< Net torque to apply during Update.  Accumulator.
    protected final Vector3f    mLinearVelocity = new Vector3f()     ;   ///< Linear velocity, auxiliary variable subordinate to momentum.
    protected final Vector3f    mAngularVelocity = new Vector3f()    ;   ///< Angular velocity, auxiliary variable subordinate to angular momentum.

    public RigidBody(){}

    public RigidBody(ReadableVector3f position , ReadableVector3f velocity ,  float mass ) {
        mPosition.set(position);
        mLinearVelocity.set(velocity);
        mReciprocalMass = 1.0f/mass;
    }

    public void setPosition( ReadableVector3f pos ) { mPosition.set(pos);}
    public ReadableVector3f getPosition() { return mPosition ; }

    public void setOrientation( Quaternion orientation ) { mOrientation.set(orientation);}
    public Quaternion getOrientation() { return mOrientation ; }

    public void setMomentum( ReadableVector3f momentum ) { mMomentum.set(momentum);}
    public ReadableVector3f getMomentum() { return mMomentum ; }

    public void setAngularMomentum( ReadableVector3f angularMomentum )
    {
//        ASSERT( ! IsNan( angularMomentum ) ) ;
        mAngularMomentum.set(angularMomentum);
    }

    public ReadableVector3f getAngularMomentum(){ return mAngularMomentum ; }

    public void setMassAndInertiaTensor( float mass , Matrix3f inertiaTensor ){
        mReciprocalMass = 1.0f / mass ;
//        mInvInertiaTensor = inertiaTensor.Inverse() ;
        Matrix3f.invert(inertiaTensor, mInvInertiaTensor);
    }

    public void setInverseInertiaTensor( Matrix3f inverseInertiaTensor ) { mInvInertiaTensor.load(inverseInertiaTensor);}

    public float getReciprocalMass()  { return mReciprocalMass ; }
    public float getMass()  { return 1.0f / mReciprocalMass ; }

    /** Apply a force to this rigid body along a line through its center of mass.

     */
    public void applyBodyForce( ReadableVector3f force )
    {
//        mNetForces += force ;
        Vector3f.add(mNetForces, force, mNetForces);
    }

    /** Apply a torque to this rigid body.

     \see ApplyTorqueAt, ApplyForce.
     */
    public void applyTorque( ReadableVector3f torque )
    {
//        mNetTorque += torque ;
        Vector3f.add(mNetTorque, torque, mNetTorque);
    }

    public Vector3f torqueOfForceAt(ReadableVector3f force , ReadableVector3f position )
    {
//                const Vec3 positionRelativeToBody = position - GetPosition() ;
//                const Vec3 torque                 = positionRelativeToBody ^ force ;
        final Vector3f positionRelativeToBody = Vector3f.sub(position, getPosition(), null);
        final Vector3f torque =  Vector3f.cross(positionRelativeToBody, force, positionRelativeToBody);
        return torque ;
    }


    /** Apply a force at a given position to this rigid body.

     \see ApplyImpulseAt, ApplyForce, ApplyTorque.
     */
    public void applyForceAt( ReadableVector3f force , ReadableVector3f position )
    {
        applyBodyForce( force ) ;
        applyTorque( torqueOfForceAt( force , position ) ) ;
    }


    /** Apply an impulse to this rigid body.

     \see ApplyForce, ApplyImpulsiveTorque.
     */
    public void applyImpulse( ReadableVector3f impulse )
    {
//        mMomentum       += impulse ;                          // Apply impulse.
        Vector3f.add(mMomentum, impulse, mMomentum);
//        mLinearVelocity  = mReciprocalMass * GetMomentum() ;  // Update linear velocity accordingly.
        Vector3f.scale(getMomentum(), mReciprocalMass, mLinearVelocity);
    }


    /** Apply an impulsive torque to this rigid body.

     \see ApplyTorque, ApplyImpulse.
     */
    public void applyImpulsiveTorque( ReadableVector3f impulsiveTorque )
    {
//        mAngularMomentum += impulsiveTorque ;                          // Apply impulsive torque.
        Vector3f.add(mAngularMomentum, impulsiveTorque, mAngularMomentum);
        assert ( ! mAngularMomentum.isNaN()) ;
//        mAngularVelocity  = mInvInertiaTensor * GetAngularMomentum() ; // Update angular velocity accordingly.
        Matrix3f.transform(mInvInertiaTensor, getAngularMomentum(), mAngularVelocity);
    }


    /** Apply an impulse at a given position to this rigid body.

     \see ApplyForceAt, ApplyImpulsiveTorque.
     */
    public void applyImpulseAt(ReadableVector3f impulse , ReadableVector3f position )
    {
//        mMomentum      += impulse ;                     // Apply impulse.
        Vector3f.add(mMomentum, impulse, mMomentum);
//        mLinearVelocity = mReciprocalMass * mMomentum ; // Update linear velocity accordingly.
        Vector3f.scale(mMomentum, mReciprocalMass, mLinearVelocity);
        applyImpulsiveTorque( torqueOfForceAt( impulse , position ) ) ;
    }



    /// Set linear velocity and momentum given a linear velocity.
    public void setVelocity( ReadableVector3f velocity )
    {
        mLinearVelocity.set(velocity);
//        mMomentum       = velocity * GetMass() ;
        Vector3f.scale(velocity, getMass(), mMomentum);
    }

    public ReadableVector3f getVelocity() { return mLinearVelocity ; }

    /// Set angular velocity and momentum given an angular velocity.
    public void setAngularVelocity( ReadableVector3f angularVelocity )
    {
        mAngularVelocity    .set(angularVelocity); ;
//        mAngularMomentum    = GetInertiaTensor().transform( angularVelocity ) ;
        Matrix3f.transform(getInertiaTensor(), angularVelocity, mAngularMomentum);
//        ASSERT( ! IsNan( mAngularMomentum ) ) ;
        assert (!mAngularMomentum.isNaN());
    }

    public ReadableVector3f getAngularVelocity() { return mAngularVelocity ; }


    public void update( float timeStep ){
        // Update linear quantities.
        final ReadableVector3f momentumBefore = getMomentum() ;
//        mMomentum += mNetForces * timeStep ;
        Vector3f.linear(mMomentum, mNetForces, timeStep, mMomentum);

//        const Vec3 velocityAvg = 0.5f * ( momentumBefore + getMomentum() ) * mReciprocalMass ;
//        mPosition += timeStep * velocityAvg ;

        Vector3f velocityAvg =  Vector3f.linear(momentumBefore, mReciprocalMass*0.5f, getMomentum(), mReciprocalMass*0.5f, null);
        Vector3f.linear(mPosition, velocityAvg, timeStep, mPosition);

        // Update angular quantities.
        ReadableVector3f  angMomBefore    = getAngularMomentum() ;
        assert ( ! angMomBefore.isNaN() ) ;
//        mAngularMomentum += mNetTorque * timeStep ;
        Vector3f.linear(mAngularMomentum, mNetTorque, timeStep, mAngularMomentum);
        assert ( ! getAngularMomentum().isNaN() );
//        const Vec3  angVelAverage   = mInvInertiaTensor * ( 0.5f * ( getAngularMomentum() + angMomBefore ) ) ;
        Vector3f angVelAverage = Vector3f.mix(getAngularMomentum(), angMomBefore, 0.5f, velocityAvg);
        Matrix3f.transform(mInvInertiaTensor, angVelAverage, angVelAverage);

        /*final Matrix3f timeDerivative  = crossProductMatrix( angVelAverage ) ; todo i dont't kown how to handle this by quatertion.
        const Mat33 angVelOperator  = timeDerivative * getOrientation() ;
        mOrientation = mOrientation + timeStep * angVelOperator ;
        mOrientation.Orthonormalize() ;*/

        // Update auxiliary values.
//        mLinearVelocity  = getMomentum() * mReciprocalMass ;
        Vector3f.scale(getMomentum(), mReciprocalMass, mLinearVelocity);
//        mAngularVelocity = mInvInertiaTensor * GetAngularMomentum() ;
        Matrix3f.transform(mInvInertiaTensor, getAngularMomentum(), mAngularVelocity);

        // Zero out forces and torques.
//        mNetForces = mNetTorque = Vec3( 0.0f , 0.0f , 0.0f ) ;
        mNetForces.set(0,0,0);
        mNetTorque.set(0,0,0);
    }

    private Matrix3f getInertiaTensor()
    {
        return Matrix3f.invert(mInvInertiaTensor, null);
    }

    public void set(RigidBody ohs){
        mReciprocalMass = ohs.mReciprocalMass;
        mPosition.set(ohs.mPosition);
        mOrientation.set(ohs.mOrientation);
        mMomentum.set(ohs.mMomentum);
        mAngularMomentum.set(ohs.mAngularMomentum);
        mInvInertiaTensor.load(ohs.mInvInertiaTensor);
        mNetForces.set(ohs.mNetForces);
        mNetTorque.set(ohs.mNetTorque);
        mLinearVelocity.set(ohs.mLinearVelocity);
        mAngularVelocity.set(ohs.mAngularVelocity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RigidBody rigidBody = (RigidBody) o;

        if (Float.compare(rigidBody.mReciprocalMass, mReciprocalMass) != 0) return false;
        if (mPosition != null ? !mPosition.equals(rigidBody.mPosition) : rigidBody.mPosition != null)
            return false;
        if (mOrientation != null ? !mOrientation.equals(rigidBody.mOrientation) : rigidBody.mOrientation != null)
            return false;
        if (mMomentum != null ? !mMomentum.equals(rigidBody.mMomentum) : rigidBody.mMomentum != null)
            return false;
        if (mAngularMomentum != null ? !mAngularMomentum.equals(rigidBody.mAngularMomentum) : rigidBody.mAngularMomentum != null)
            return false;
        if (mInvInertiaTensor != null ? !mInvInertiaTensor.equals(rigidBody.mInvInertiaTensor) : rigidBody.mInvInertiaTensor != null)
            return false;
        if (mNetForces != null ? !mNetForces.equals(rigidBody.mNetForces) : rigidBody.mNetForces != null)
            return false;
        if (mNetTorque != null ? !mNetTorque.equals(rigidBody.mNetTorque) : rigidBody.mNetTorque != null)
            return false;
        if (mLinearVelocity != null ? !mLinearVelocity.equals(rigidBody.mLinearVelocity) : rigidBody.mLinearVelocity != null)
            return false;
        return mAngularVelocity != null ? mAngularVelocity.equals(rigidBody.mAngularVelocity) : rigidBody.mAngularVelocity == null;
    }

    @Override
    public int hashCode() {
        int result = mPosition != null ? mPosition.hashCode() : 0;
        result = 31 * result + (mOrientation != null ? mOrientation.hashCode() : 0);
        result = 31 * result + (mMomentum != null ? mMomentum.hashCode() : 0);
        result = 31 * result + (mAngularMomentum != null ? mAngularMomentum.hashCode() : 0);
        result = 31 * result + (mReciprocalMass != +0.0f ? Float.floatToIntBits(mReciprocalMass) : 0);
        result = 31 * result + (mInvInertiaTensor != null ? mInvInertiaTensor.hashCode() : 0);
        result = 31 * result + (mNetForces != null ? mNetForces.hashCode() : 0);
        result = 31 * result + (mNetTorque != null ? mNetTorque.hashCode() : 0);
        result = 31 * result + (mLinearVelocity != null ? mLinearVelocity.hashCode() : 0);
        result = 31 * result + (mAngularVelocity != null ? mAngularVelocity.hashCode() : 0);
        return result;
    }

    /** Return skew-symmetric matrix that represents axisAngle crossed with the thing on its right.

     \param axisAngle    Vector representing a rotation about an axis,
     where the axis is the unit vector along axisAngle
     and the angle is the magnitude of axisAngle.

     \note See Rodrigues' rotation formula.

     \note   When omega is the time-derivative of some orientation theta,
     then the cross-product of that time-derivative with that
     orientation acts like a time-derivative of the orientation.
     That is, if theta is an orientation and omega is its time
     derivative, then it is also the case that
     d theta / dt = omega x theta .
     In this situation, both "theta" and "omega" represent
     vectors.  In the situation where "theta" represents a
     matrix, the operation of "omega x" would also need to be a
     matrix multiplication.  That is what this routine does.
     So, if THETA is an orientation matrix and omega is its time
     derivative (as a vector), then the time-derivative of THETA,
     as a matrix, is CrossProductMatrix( omega ) * THETA.
     */
    static Matrix3f crossProductMatrix( ReadableVector3f axisAngle )
    {
        /*return Mat33(   Vec3( 0.0f          ,  axisAngle.z , -axisAngle.y )
                ,   Vec3( -axisAngle.z  , 0.0f         ,  axisAngle.x )
                ,   Vec3(  axisAngle.y  , -axisAngle.x , 0.0f         ) ) ;*/

        return new Matrix3f(0.0f          ,  axisAngle.getZ() , -axisAngle.getY(),
                -axisAngle.getZ()  , 0.0f         ,  axisAngle.getX(),
                axisAngle.getY()  , -axisAngle.getX() , 0.0f);
    }

    /** Update rigid bodies.
     */
    public static void updateSystem(List<RigidBody> rigidBodies , float timeStep , int uFrame  )
    {
        final int numBodies = rigidBodies.size() ;

        for( int uBody = 0 ; uBody < numBodies ; ++ uBody )
        {   // For each body in the simulation...
            RigidBody rBody = rigidBodies.get(uBody) ;
            // Update body physical state
            rBody.update( timeStep ) ;
        }
    }


    public static Vector3f computeAngularMomentum( List<RigidBody> rigidBodies )
    {
        Vector3f         angMom = new Vector3f() ;
        final int numBodies = rigidBodies.size() ;

        for( int uBody = 0 ; uBody < numBodies ; ++ uBody )
        {   // For each body in the simulation...
            RigidBody rBody = rigidBodies.get(uBody) ;
            // Update body physical state
//            angMom += rBody.GetAngularMomentum() ;
            Vector3f.add(angMom, rBody.getAngularMomentum(), angMom);
        }
        return angMom ;
    }
}
