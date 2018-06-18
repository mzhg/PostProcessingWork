package jet.opengl.demos.intel.fluid.impulsion;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.fluid.collision.ConvexPolytope;

/**
 * Created by Administrator on 2018/4/7 0007.
 */

public class RbBox extends PhysicalObject {

    private final RigidBody        mRigidBody   = new RigidBody();
    private final ConvexPolytope   mPolytope    = new ConvexPolytope();
    private final Vector3f   mDimensions = new Vector3f();
    private boolean          mIsHole     ;   ///< Whether this box is a hole.

    public RbBox(ReadableVector3f vPos , ReadableVector3f vVelocity , float fMass , ReadableVector3f dimensions , boolean isHole /*= false*/ )
//            : PhysicalObject( & mRigidBody , & mPolytope )
//            , mRigidBody( vPos , vVelocity , fMass )
//            , mPolytope( isHole )
//            , mDimensions( dimensions )
//            , mIsHole( isHole )
    {
        super(null, null);
        mBody = mRigidBody;
        mCollisionShape = mPolytope;

        Vector3f dims2 = new Vector3f( (float)Math.pow( dimensions.getX(),2 ) , (float)Math.pow( dimensions.getY(),2 ) , (float)Math.pow( dimensions.getZ(),2 ) ) ;
        Matrix3f inertiaTensor = new Matrix3f() ;
        inertiaTensor.m00 = 1.0f/(fMass * ( dims2.y + dims2.z ) / 12.0f) ;
        inertiaTensor.m11 = 1.0f/(fMass * ( dims2.x + dims2.z ) / 12.0f) ;
        inertiaTensor.m22 = 1.0f/(fMass * ( dims2.x + dims2.y ) / 12.0f) ;
        getBody().setInverseInertiaTensor( inertiaTensor ) ;

//        ConvexPolytope_MakeBox( mPolytope , dimensions ) ;
        ConvexPolytope.makeBox(mPolytope , dimensions);

        getThermalProperties().setTemperature( PhysicalObject.sAmbientTemperature ) ;
        getThermalProperties().setThermalConductivity( 500.0f ) ;
        getThermalProperties().setOneOverHeatCapacity( 0.00001f );
    }

    public void set( RbBox that )
    {
        // Assign parent class members.
        mBody               =  mRigidBody ;
        mCollisionShape     =  mPolytope ; // TODO: FIXME
        mFrictionProperties .set(that.getFrictionProperties());
        mThermalProperties  .set(that.getThermalProperties());
        mVolume             = that.mVolume ;

        // Copy members of this class.
        mRigidBody          .set(that.mRigidBody); ;
        mPolytope           .set(that.mPolytope) ;
        mDimensions         .set(that.mDimensions);
        mIsHole             = that.mIsHole ;

//        return * this ;
    }

    public RbBox(RbBox that )
           /* : PhysicalObject( & mRigidBody , & mPolytope , that.mFrictionProperties , that.mThermalProperties )
            , mRigidBody( that.mRigidBody )
            , mPolytope( that.mPolytope )
            , mDimensions( that.mDimensions )
            , mIsHole( that.mIsHole )*/
    {
        super(null, null, that.mFrictionProperties, that.mThermalProperties);
        set(that);
    }
}
