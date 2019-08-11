package jet.opengl.demos.nvidia.shadows;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import jet.opengl.postprocessing.util.BoundingBox;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/11/8.
 */

final class Frustum {

    private final Vector4f[] camPlanes = new Vector4f[6];
    private final int[] nVertexLUT = new int[6];
    final Vector3f[] pntList = new Vector3f[8];

    Frustum(){
        for(int i = 0; i < camPlanes.length; i++)
            camPlanes[i] = new Vector4f();

        for(int i = 0; i < pntList.length; i++)
            pntList[i] = new Vector3f();
    }

    Frustum( Matrix4f matrix ){
        this();
        Matrix4f.extractFrustumPlanes(matrix, camPlanes);

        //  build a bit-field that will tell us the indices for the nearest and farthest vertices from each plane...
        for (int i=0; i<6; i++)
            nVertexLUT[i] = ((camPlanes[i].x<0.f)?1:0) | ((camPlanes[i].y<0.f)?2:0) | ((camPlanes[i].z<0.f)?4:0);

        for (int i=0; i<8; i++)  // compute extrema
        {
            ReadableVector4f p0 = (i&1)!=0?camPlanes[4] : camPlanes[5];
            ReadableVector4f p1 = (i&2)!=0?camPlanes[3] : camPlanes[2];
            ReadableVector4f p2 = (i&4)!=0?camPlanes[0] : camPlanes[1];

            PlaneIntersection( pntList[i], p0, p1, p2 );
        }
    }

    static boolean IS_SPECIAL(float f){
        int intBits = Float.floatToIntBits(f);
        return (intBits & 0x7f800000)==0x7f800000;
    }

    ///////////////////////////////////////////////////////////////////////////
    //  PlaneIntersection
    //    computes the point where three planes intersect
    //    returns whether or not the point exists.
    private static boolean PlaneIntersection(Vector3f intersectPt, ReadableVector4f p0, ReadableVector4f p1, ReadableVector4f p2 )
    {
        ReadableVector3f n0 = p0;//(p0->a, p0->b, p0->c );
        ReadableVector3f n1 = p1;//( p1->a, p1->b, p1->c );
        ReadableVector3f n2 = p2;//( p2->a, p2->b, p2->c );

        Vector3f n1_n2 = new Vector3f(), n2_n0 = new Vector3f(), n0_n1 = new Vector3f();

        Vector3f.cross(n1, n2,n1_n2 );
        Vector3f.cross(n2, n0,n2_n0 );
        Vector3f.cross(n0, n1,n0_n1 );

        float cosTheta = Vector3f.dot( n0, n1_n2 );

        if ( Numeric.almostZero(cosTheta) || IS_SPECIAL(cosTheta) )
            return false;

        float secTheta = 1.f / cosTheta;

        /*n1_n2 = n1_n2 * p0->d;
        n2_n0 = n2_n0 * p1->d;
        n0_n1 = n0_n1 * p2->d;*/
        n1_n2.scale(p0.getW());
        n2_n0.scale(p1.getW());
        n0_n1.scale(p2.getW());

//    *intersectPt = -(n1_n2 + n2_n0 + n0_n1) * secTheta;
        Vector3f.add(n1_n2, n2_n0, intersectPt);
        Vector3f.add(intersectPt, n0_n1, intersectPt);
        intersectPt.scale(-secTheta);

        return true;
    }

    //  test if a sphere is within the view frustum
    boolean TestSphere     ( BoundingSphere sphere ) {
        boolean inside = true;
        float radius = sphere.radius;

        for (int i=0; (i<6) && inside; i++)
            inside &= ((Vector4f.planeDotCoord(camPlanes[i], sphere.centerVec) + radius) >= 0.f);

        return inside;
    }

    //  Tests if an AABB is inside/intersecting the view frustum
    int  TestBox        ( BoundingBox box ) {
        boolean intersect = false;

        Vector3f nVertex = new Vector3f();
        Vector3f pVertex = new Vector3f();
        for (int i=0; i<6; i++)
        {
            int nV = nVertexLUT[i];
            // pVertex is diagonally opposed to nVertex
            nVertex.set( (nV&1)!=0?box._min.x:box._max.x, (nV&2)!=0?box._min.y:box._max.y, (nV&4)!=0?box._min.z:box._max.z );
            pVertex.set( (nV&1)!=0?box._max.x:box._min.x, (nV&2)!=0?box._max.y:box._min.y, (nV&4)!=0?box._max.z:box._min.z );

            if ( Vector4f.planeDotCoord(camPlanes[i], nVertex) < 0.f )
            return 0;
            if ( Vector4f.planeDotCoord(camPlanes[i], pVertex) < 0.f )
            intersect = true;
        }

        return (intersect)?2 : 1;
    }

    //  this function tests if the projection of a bounding sphere along the light direction intersects
    //  the view frustum
    //  this function tests if the projection of a bounding sphere along the light direction intersects
//  the view frustum

    private static boolean SweptSpherePlaneIntersect(Vector2f t, ReadableVector4f plane, BoundingSphere sphere, ReadableVector3f sweepDir)
    {
        float b_dot_n = Vector4f.planeDotCoord(plane, sphere.centerVec);
        float d_dot_n = Vector4f.planeDotNormal(plane, sweepDir);

        float t0 = -1, t1 = -1;
        try{
            if (d_dot_n == 0.f)
            {
                if (b_dot_n <= sphere.radius)
                {
                    //  effectively infinity
                    t0 = 0.f;
                    t1 = 1e32f;
                    return true;
                }
                else
                    return false;
            }
            else
            {
                float tmp0 = ( sphere.radius - b_dot_n) / d_dot_n;
                float tmp1 = (-sphere.radius - b_dot_n) / d_dot_n;
                t0 = Math.min(tmp0, tmp1);
                t1 = Math.max(tmp0, tmp1);
                return true;
            }
        }finally {
            t.set(t0, t1);
        }
    }

    boolean TestSweptSphere( BoundingSphere sphere, ReadableVector3f sweepDir ){
        //  algorithm -- get all 12 intersection points of the swept sphere with the view frustum
        //  for all points >0, displace sphere along the sweep driection.  if the displaced sphere
        //  is inside the frustum, return TRUE.  else, return FALSE
        float[] displacements = new float[12];
        int cnt = 0;
        Vector2f ab = new Vector2f();
        boolean inFrustum = false;

        for (int i=0; i<6; i++)
        {
            if (SweptSpherePlaneIntersect(ab, camPlanes[i], sphere, sweepDir))
            {
                if (ab.x>=0.f)
                    displacements[cnt++] = ab.x;
                if (ab.y>=0.f)
                    displacements[cnt++] = ab.y;
            }
        }

        BoundingSphere displacedSphere = new BoundingSphere();
        for (int i=0; i<cnt; i++)
        {
            displacedSphere.set(sphere);
//            displacedSphere.centerVec += (*sweepDir)*displacements[i];
            Vector3f.linear(displacedSphere.centerVec, sweepDir, displacements[i], displacedSphere.centerVec);
            displacedSphere.radius *= 1.1f;
            inFrustum |= TestSphere(displacedSphere);
        }

        return inFrustum;
    }


}
