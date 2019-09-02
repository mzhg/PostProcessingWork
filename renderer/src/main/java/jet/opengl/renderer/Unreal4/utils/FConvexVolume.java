package jet.opengl.renderer.Unreal4.utils;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.Numeric;

public class FConvexVolume {
    public final ArrayList<FPlane> Planes = new ArrayList<>();
    /** This is the set of planes pre-permuted to SSE/Altivec form */
    public final ArrayList<FPlane> PermutedPlanes = new ArrayList<>();

    public FConvexVolume()
    {
//		int32 N = 5;
    }

    /**
     * Builds the set of planes used to clip against. Also, puts the planes
     * into a form more readily used by SSE/Altivec so 4 planes can be
     * clipped against at once.
     */
    public FConvexVolume(List<FPlane> InPlanes)
    {
        Planes.addAll(InPlanes);  //  Todo reference copy
        Init();
    }

    /**
     * Builds the permuted planes for SSE/Altivec fast clipping
     */
    public void Init(){
        int NumToAdd = Planes.size() / 4;
        int NumRemaining = Planes.size() % 4;
        // Presize the array
        PermutedPlanes.ensureCapacity(NumToAdd * 4 + (NumRemaining > 0 ? 4 : 0));
        // For each set of four planes
        for (int Count = 0, Offset = 0; Count < NumToAdd; Count++, Offset += 4)
        {
            // Add them in SSE ready form
            PermutedPlanes.add(new FPlane(Planes.get(Offset + 0).X,Planes.get(Offset + 1).X,Planes.get(Offset + 2).X,Planes.get(Offset + 3).X));
            PermutedPlanes.add(new FPlane(Planes.get(Offset + 0).Y,Planes.get(Offset + 1).Y,Planes.get(Offset + 2).Y,Planes.get(Offset + 3).Y));
            PermutedPlanes.add(new FPlane(Planes.get(Offset + 0).Z,Planes.get(Offset + 1).Z,Planes.get(Offset + 2).Z,Planes.get(Offset + 3).Z));
            PermutedPlanes.add(new FPlane(Planes.get(Offset + 0).W,Planes.get(Offset + 1).W,Planes.get(Offset + 2).W,Planes.get(Offset + 3).W));


        }
        // Pad the last set so we have an even 4 planes of vert data
        if (NumRemaining > 0)
        {
            FPlane Last1, Last2, Last3, Last4;
            // Read the last set of verts
            switch (NumRemaining)
            {
                case 3:
                {
                    Last1 = Planes.get(NumToAdd * 4 + 0);
                    Last2 = Planes.get(NumToAdd * 4 + 1);
                    Last3 = Planes.get(NumToAdd * 4 + 2);
                    Last4 = Last1;
                    break;
                }
                case 2:
                {
                    Last1 = Planes.get(NumToAdd * 4 + 0);
                    Last2 = Planes.get(NumToAdd * 4 + 1);
                    Last3 = Last4 = Last1;
                    break;
                }
                case 1:
                {
                    Last1 = Planes.get(NumToAdd * 4 + 0);
                    Last2 = Last3 = Last4 = Last1;
                    break;
                }
                default:
                {
                    Last1 = new FPlane(0, 0, 0, 0);
                    Last2 = Last3 = Last4 = Last1;
                    break;
                }
            }
            // Add them in SIMD ready form
            PermutedPlanes.add(new FPlane(Last1.X,Last2.X,Last3.X,Last4.X));
            PermutedPlanes.add(new FPlane(Last1.Y,Last2.Y,Last3.Y,Last4.Y));
            PermutedPlanes.add(new FPlane(Last1.Z,Last2.Z,Last3.Z,Last4.Z));
            PermutedPlanes.add(new FPlane(Last1.W,Last2.W,Last3.W,Last4.W));
        }
    }

    /**
     * Clips a polygon to the volume.
     *
     * @param	Polygon - The polygon to be clipped.  If the true is returned, contains the
     *			clipped polygon.
     *
     * @return	Returns false if the polygon is entirely outside the volume and true otherwise.

    public boolean ClipPolygon(class FPoly& Polygon) const;*/

    // Intersection tests.
    public int GetBoxIntersectionOutcode(ReadableVector3f Origin,ReadableVector3f Extent){
        boolean bInside = false;
        boolean bOutSide = false;

        // Load the origin & extent
//        VectorRegister Orig = VectorLoadFloat3(&Origin);
//        VectorRegister Ext = VectorLoadFloat3(&Extent);

        // Splat origin into 3 vectors
        float OrigX = /*VectorReplicate(Orig, 0)*/Origin.getX();
        float OrigY = /*VectorReplicate(Orig, 1)*/Origin.getY();
        float OrigZ = /*VectorReplicate(Orig, 2)*/Origin.getZ();
        // Splat extent into 3 vectors
        float ExtentX = /*VectorReplicate(Ext, 0)*/Extent.get(0);
        float ExtentY = /*VectorReplicate(Ext, 1)*/Extent.getY();
        float ExtentZ = /*VectorReplicate(Ext, 2)*/Extent.getZ();
        // Splat the abs for the pushout calculation
//        VectorRegister AbsExt = VectorAbs(Ext);
        float AbsExtentX = /*VectorReplicate(AbsExt, 0)*/Math.abs(ExtentX);
        float AbsExtentY = /*VectorReplicate(AbsExt, 1)*/Math.abs(ExtentY);
        float AbsExtentZ = /*VectorReplicate(AbsExt, 2)*/Math.abs(ExtentZ);

        Vector4f DistX = CacheBuffer.getCachedVec4();
        Vector4f DistY = CacheBuffer.getCachedVec4();
        Vector4f DistZ = CacheBuffer.getCachedVec4();
        Vector4f Distance = CacheBuffer.getCachedVec4();

        Vector4f PushX = CacheBuffer.getCachedVec4();
        Vector4f PushY = CacheBuffer.getCachedVec4();
        Vector4f PushOut = CacheBuffer.getCachedVec4();

        // Since we are moving straight through get a pointer to the data
//	    const FPlane* RESTRICT PermutedPlanePtr = (FPlane*)PermutedPlanes.GetData();
        int PermutedPlanePtr = 0;
        // Process four planes at a time until we have < 4 left
        for (int Count = 0; Count < PermutedPlanes.size(); Count += 4)
        {
            // Load 4 planes that are already all Xs, Ys, ...
            FPlane PlanesX = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesY = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesZ = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesW = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            // Calculate the distance (x * x) + (y * y) + (z * z) - w
            /*VectorRegister DistX = VectorMultiply(OrigX,PlanesX);
            VectorRegister DistY = VectorMultiplyAdd(OrigY,PlanesY,DistX);
            VectorRegister DistZ = VectorMultiplyAdd(OrigZ,PlanesZ,DistY);
            VectorRegister Distance = VectorSubtract(DistZ,PlanesW);*/
            Vector4f.scale(PlanesX, OrigX, DistX);
            Vector4f.scale(PlanesY, OrigY, DistY); Vector4f.add(DistY, DistX, DistY);
            Vector4f.scale(PlanesZ, OrigZ, DistZ); Vector4f.add(DistZ, DistY, DistZ);
            Vector4f.sub(DistZ, PlanesW, Distance);

            // Now do the push out FMath::Abs(x * x) + FMath::Abs(y * y) + FMath::Abs(z * z)
//            VectorRegister PushX = VectorMultiply(AbsExtentX,VectorAbs(PlanesX));
//            VectorRegister PushY = VectorMultiplyAdd(AbsExtentY,VectorAbs(PlanesY),PushX);
//            VectorRegister PushOut = VectorMultiplyAdd(AbsExtentZ,VectorAbs(PlanesZ),PushY);

            PushX.set(AbsExtentX * Math.abs(PlanesX.X), AbsExtentX * Math.abs(PlanesX.Y), AbsExtentX * Math.abs(PlanesX.Z), AbsExtentX * Math.abs(PlanesX.W));
            PushY.set(AbsExtentY * Math.abs(PlanesY.X), AbsExtentY * Math.abs(PlanesY.Y), AbsExtentY * Math.abs(PlanesY.Z), AbsExtentY * Math.abs(PlanesY.W));
            PushOut.set(AbsExtentZ * Math.abs(PlanesZ.X), AbsExtentZ * Math.abs(PlanesZ.Y), AbsExtentZ * Math.abs(PlanesZ.Z), AbsExtentZ * Math.abs(PlanesZ.W));
            Vector4f.add(PushOut, PushX, PushOut);
            Vector4f.add(PushOut, PushY, PushOut);


            // Check for completely outside
            if (Vector4f.anyGreaterThan(Distance,PushOut))
            {
//                Result.SetInside(0);
//                Result.SetOutside(1);

                bInside = false;
                bOutSide = true;
                break;
            }

            // See if any part is outside
            PushOut.scale(-1);
            if (Vector4f. anyGreaterThan(Distance,/*VectorNegate()*/ PushOut))
            {
//                Result.SetOutside(1);
                bOutSide = true;
            }
        }

        CacheBuffer.free(DistX);
        CacheBuffer.free(DistY);
        CacheBuffer.free(DistZ);
        CacheBuffer.free(Distance);
        CacheBuffer.free(PushX);
        CacheBuffer.free(PushY);
        CacheBuffer.free(PushOut);

        return Numeric.encode((short)(bInside ? 1: 0), (short)(bOutSide ? 1 : 0));
    }

    //
//	FConvexVolume::IntersectBox
//

    private static  boolean IntersectBoxWithPermutedPlanes(
	List<FPlane> PermutedPlanes,
	ReadableVector3f BoxOrigin,
    ReadableVector3f BoxExtent )
    {
        boolean Result = true;

//        checkSlow(PermutedPlanes.Num() % 4 == 0);

        // Splat origin into 3 vectors
        float OrigX = /*VectorReplicate(Orig, 0)*/BoxOrigin.getX();
        float OrigY = /*VectorReplicate(Orig, 1)*/BoxOrigin.getY();
        float OrigZ = /*VectorReplicate(Orig, 2)*/BoxOrigin.getZ();
        // Splat extent into 3 vectors
        float ExtentX = /*VectorReplicate(Ext, 0)*/BoxExtent.getX();
        float ExtentY = /*VectorReplicate(Ext, 1)*/BoxExtent.getY();
        float ExtentZ = /*VectorReplicate(Ext, 2)*/BoxExtent.getZ();
        // Splat the abs for the pushout calculation
//        VectorRegister AbsExt = VectorAbs(Ext);
        float AbsExtentX = /*VectorReplicate(AbsExt, 0)*/Math.abs(ExtentX);
        float AbsExtentY = /*VectorReplicate(AbsExt, 1)*/Math.abs(ExtentY);
        float AbsExtentZ = /*VectorReplicate(AbsExt, 2)*/Math.abs(ExtentZ);

        final Vector4f DistX = CacheBuffer.getCachedVec4();
        final Vector4f DistY = CacheBuffer.getCachedVec4();
        final Vector4f DistZ = CacheBuffer.getCachedVec4();
        final Vector4f Distance = CacheBuffer.getCachedVec4();

        final Vector4f PushX = CacheBuffer.getCachedVec4();
        final Vector4f PushY = CacheBuffer.getCachedVec4();
        final Vector4f PushOut = CacheBuffer.getCachedVec4();

        // Since we are moving straight through get a pointer to the data
//	    const FPlane* RESTRICT PermutedPlanePtr = (FPlane*)PermutedPlanes.GetData();
        int PermutedPlanePtr = 0;
        // Process four planes at a time until we have < 4 left
        for (int Count = 0; Count < PermutedPlanes.size(); Count += 4)
        {
            // Load 4 planes that are already all Xs, Ys, ...
            FPlane PlanesX = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesY = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesZ = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesW = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            // Calculate the distance (x * x) + (y * y) + (z * z) - w
            /*VectorRegister DistX = VectorMultiply(OrigX,PlanesX);
            VectorRegister DistY = VectorMultiplyAdd(OrigY,PlanesY,DistX);
            VectorRegister DistZ = VectorMultiplyAdd(OrigZ,PlanesZ,DistY);
            VectorRegister Distance = VectorSubtract(DistZ,PlanesW);*/
            Vector4f.scale(PlanesX, OrigX, DistX);
            Vector4f.scale(PlanesY, OrigY, DistY); Vector4f.add(DistY, DistX, DistY);
            Vector4f.scale(PlanesZ, OrigZ, DistZ); Vector4f.add(DistZ, DistY, DistZ);
            Vector4f.sub(DistZ, PlanesW, Distance);

            // Now do the push out FMath::Abs(x * x) + FMath::Abs(y * y) + FMath::Abs(z * z)
//            VectorRegister PushX = VectorMultiply(AbsExtentX,VectorAbs(PlanesX));
//            VectorRegister PushY = VectorMultiplyAdd(AbsExtentY,VectorAbs(PlanesY),PushX);
//            VectorRegister PushOut = VectorMultiplyAdd(AbsExtentZ,VectorAbs(PlanesZ),PushY);

            PushX.set(AbsExtentX * Math.abs(PlanesX.X), AbsExtentX * Math.abs(PlanesX.Y), AbsExtentX * Math.abs(PlanesX.Z), AbsExtentX * Math.abs(PlanesX.W));
            PushY.set(AbsExtentY * Math.abs(PlanesY.X), AbsExtentY * Math.abs(PlanesY.Y), AbsExtentY * Math.abs(PlanesY.Z), AbsExtentY * Math.abs(PlanesY.W));
            PushOut.set(AbsExtentZ * Math.abs(PlanesZ.X), AbsExtentZ * Math.abs(PlanesZ.Y), AbsExtentZ * Math.abs(PlanesZ.Z), AbsExtentZ * Math.abs(PlanesZ.W));
            Vector4f.add(PushOut, PushX, PushOut);
            Vector4f.add(PushOut, PushY, PushOut);


            // Check for completely outside
            if (Vector4f.anyGreaterThan(Distance,PushOut))
            {
                Result = false;
                break;
            }
        }

        CacheBuffer.free(DistX);
        CacheBuffer.free(DistY);
        CacheBuffer.free(DistZ);
        CacheBuffer.free(Distance);
        CacheBuffer.free(PushX);
        CacheBuffer.free(PushY);
        CacheBuffer.free(PushOut);

        return Result;
    }

    public boolean IntersectBox(ReadableVector3f Origin,ReadableVector3f Extent){
        // Load the origin & extent
//        const VectorRegister Orig = VectorLoadFloat3( &Origin );
//        const VectorRegister Ext = VectorLoadFloat3( &Extent );
        return IntersectBoxWithPermutedPlanes( PermutedPlanes, Origin, Extent );
    }

//    public boolean IntersectBox(const FVector& Origin,const FVector& Extent, bool& bOutFullyContained) const;
    public boolean IntersectSphere(ReadableVector3f Origin,float Radius) {
        boolean Result = true;

//        checkSlow(PermutedPlanes.Num() % 4 == 0);

        // Load the origin & radius
//        VectorRegister Orig = VectorLoadFloat3(&Origin);
//        VectorRegister VRadius = VectorLoadFloat1(&Radius);

        // Splat origin into 3 vectors
        float OrigX = /*VectorReplicate(Orig, 0)*/Origin.getX();
        float OrigY = /*VectorReplicate(Orig, 1)*/Origin.getY();
        float OrigZ = /*VectorReplicate(Orig, 2)*/Origin.getZ();

        final Vector4f DistX = CacheBuffer.getCachedVec4();
        final Vector4f DistY = CacheBuffer.getCachedVec4();
        final Vector4f DistZ = CacheBuffer.getCachedVec4();
        final Vector4f Distance = CacheBuffer.getCachedVec4();

        final Vector4f vRadius = CacheBuffer.getCachedVec4();
        vRadius.set(Radius, Radius, Radius, Radius);

        // Since we are moving straight through get a pointer to the data
//	    const FPlane* RESTRICT PermutedPlanePtr = (FPlane*)PermutedPlanes.GetData();
        int PermutedPlanePtr = 0;
        // Process four planes at a time until we have < 4 left
        for (int Count = 0; Count < PermutedPlanes.size(); Count += 4)
        {
            // Load 4 planes that are already all Xs, Ys, ...
            FPlane PlanesX = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesY = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesZ = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            FPlane PlanesW = PermutedPlanes.get(PermutedPlanePtr);
            PermutedPlanePtr++;
            // Calculate the distance (x * x) + (y * y) + (z * z) - w
           /* VectorRegister DistX = VectorMultiply(OrigX,PlanesX);
            VectorRegister DistY = VectorMultiplyAdd(OrigY,PlanesY,DistX);
            VectorRegister DistZ = VectorMultiplyAdd(OrigZ,PlanesZ,DistY);
            VectorRegister Distance = VectorSubtract(DistZ,PlanesW);*/

            Vector4f.scale(PlanesX, OrigX, DistX);
            Vector4f.scale(PlanesY, OrigY, DistY); Vector4f.add(DistY, DistX, DistY);
            Vector4f.scale(PlanesZ, OrigZ, DistZ); Vector4f.add(DistZ, DistY, DistZ);
            Vector4f.sub(DistZ, PlanesW, Distance);

            // Check for completely outside
            if (Vector4f.anyGreaterThan(Distance,vRadius))
            {
                Result = false;
                break;
            }
        }

        CacheBuffer.free(DistX);
        CacheBuffer.free(DistY);
        CacheBuffer.free(DistZ);
        CacheBuffer.free(Distance);
        CacheBuffer.free(vRadius);

        return Result;
    }

    public static Vector3f LinePlaneIntersection(ReadableVector3f Point1, ReadableVector3f Point2, ReadableVector4f  Plane, Vector3f out)
    {
        /*return
                Point1
                        +	(Point2-Point1)
                        *	((Plane.W - (Point1|Plane))/((Point2 - Point1)|Plane));*/

        final Vector3f _1To2 = CacheBuffer.getCachedVec3();

        // Point2-Point1
        Vector3f.sub(Point2, Point1, _1To2);

        // (Plane.W - (Point1|Plane))/((Point2 - Point1)|Plane)
        final float Factor = (Plane.getW() - Vector3f.dot(Point1, Plane))/Vector3f.dot(_1To2, Plane);
        _1To2.scale(Factor);

        out = Vector3f.add(Point1, _1To2, out);

        CacheBuffer.free(_1To2);

        return out;
    }

//    public boolean IntersectSphere(const FVector& Origin,const float& Radius, bool& bOutFullyContained) const;
    public boolean IntersectLineSegment(ReadableVector3f InStart, ReadableVector3f InEnd){
        // @todo: not optimized
        // Not sure if there's a better algorithm for this; in any case, there's scope for vectorizing some of this stuff
        // using the permuted planes array.

        // Take copies of the line segment start/end points so they can be modified
        final Vector3f Start = CacheBuffer.getCachedVec3();
        final Vector3f End = CacheBuffer.getCachedVec3();
        final Vector3f IntersectionPoint = CacheBuffer.getCachedVec3();

        Start.set(InStart);
        End.set(InEnd);

        try{
            // Iterate through all planes, successively clipping the line segment against each one,
            // until it is either completely contained within the convex volume (intersects), or
            // it is completely outside (doesn't intersect)
            for (FPlane Plane : Planes)
            {
                final float DistanceFromStart = Plane.PlaneDot(Start);
                final float DistanceFromEnd = Plane.PlaneDot(End);

                if (DistanceFromStart > 0.0f && DistanceFromEnd > 0.0f)
                {
                    // Both points are outside one of the frustum planes, so cannot intersect
                    return false;
                }

                if (DistanceFromStart < 0.0f && DistanceFromEnd < 0.0f)
                {
                    // Both points are inside this frustum plane, no need to clip it against the plane
                    continue;
                }

                // Clip the line segment against the plane
                LinePlaneIntersection(Start, End, Plane, IntersectionPoint);
                if (DistanceFromStart > 0.0f)
                {
                    Start.set(IntersectionPoint);
                }
                else
                {
                    End.set(IntersectionPoint);
                }
            }

            return true;

        }finally {
            CacheBuffer.free(Start);
            CacheBuffer.free(End);
            CacheBuffer.free(IntersectionPoint);
        }
    }

    /**
     * Intersection test with a translated axis-aligned box.
     * @param Origin - Origin of the box.
     * @param Translation - Translation to apply to the box.
     * @param Extent - Extent of the box along each axis.
     * @returns true if this convex volume intersects the given translated box.
     */
    public boolean IntersectBox(ReadableVector3f Origin,ReadableVector3f Translation,ReadableVector3f Extent){
        Vector3f BoxOrigin = CacheBuffer.getCachedVec3();
        Vector3f.add(Origin, Translation, BoxOrigin);

        try {
            return IntersectBoxWithPermutedPlanes( PermutedPlanes, BoxOrigin, Extent );
        }finally {
            CacheBuffer.free(BoxOrigin);
        }
    }

    /** Determines whether the given point lies inside the convex volume */
    public boolean IntersectPoint(ReadableVector3f Point)
    {
        return IntersectSphere(Point, 0.0f);
    }
}
