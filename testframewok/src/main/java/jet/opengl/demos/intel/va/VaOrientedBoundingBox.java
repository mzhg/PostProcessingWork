package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2017/11/19 0019.
 */

public class VaOrientedBoundingBox {
    public final Vector3f Center = new Vector3f();
    public final Vector3f Extents = new Vector3f();    // aka half-size
    public final Matrix3f Axis = new Matrix3f();

    public VaOrientedBoundingBox( ) { }
    public VaOrientedBoundingBox(ReadableVector3f center, ReadableVector3f halfSize, Matrix3f axis ) //: Center(center), Extents( halfSize ), Axis(axis) { }
    {
        Center.set(center);
        Extents.set(halfSize);
        Axis.load(axis);
    }
    public VaOrientedBoundingBox( VaBoundingBox box, Matrix4f transform ) { FromAABBAndTransform( box, transform ); }

    public void set(VaOrientedBoundingBox ohs){
        Center.set(ohs.Center);
        Axis.load(ohs.Axis);
        Extents.set(ohs.Extents);
    }

    public Vector3f                        Min( )  { return Vector3f.sub(Center ,Extents,null); }
    public Vector3f                        Max( )  { return Vector3f.add(Center ,Extents,null); }

    public VaOrientedBoundingBox    FromAABBAndTransform( VaBoundingBox box, Matrix4f transform ){
        VaOrientedBoundingBox ret = this;

        /*ret.Extents = box.Size * 0.5f;
        ret.Center  = box.Min + ret.Extents;*/
        Vector3f.scale(box.Size, 0.5f, ret.Center);
        Vector3f.add(box.Min, ret.Extents, ret.Center);

        /*ret.Center  = vaVector3::TransformCoord( ret.Center, transform );
        ret.Axis    = transform.GetRotationMatrix3x3( );*/
        Matrix4f.transformCoord(transform, ret.Center, ret.Center);
        ret.Axis.load(transform);

        return ret;
    }

    public void ToAABBAndTransform( VaBoundingBox outBox, Matrix4f outTransform ) {
        if(outTransform != null) {
            outTransform.load(Axis);
            outTransform.m30 = Center.x;
            outTransform.m31 = Center.y;
            outTransform.m32 = Center.z;
            outTransform.m33 = 1;
        }

        if(outBox != null){
            outBox.Min.set(Extents).scale(-1);
            outBox.Size.set(Extents).scale(2);
        }
    }

    /**
     * 0 means intersect, -1 means it's wholly in the negative halfspace of the plane, 1 means it's in the positive half-space of the plane<br>
     * <b>This method may be not correcting, need viryfiy it.</b>
     * @return
     */
    public int                             IntersectPlane(ReadableVector4f plane ){
        // From Christer Ericson "Real Time Collision Detection" page 163

        final Vector3f tmp = CacheBuffer.getCachedVec3();
        // Compute the projection interval radius of b onto L(t) = b.c + t * p.n
        try{
            float r =   Extents.x * Math.abs( Vector3f.dot( plane/*.normal*/, (ReadableVector3f) Axis.getColumn(0, tmp) ) ) +
                    Extents.y * Math.abs( Vector3f.dot( plane/*.normal*/, (ReadableVector3f) Axis.getColumn(1, tmp) ) ) +
                    Extents.z * Math.abs( Vector3f.dot( plane/*.normal*/, (ReadableVector3f) Axis.getColumn(2, tmp) ) );

            // Compute distance of box center from plane
            float s = Vector3f.dot( plane/*.normal*/, Center ) + plane.getW();

            // Intersection occurs when distance s falls within [-r,+r] interval
            if( Math.abs( s ) <= r )
            return 0;

            return (s < r)?(-1):(1);
        }finally {
            CacheBuffer.free(tmp);
        }
    }

    public boolean IntersectFrustum( ReadableVector4f planes[], int planeCount ){
        int k = 0;
        for( int i = 0; i < planeCount; i++ )
        {
            int rk = IntersectPlane( planes[i] );

            // if it's completely out of any plane, bail out
            if( rk < 0 )
                return false;
        }
        // otherwise, we're in!
        return true;
    }

    public Vector3f RandomPointInside( /*vaRandom & randomGeneratorToUse = vaRandom::Singleton*/ Vector3f result){
        result.x = Numeric.random(-1,+1) * Extents.x;
        result.y = Numeric.random(-1,+1) * Extents.y;
        result.z = Numeric.random(-1,+1) * Extents.z;

        final Matrix4f transform = CacheBuffer.getCachedMatrix();
        try {
            ToAABBAndTransform(null, transform);
            Matrix4f.transformCoord(transform, result, result);
            return result;
        }finally {
            CacheBuffer.free(transform);
        }
    }

    // supports only affine transformations

    /**
     * <b>This method may be not correcting, need viryfiy it.</b>
     * @param obb
     * @param mat
     * @param result
     * @return
     */
    public static VaOrientedBoundingBox Transform( VaOrientedBoundingBox  obb, Matrix4f mat, VaOrientedBoundingBox result ){
        // !! NOT THE MOST OPTIMAL IMPLEMENTATION !!
        VaOrientedBoundingBox ret = result;
        if(ret == null)
            ret = new VaOrientedBoundingBox();

        /*vaVector3 newCenter     = vaVector3::TransformCoord( obb.Center, mat );
        vaVector3 newExtents    = vaVector3::TransformCoord( obb.Center + obb.Extents, mat ) - newCenter;*/
        Matrix4f.transformCoord(mat, obb.Center, ret.Center);
        Vector3f.add(obb.Center, obb.Extents, ret.Extents);
        Matrix4f.transformCoord(mat, ret.Extents, ret.Extents);
        Vector3f.sub(ret.Extents, ret.Center, ret.Extents);

        /*vaVector3 axisX         = vaVector3::TransformNormal( obb.Axis.r0, mat ).Normalize();
        vaVector3 axisY         = vaVector3::TransformNormal( obb.Axis.r1, mat ).Normalize();
        vaVector3 axisZ         = vaVector3::TransformNormal( obb.Axis.r2, mat ).Normalize();*/

        Matrix4f.mul(mat, obb.Axis, ret.Axis);

//        return vaOrientedBoundingBox( newCenter, newExtents, vaMatrix3x3( axisX, axisY, axisZ ) );
        return ret;
    }
}
