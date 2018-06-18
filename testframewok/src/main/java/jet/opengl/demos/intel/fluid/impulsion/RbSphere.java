package jet.opengl.demos.intel.fluid.impulsion;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.ReadableVector3f;

import jet.opengl.demos.intel.fluid.collision.SphereShape;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2018/4/25 0025.
 */

public class RbSphere extends PhysicalObject {
    private final RigidBody    mRigidBody = new RigidBody();
    private final SphereShape  mSphereShape    =new SphereShape();

    public RbSphere(ReadableVector3f vPos , ReadableVector3f vVelocity, float fMass , float fRadius )
//            : PhysicalObject( & mRigidBody , & mSphereShape )
//            , mRigidBody( vPos , vVelocity , fMass )
    {
        super(null, null);
        mBody = mRigidBody;
        mCollisionShape = mSphereShape;
        // Moments of inertia for a sphere are 2 M R^2 / 5.
        // So the inverse of that is 5/(2 M R^2)
        // which is 5 (1/M) / (2 R^2) = 2.5 (1/M) / R^2
//        mBody.setInverseInertiaTensor( Mat33_xIdentity * 2.5f * mBody.getReciprocalMass() / ( fRadius * fRadius ) ) ;
        Matrix3f inertiaTensor = new Matrix3f();
        inertiaTensor.m00 = inertiaTensor.m11 = inertiaTensor.m22 = 2.5f * mBody.getReciprocalMass() / ( fRadius * fRadius );
        mBody.setInverseInertiaTensor(inertiaTensor);

        mSphereShape.setBoundingSphereRadius( fRadius ) ;

        getThermalProperties().setTemperature( PhysicalObject.sAmbientTemperature ) ;
        getThermalProperties().setThermalConductivity( 500.0f ) ;
        getThermalProperties().setOneOverHeatCapacity( 0.00001f );

        mVolume = 4.0f * Numeric.PI * fRadius * fRadius * fRadius / 3.0f ;
    }

    public RbSphere set( RbSphere that )
    {
        // Assign parent class members.
        mBody               =  mRigidBody ;
        mCollisionShape     =  mSphereShape ;
        mFrictionProperties .set(that.getFrictionProperties());
        mThermalProperties  .set(that.getThermalProperties());

        // Copy members of this class.
        mRigidBody          .set(that.mRigidBody);
        mSphereShape        .set(that.mSphereShape);
        mVolume             = that.mVolume ;
        return this ;
    }

    public RbSphere( RbSphere that )
            /*: PhysicalObject( & mRigidBody , & mSphereShape , that.mFrictionProperties , that.mThermalProperties )
            , mRigidBody( that.mRigidBody )
            , mSphereShape( that.mSphereShape )*/
    {
        super(null, null);
        set(that);
    }
}
