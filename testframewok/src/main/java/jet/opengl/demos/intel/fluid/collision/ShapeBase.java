package jet.opengl.demos.intel.fluid.collision;

/**
 * Created by Administrator on 2018/4/7 0007.
 */

public class ShapeBase {
    private int     mShapeType              ;   ///< Type identifier for this shape.  Crude form of RTTI.
    private float   mBoundingSphereRadius   ;   ///< Radius of sphere that contains this shape. Used for broad phase collision detection.
    private float   mParity                 ;   ///< Sign to apply to distances and normals. +1 for solid, -1 for hole.

    public ShapeBase( int shapeType , boolean isHole /*= false */)
//            : mShapeType( shapeType )
//            , mBoundingSphereRadius( -1.0f )
//            , mParity( isHole ? -1.0f : 1.0f )
    {
        mShapeType = shapeType;
        mBoundingSphereRadius = -1;
        mParity = isHole ? -1.0f : 1.0f;
    }

    public ShapeBase( int shapeType , float boundingSphereRadius )
//            : mShapeType( shapeType )
//            , mBoundingSphereRadius( boundingSphereRadius )
//            , mParity( 1.0f )
    {
        mShapeType = shapeType;
        mBoundingSphereRadius = boundingSphereRadius;
        mParity = 1.0f;
    }

    public void set(ShapeBase ohs){
        mShapeType = ohs.mShapeType;
        mBoundingSphereRadius = ohs.mBoundingSphereRadius;
        mParity = ohs.mParity;
    }

    public int getShapeType()
    {
        return mShapeType ;
    }

    public void setBoundingSphereRadius( float boundingSphereRadius )
    {
        mBoundingSphereRadius = boundingSphereRadius ;
    }


    /// Return whether this shape is a hole.  True means hole, false means solid.
    public boolean isHole()
    {
        return mParity < 0.0f ;
    }

    protected float getParity() {
        return mParity ;
    }
}
