package jet.opengl.demos.intel.fluid.collision;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by Administrator on 2018/4/7 0007.
 */

public class Plane extends Vector4f {
    /** Construct plane representation from floats.
     \param normal           Plane normal vector, the direction the plane faces.
     Must be a unit vector.

     \param distFromOrigin   Distance of plane, along normal, from origin.
     Must be non-negative.
     */
    public Plane(ReadableVector3f normal , float distFromOrigin )
    {
        assert (Math.abs(normal.lengthSquared() - 1.0f) < Numeric.EPSILON);
        assert ( distFromOrigin >= 0.0f ) ;
        x = normal.getX() ;
        y = normal.getY() ;
        z = normal.getZ() ;
        w = distFromOrigin ;
    }


    public Plane( ReadableVector4f v4 )
    {
        set(v4);
    }


    /** Return direction plane faces.
     */
    public Vector3f getNormal() { return new Vector3f(x,y,z); }

    /** Return distance, along normal, of plane from origin.
     */
    public float getD() { return w ; }


    /** Return signed distance of the given point to this plane.
     */
    public float distance( ReadableVector3f vPoint )
    {
        float distFromPlane = Vector3f.dot(this, vPoint) - w ;
        return distFromPlane ;
    }
}
