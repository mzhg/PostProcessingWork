package jet.opengl.postprocessing.util;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/** General purpose axis-aligned bounding box class for enclosing objects/vertices.
 * Bounds leaf objects in a scene such as osg::Drawable objects. Used for frustum
 * culling etc.
 */
public class BoundingBox {

    private static final float MAX = Float.MAX_VALUE;
    private static final float MIN = -MAX;

    /** Minimum extent. (Smallest X, Y, and Z values of all coordinates.) */
    public final Vector3f _min = new Vector3f(MAX, MAX, MAX);
    /** Maximum extent. (Greatest X, Y, and Z values of all coordinates.) */
    public final Vector3f _max = new Vector3f(MIN, MIN, MIN);

    /** Creates an uninitialized bounding box. */
    public BoundingBox() {
    }

    /** Creates a bounding box initialized to the given extents. */
    public BoundingBox(float xmin, float ymin, float zmin, float xmax, float ymax, float zmax){
        _min.set(xmin,ymin,zmin);
        _max.set(xmax,ymax,zmax);
    }

    /** Creates a bounding box initialized to the given extents. */
    public BoundingBox(Vector3f min, Vector3f max){
        _min.set(min);
        _max.set(max);
    }

    // Copy constructor
    public BoundingBox(BoundingBox bb) {
        _min.set(bb._min);
        _max.set(bb._max);
    }

    /** Clear the bounding box. Erases existing minimum and maximum extents. */
    public void init(){
        _min.set(MAX, MAX, MAX);
        _max.set(MIN, MIN, MIN);
    }

    /** Returns true if the bounding box extents are valid, false otherwise. */
    public boolean valid()
    {
        return _max.x>=_min.x &&  _max.y>=_min.y &&  _max.z>=_min.z;
    }

    /** Sets the bounding box extents. */
    public void set (float xmin, float ymin, float zmin,
                     float xmax, float ymax, float zmax)
    {
        _min.set(xmin,ymin,zmin);
        _max.set(xmax,ymax,zmax);
    }

    /** Sets the bounding box extents. */
    public void set(Vector3f min,Vector3f max) {
        _min.set(min);
        _max.set(max);
    }

    public float xMin() { return _min.x; }

    public float yMin() { return _min.y; }

    public float zMin() { return _min.z; }

    public float xMax() { return _max.x; }

    public float yMax() { return _max.y; }

    public float zMax() { return _max.z; }

    public static BoundingBox transform(Matrix4f left, BoundingBox right, BoundingBox dest){
        if(dest == null){
            dest = new BoundingBox();
        }

        float maxx = MIN, maxy = MIN,maxz = MIN;
        float minx = MAX, miny = MAX,minz = MAX;

        final float omaxx = right._max.x,omaxy = right._max.y,omaxz = right._max.z;
        final float ominx = right._min.x,ominy = right._min.y,ominz = right._min.z;

        Vector3f temp = dest._max;
        {
            temp.set(ominx, ominy, ominz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(omaxx, ominy, ominz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(omaxx, omaxy, ominz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(ominx, omaxy, ominz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(ominx, ominy, omaxz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(omaxx, ominy, omaxz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(omaxx, omaxy, omaxz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        {
            temp.set(ominx, omaxy, omaxz);
            Matrix4f.transformCoord(left, temp, temp);
            if(minx > temp.x) minx = temp.x;
            if(miny > temp.y) miny = temp.y;
            if(minz > temp.z) minz = temp.z;
            if(maxx < temp.x) maxx = temp.x;
            if(maxy < temp.y) maxy = temp.y;
            if(maxz < temp.z) maxz = temp.z;
        }

        dest._min.set(minx, miny, minz);
        dest._max.set(maxx, maxy, maxz);
        return dest;
    }

    /** Calculates and returns the bounding box center. */
    public Vector3f center(Vector3f center)
    {
        return Vector3f.mix(_min, _max, 0.5f, center);
    }

    /** Calculates and returns the bounding box radius. */
    public float radius()
    {
        return (float) Math.sqrt(radius2());
    }

    /** Calculates and returns the squared length of the bounding box radius.
     * Note, radius2() is faster to calculate than radius(). */
    public float radius2()
    {
        return 0.25f */*((_max-_min).length2())*/ Vector3f.distanceSquare(_max, _min);
    }

    /** Returns a specific corner of the bounding box.
     * pos specifies the corner as a number between 0 and 7.
     * Each bit selects an axis, X, Y, or Z from least- to
     * most-significant. Unset bits select the minimum value
     * for that axis, and set bits select the maximum. */
    public Vector3f corner(int pos, Vector3f corner)
    {
        if(corner == null) corner = new Vector3f();
        corner.set((pos&1) != 0 ?_max.x:_min.x,(pos&2)!= 0?_max.y:_min.y,(pos&4) != 0?_max.z:_min.z);
        return corner;
    }

    /** Returns a specific corner of the bounding box.
     * pos specifies the corner as a number between 0 and 7.
     * Each bit selects an axis, X, Y, or Z from least- to
     * most-significant. Unset bits select the minimum value
     * for that axis, and set bits select the maximum. */
    public Vector4f corner(int pos, Vector4f corner)
    {
        if(corner == null) corner = new Vector4f();
        corner.set((pos&1) != 0 ?_max.x:_min.x,(pos&2)!= 0?_max.y:_min.y,(pos&4) != 0?_max.z:_min.z, 1);
        return corner;
    }

    /** Expands the bounding box to include the given coordinate.
     * If the box is uninitialized, set its min and max extents to v. */
    public void expandBy(ReadableVector3f v)
    {
        expandBy(v.getX(), v.getY(), v.getZ());
    }

    /** Expands the bounding box to include the given coordinate.
     * If the box is uninitialized, set its min and max extents to
     * Vec3(x,y,z). */
    public void expandBy(float x,float y,float z)
    {
        if(x<_min.x) _min.x = x;
        if(x>_max.x) _max.x = x;

        if(y<_min.y) _min.y = y;
        if(y>_max.y) _max.y = y;

        if(z<_min.z) _min.z = z;
        if(z>_max.z) _max.z = z;
    }

    /** Expands this bounding box to include the given bounding box.
     * If this box is uninitialized, set it equal to bb. */
    public void expandBy(BoundingBox bb)
    {
        if (!bb.valid()) return;

        if(bb._min.x<_min.x) _min.x = bb._min.x;
        if(bb._max.x>_max.x) _max.x = bb._max.x;

        if(bb._min.y<_min.y) _min.y = bb._min.y;
        if(bb._max.y>_max.y) _max.y = bb._max.y;

        if(bb._min.z<_min.z) _min.z = bb._min.z;
        if(bb._max.z>_max.z) _max.z = bb._max.z;
    }

    /** Returns the intersection of this bounding box and the specified bounding box. */
    public static BoundingBox intersect(BoundingBox a, BoundingBox b, BoundingBox dest)
    {   /*if(dest == null)
        dest =  new BoundingBox(Math.min(xMin(),bb.xMin()),Math.min(yMin(),bb.yMin()),Math.min(zMin(),bb.zMin()),
                Math.max(xMax(),bb.xMax()),Math.max(yMax(),bb.yMax()),Math.max(zMax(),bb.zMax()));
    else
        dest.set(Math.min(xMin(),bb.xMin()),Math.min(yMin(),bb.yMin()),Math.min(zMin(),bb.zMin()),
                Math.max(xMax(),bb.xMax()),Math.max(yMax(),bb.yMax()),Math.max(zMax(),bb.zMax()));
        return dest;*/

        if(dest == null)
            dest = new BoundingBox();

        Vector3f.max(a._min, b._min, dest._min);
        Vector3f.min(a._max, b._max, dest._max);
        return dest;
    }

    /** Return true if this bounding box intersects the specified bounding box. */
    public boolean intersects(BoundingBox bb)
    {    return Math.min(xMin(),bb.xMin()) <= Math.max(xMax(),bb.xMax()) &&
            Math.min(yMin(),bb.yMin()) <= Math.max(yMax(),bb.yMax()) &&
            Math.min(zMin(),bb.zMin()) <= Math.max(zMax(),bb.zMax());

    }

    public boolean intersect(float[] hitDist, ReadableVector3f origPt, ReadableVector3f dir){
        Vector4f sides[] = {
                new Vector4f( 1, 0, 0,-_min.x), new Vector4f(-1, 0, 0, _max.x),
                new Vector4f( 0, 1, 0,-_min.y), new Vector4f( 0,-1, 0, _max.y),
                new Vector4f( 0, 0, 1,-_min.z), new Vector4f( 0, 0,-1, _max.z) };

        hitDist[0] = 0.f;  // safe initial value
        Vector3f hitPt = new Vector3f(origPt);

        boolean inside = false;

        for ( int i=0; (i<6) && !inside; i++ )
        {
            float cosTheta = Vector4f.planeDotNormal( sides[i], dir );
            float dist = Vector4f.planeDotCoord ( sides[i], origPt );

            //  if we're nearly intersecting, just punt and call it an intersection
            if ( Numeric.almostZero(dist) ) return true;
            //  skip nearly (&actually) parallel rays
            if ( Numeric.almostZero(cosTheta) ) continue;
            //  only interested in intersections along the ray, not before it.
            hitDist[0] = -dist / cosTheta;
            if ( hitDist[0] < 0.f ) continue;

//            hitPt = (*hitDist)*(*dir) + (*origPt);
            Vector3f.linear(origPt, dir, hitDist[0], hitPt);

            inside = true;

            for ( int j=0; (j<6) && inside; j++ )
            {
                if ( j==i )
                    continue;
                float d = Vector4f.planeDotCoord( sides[j], hitPt );

                inside = ((d + 0.00015) >= 0.f);
            }
        }

        return inside;
    }

    public void setFromExtent(ReadableVector3f center, ReadableVector3f extent){
        Vector3f.sub(center, extent, _min);
        Vector3f.add(center, extent, _max);
    }

    /** Returns true if this bounding box contains the specified coordinate. */
    public boolean contains(Vector3f v)
    {
        return valid() &&
                (v.x>=_min.x && v.x<=_max.x) &&
                (v.y>=_min.y && v.y<=_max.y) &&
                (v.z>=_min.z && v.z<=_max.z);
    }

    public void set(BoundingBox ohs) {
        _min.set(ohs._min);
        _max.set(ohs._max);
    }

    @Override
    public String toString() {
        return "Min = " + _min + ", Max = " + _max;
    }
}
