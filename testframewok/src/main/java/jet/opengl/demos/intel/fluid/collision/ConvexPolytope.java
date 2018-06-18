package jet.opengl.demos.intel.fluid.collision;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/4/7 0007.
 */

public class ConvexPolytope extends ShapeBase{
    private final ArrayList<Plane> mPlanes = new ArrayList<>() ;   ///< List of planar faces that constitute this polytope.

    public static final int sShapeType = 1937007472 /*'stcp'*/ ;

    /** Construct a convex polytope.
     */
    public ConvexPolytope( boolean isHole /*= false*/ )
//            : ShapeBase( sShapeType , isHole )
    {
        super(sShapeType, isHole);
    }

    public ConvexPolytope() { this(false);}

    public ConvexPolytope(Plane[] faces , boolean isHole ){
        super(sShapeType, isHole);
        setFaces(faces);
    }

    public ConvexPolytope( ConvexPolytope that , Matrix3f rotation ){
        super(sShapeType, that.isHole());

        final int numFaces = that.mPlanes.size() ;
        mPlanes.ensureCapacity( numFaces ); ;
        Vector3f rotatedNormal = new Vector3f();

        for( int idxFace = 0 ; idxFace < numFaces ; ++ idxFace )
        {   // For each face in the original...
//            Vec3 rotatedNormal = rotation.Transform( that.mPlanes[ idxFace ].GetNormal() ) ;
            Matrix3f.transform(rotation, that.mPlanes.get(idxFace).getNormal(), rotatedNormal);
            Plane rotatedPlane = new Plane( rotatedNormal , that.mPlanes.get(idxFace).getD() ) ;
            mPlanes.add( rotatedPlane ) ;
        }
    }

    public void set(ConvexPolytope ohs){
        mPlanes.clear();
        mPlanes.addAll(ohs.mPlanes);
    }

    public void setFaces( Plane faces[] ){
        assert ( faces != null ) ;

        int numFaces = faces.length;
        assert( numFaces > 0 ) ;
        mPlanes.clear() ;
        mPlanes.ensureCapacity( numFaces ); ;
//        DEBUG_ONLY( bool dSign = faces[ 0 ].GetD() >= 0.0f ) ;
        boolean dSign = faces[ 0 ].getD() >= 0.0f;

        for( int idxFace = 0 ; idxFace < numFaces ; ++ idxFace )
        {   // For each face in faces...
            assert ( dSign == ( faces[ idxFace ].getD() >= 0.0f ) ) ; // All planes must have same sense (interior or exterior).
            mPlanes.add( faces[ idxFace ] ) ;
        }
    }

    /** Return distance of the given point from a feature of this convex polytope.

     \param queryPoint   Point in world space being asked about.

     \param idxPlaneLeastPenetration Index of plane in this polytope
     in which the given queryPoint has the least penetration.

     \return Distance of the given point from this convex polytope.
     Value is negative when queryPoint lies within polytope.

     Positive values mean queryPoint lies outside polytope,
     but do not necessarily indicate the actual distance
     to the polytope; actual distance could be larger.
     The reason is that, outside a polytope, there are regions
     where the closest point is to an edge or vertex instead
     of to a face.  This routine assumes that the caller
     only cares about actual penetration, not distance,
     so this routine makes no attempt to handle edge
     cases (no pun intended).

     \note   The notion of "inside" for a hole means that the opposite of what it
     means for non-holes.
     */
    public float contactDistance(ReadableVector3f queryPoint , int[] idxPlaneLeastPenetration ) {
        float largestDistance = - Float.MAX_VALUE ;
        final int numPlanes = mPlanes.size() ;
        for( int iPlane = 0 ; iPlane < numPlanes ; ++ iPlane )
        {   // For each planar face of this convex hull...
            final float distToPlane = mPlanes.get(iPlane).distance( queryPoint ) ;
            if( distToPlane > largestDistance )
            {   // Point distance to iPlane is largest of all planes visited so far.
                largestDistance = distToPlane ;
                // Remember this plane.
                idxPlaneLeastPenetration[0] = iPlane ;
            }
        }
        return largestDistance * getParity() ;
    }

    /** Return distance of the given point from a feature of this convex polytope.

     \param queryPoint   See other ContactDistance.

     \param position     World-space position of this polytope.

     \param orientation  World-space orientation of this polytope.

     \param idxPlaneLeastPenetration See other ContactDistance.

     \return See other ContactDistance.
     */
    public float contactDistance( ReadableVector3f queryPoint , ReadableVector3f position , Matrix3f orientation , int[] idxPlaneLeastPenetration ){
//        assert ( orientation.IsOrthonormal() ) ;

        // The polytope should be translated and rotated according to position and orientation,
        // which is tantamount to transforming the query point by the inverse of those.
        // Transforming the point takes less time than transforming each plane. (if done here.)
// NOTE: Could/should pre-transform Planes, outside per-particle loop, instead of transforming each particle.
// As long as num particles exceeds num planes, that would run faster.
// If you pre-transform Planes, remember not to transform plane in ContactPoint.

        final Vector3f translatedPoint = Vector3f.sub(queryPoint, position, null) ;
//        const Vec3 reorientedPoint = orientation.TransformByTranspose( translatedPoint ) ;
        orientation.transpose();
        Matrix3f.transform(orientation, translatedPoint, translatedPoint);
        orientation.transpose();

        return contactDistance( translatedPoint , idxPlaneLeastPenetration ) ;
    }

    public float collisionDistance( ReadableVector3f queryPoint, ReadableVector3f queryPointRelativeVelocity , int[] idxPlaneLeastPenetration ) {
        float largestDistance = - Float.MAX_VALUE ;
        final int numPlanes = mPlanes.size() ;
        for( int iPlane = 0 ; iPlane < numPlanes ; ++ iPlane )
        {   // For each planar face of this convex hull...
            Plane testPlane = mPlanes.get(iPlane) ;
            final float distToPlane = testPlane.distance( queryPoint ) ;
            if( distToPlane > largestDistance )
            {   // Point distance to iPlane is largest of all planes visited so far.
                final float speedThroughPlane = Vector3f.dot(queryPointRelativeVelocity, testPlane.getNormal()) * getParity() ;
                if(     ( speedThroughPlane <= 0.0f )   // Query point is going deeper through this face.
                        ||  ( distToPlane >= 0.0f )         // Query point is outside polytope.
                        ||  ( isHole() )                    // Polytope is a hole.
                        )
                {   // Query point is moving deeper through this plane.
                    largestDistance = distToPlane ;
                    // Remember this plane.
                    idxPlaneLeastPenetration[0] = iPlane ;
                }
            }
        }
        return largestDistance * getParity() ;
    }

    public float collisionDistance(ReadableVector3f queryPoint, ReadableVector3f queryPointVelocity ,ReadableVector3f position ,
                                   Matrix3f orientation ,ReadableVector3f polytopeVelocity , int[] idxPlaneLeastPenetration ) {
        // The polytope should be translated and rotated according to position and orientation,
        // which is tantamount to transforming the query point by the inverse of those.
        // Transforming the point takes less time than transforming each plane. (if done here.)
// NOTE: Could/should pre-transform Planes, outside per-particle loop, instead of transforming each particle.
// As long as num particles exceeds num planes, that would run faster.
// If you pre-transform Planes, remember not to transform plane in ContactPoint.

        final Vector3f translatedPoint    = Vector3f.sub(queryPoint, position, null) ;
//        final ReadableVector3f reorientedPoint    = orientation.TransformByTranspose( translatedPoint ) ;
        final Vector3f relativeVelocity   = Vector3f.sub(queryPointVelocity, polytopeVelocity, null) ;
//        final ReadableVector3f reorientedVelocity = orientation.TransformByTranspose( relativeVelocity ) ;
        orientation.transpose();
        final ReadableVector3f reorientedPoint = Matrix3f.transform(orientation, translatedPoint, translatedPoint);
        final ReadableVector3f reorientedVelocity = Matrix3f.transform(orientation, relativeVelocity, relativeVelocity);
        orientation.transpose();

        return collisionDistance( reorientedPoint , reorientedVelocity , idxPlaneLeastPenetration ) ;
    }

    /** Return distance of the given sphere from a feature of this convex polytope.

     \param queryPoint   Center of sphere being asked about.

     \param sphereRadius Radius of sphere being asked about.

     \param idxPlaneLeastPenetration Index of plane in this polytope
     in which the given sphere has the least penetration.

     \return Distance of the given sphere from this convex polytope.
     Value is negative when sphere overlaps with polytope.

     Positive values mean sphere lies entirely outside polytope,
     but do not necessarily indicate the actual distance
     to the polytope; actual distance could be larger.
     The reason is that, outside a polytope, there are regions
     where the closest point is to an edge or vertex instead
     of to a face.  This routine assumes that the caller
     only cares about actual penetration, not distance,
     so this routine makes no attempt to handle edge
     cases (no pun intended).

     \note This routine will return early if it finds
     any faces for which the sphere lies entirely outside that face.

     */
    public float contactDistanceSphere( ReadableVector3f queryPoint , float sphereRadius , int[] idxPlaneLeastPenetration ) {
        float largestDistance = - Float.MAX_VALUE ;
        final int numPlanes = mPlanes.size() ;
        for( int iPlane = 0 ; iPlane < numPlanes ; ++ iPlane )
        {   // For each planar face of this convex hull...
            final float distToPlane = mPlanes.get(iPlane).distance( queryPoint ) - sphereRadius ;
            if( ISignBit( distToPlane ) ==0)
            {   // Point is outside hull.
                assert ( distToPlane >= 0.0f ) ;
                // iPlane might not be the closest face.
                // Sphere could be closer, but it cannot be any farther.
                // Remember this plane.
                idxPlaneLeastPenetration[0] = iPlane ;
                return distToPlane ;
            }
            if( distToPlane > largestDistance )
            {   // Sphere penetrates iPlane least, of all planes visited so far.
                largestDistance = distToPlane ;
                // Remember this plane.
                idxPlaneLeastPenetration[0] = iPlane ;
            }
        }
        return largestDistance * getParity() ;
    }

    /** Return distance of the given sphere from a feature of this convex polytope.

     \param queryPoint   Center of sphere being asked about.

     \param sphereRadius Radius of sphere being asked about.

     \param position     World-space position of this polytope.

     \param orientation  World-space orientation of this polytope.

     \param idxPlaneLeastPenetration Index of plane in this polytope
     in which the given sphere has the least penetration.

     \return Distance of the given sphere from this convex polytope.
     Value is negative when sphere overlaps with polytope.

     Positive values mean sphere lies entirely outside polytope,
     but do not necessarily indicate the actual distance
     to the polytope; actual distance could be larger.
     The reason is that, outside a polytope, there are regions
     where the closest point is to an edge or vertex instead
     of to a face.  This routine assumes that the caller
     only cares about actual penetration, not distance,
     so this routine makes no attempt to handle edge
     cases (no pun intended).

     \note This routine will return early when it finds
     any faces for which the sphere lies entirely outside that face.

     */
    public float contactDistanceSphere( ReadableVector3f queryPoint , float sphereRadius , ReadableVector3f position , Matrix3f orientation , int[] idxPlaneLeastPenetration ){
        // The polytope should be translated and rotated according to position and orientation,
        // which is tantamount to transforming the query point by the inverse of those.
        // Transforming the point takes less time than transforming each plane.
        final Vector3f translatedPoint = Vector3f.sub(queryPoint, position, null) ;
//        final ReadableVector3f reorientedPoint = orientation.TransformByTranspose( translatedPoint ) ;
        orientation.transpose();
        Matrix3f.transform(orientation, translatedPoint, translatedPoint);
        orientation.transpose();

        // NOTE: Could/should pre-transform Planes, outside per-particle loop, instead of transforming each particle.
        // As long as num particles exceeds num planes, that would run faster.
        // If you pre-transform Planes, remember not to transform plane in ContactPoint.

        return contactDistanceSphere( translatedPoint , sphereRadius , idxPlaneLeastPenetration ) ;
    }

    /** Return contact point given a query point, plane and contact distance.

     This routine is meant to yield additional information after running one of
     the distance routines to obtain an appropriate face and distance.
     */
    public Vector3f  contactPoint( ReadableVector3f queryPoint , int idxPlaneLeastPenetration , float distance ) {
        Vector3f    contactNormal   = mPlanes.get(idxPlaneLeastPenetration).getNormal() /** GetParity()*/ ;
        contactNormal.scale(getParity());
        /*Vec3            contactPoint    = queryPoint - contactNormal * distance ;
        return contactPoint ;*/
        contactNormal.scale(distance);
        return Vector3f.sub(queryPoint, contactNormal, contactNormal);
    }

    /** Return contact point given a query point, plane and contact distance.

     This routine is meant to yield additional information after running one of
     the distance routines to obtain an appropriate face and distance.
     */
    public Vector3f  contactPoint( ReadableVector3f queryPoint , Matrix3f orientation , int idxPlaneLeastPenetration , float distance , Vector3f contactNormal ) {
        Plane originalPlane   = mPlanes.get(idxPlaneLeastPenetration) ;
        // Note that for holes, contactNormal is reversed.
//        contactNormal                       = orientation.Transform( originalPlane.GetNormal() ) * GetParity() ;
        Matrix3f.transform(orientation, originalPlane.getNormal(), contactNormal);
        contactNormal.scale(getParity());

        // For holes, contactNormal is reversed, but so is distance, which should
        // have already had parity applied.  Since parity is applied to both normal
        // and distance, it cancels out so it needs to be applied again here.
//        Vec3                contactPoint    = queryPoint - contactNormal * distance ;
//        return contactPoint ;
        return Vector3f.linear(queryPoint, contactNormal, -distance, null);
    }

    public Plane getPlane( int idxPlane )
    {
        return mPlanes.get(idxPlane) ;
    }

    /// Return sign bit of floating point value.
    private static int ISignBit( float f )
    {
        return ( /*(int&) f*/ Float.floatToIntBits(f) & 0x80000000 ) >> 31 ;
    }

    /** Convenience utility routine to make a box polytope with given dimensions
     */
    public static void makeBox( ConvexPolytope convexPolytope , ReadableVector3f dimensions )
    {
        final float x = dimensions.getX() * 0.5f ;
        final float y = dimensions.getY() * 0.5f ;
        final float z = dimensions.getZ() * 0.5f ;
        final Plane faces[] =
        {
            new Plane( new Vector3f(  1.0f ,  0.0f ,  0.0f ) , x )
        ,   new Plane( new Vector3f( -1.0f ,  0.0f ,  0.0f ) , x )
        ,   new Plane( new Vector3f(  0.0f ,  1.0f ,  0.0f ) , y )
        ,   new Plane( new Vector3f(  0.0f , -1.0f ,  0.0f ) , y )
        ,   new Plane( new Vector3f(  0.0f ,  0.0f ,  1.0f ) , z )
        ,   new Plane( new Vector3f(  0.0f ,  0.0f , -1.0f ) , z )
        } ;

        final int numFaces = faces.length ;

        convexPolytope.setFaces( faces /*, numFaces*/ ) ;

        if( convexPolytope.isHole() )
        {   // Make inscribed sphere.
            final float thin = 1.0f - Float.MIN_VALUE ;
            final float smallestDim = Math.min( x , Math.min( y , z)) ;
            convexPolytope.setBoundingSphereRadius( thin * smallestDim ) ;
        }
        else
        {   // Make circumscribing sphere.
            final float fatten = 1.0f + Float.MIN_VALUE ;    // Make bounding sphere slightly larger.
            convexPolytope.setBoundingSphereRadius( 0.5f * fatten * dimensions.length() ) ;
        }
    }

}
