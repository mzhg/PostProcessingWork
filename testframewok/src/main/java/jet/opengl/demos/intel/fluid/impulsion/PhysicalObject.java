package jet.opengl.demos.intel.fluid.impulsion;

import org.lwjgl.util.vector.Vector3f;

import java.util.List;

import jet.opengl.demos.intel.fluid.collision.ShapeBase;

/**
 * Created by Administrator on 2018/4/7 0007.
 */

public class PhysicalObject {
    public static final float sAmbientTemperature  = 0;

    protected RigidBody   mBody               ;
    protected ShapeBase   mCollisionShape     ;
    protected final FrictionProperties      mFrictionProperties = new FrictionProperties();
    protected final ThermalProperties       mThermalProperties  = new ThermalProperties();
    protected float                         mVolume;

    /** Construct a physical object.
     */
    public PhysicalObject(RigidBody body , ShapeBase collisionShape )
//            : mBody( body )
//            , mCollisionShape( collisionShape )
//            , mVolume( 0.0f )
    {
        mBody = body;
        mCollisionShape = collisionShape;
    }



    public PhysicalObject(RigidBody body ,ShapeBase collisionShape , FrictionProperties frictionProperties , ThermalProperties thermalProperties )
//            : mBody( body )
//            , mCollisionShape( collisionShape )
//            , mFrictionProperties( FrictionProperties )
//            , mThermalProperties( thermalProperties )
//            , mVolume( 0.0f )
    {
        mBody = body;
        mCollisionShape = collisionShape;
        mFrictionProperties.set(frictionProperties);
        mThermalProperties.set(thermalProperties);
    }

    public RigidBody         getBody()                       { return mBody ; }

    public ShapeBase         getCollisionShape()             { return mCollisionShape ; }
    public void setCollisionShape( ShapeBase collisionShape ){
        assert ( null == mCollisionShape ) ; // Not allowed to change collision shape after it was set.

        mCollisionShape = collisionShape ;
    }

    public FrictionProperties getFrictionProperties()   { return mFrictionProperties ; }
    public ThermalProperties  getThermalProperties()    { return mThermalProperties ; }

    public float getVolume() { return mVolume ; }

    /** Update rigid bodies associated with the given physical objects.
     */
    public static void updateSystem(List<PhysicalObject> physicalObjects , float timeStep , int uFrame )
    {
//        PERF_BLOCK( PhysicalObject_UpdateSystem ) ;

        final int numPhysObjs = physicalObjects.size() ;

        for( int idxPhysObj = 0 ; idxPhysObj < numPhysObjs ; ++ idxPhysObj )
        {   // For each body in the simulation...
            RigidBody rBody = physicalObjects.get(idxPhysObj).getBody() ;
            // Update body physical state
            rBody.update( timeStep ) ;
        }
    }

    /** Compute total angular momentum of all rigid bodies associated with the given physical objects.
     */
    public static Vector3f computeAngularMomentum( List<PhysicalObject> physicalObjects )
    {
//        PERF_BLOCK( PhysicalObject_ComputeAngularMomentum ) ;

        Vector3f angMom = new Vector3f( 0.0f , 0.0f , 0.0f ) ;
        final int numPhysObjs = physicalObjects.size() ;

        for( int idxPhysObj = 0 ; idxPhysObj < numPhysObjs ; ++ idxPhysObj )
        {   // For each body in the simulation...
            RigidBody rBody = physicalObjects.get(idxPhysObj).getBody() ;
            // Update body physical state
//            angMom += rBody.getAngularMomentum() ;
            Vector3f.add(angMom, rBody.getAngularMomentum(), angMom);
        }
        return angMom ;
    }
}
