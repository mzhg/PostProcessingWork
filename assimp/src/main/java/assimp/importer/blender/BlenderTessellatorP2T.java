package assimp.importer.blender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import poly2tri.common.Point;
import poly2tri.common.Triangle;
import poly2tri.sweep.CDT;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;

final class BlenderTessellatorP2T {
	
	static final int BLEND_TESS_MAGIC = 0x83ed9ac3;

	BlenderBMeshConverter converter;
	
	static final class PointP2T
	{
		final Vector3f point3D = new Vector3f();
		final Point point2D = new Point();
		int magic;
		int index;
	};

	static final class PlaneP2T
	{
		final Vector3f centre = new Vector3f();
		final Vector3f normal = new Vector3f();
	};
	
	BlenderTessellatorP2T(BlenderBMeshConverter converter ){
		this.converter = converter;
	}
	
	final HashMap<Point, PointP2T> maps = new HashMap<Point, BlenderTessellatorP2T.PointP2T>();

	void tessellate( MLoop[] polyLoop, int vertexCount, List<MVert> vertices ){
		if(vertexCount == 0)
			throw new AssertionError();
		// NOTE - We have to hope that points in a Blender polygon are roughly on the same plane.
		//        There may be some triangulation artifacts if they are wildly different.

		ArrayList<PointP2T> points = new ArrayList<PointP2T>();
		copy3DVertices( polyLoop, vertexCount, vertices, points );

		PlaneP2T plane = findLLSQPlane( points );

		Matrix4f transform = generatePointTransformMatrix( plane );

		transformAndFlattenVectices( transform, points );

//		std::vector< p2t::Point* > pointRefs;
		ArrayList<Point> pointRefs = new ArrayList<Point>();
		referencePoints( points, pointRefs );

		CDT cdt = new CDT( pointRefs );

		cdt.triangulate( );
		List<Triangle> triangles = cdt.getTriangles( );

		makeFacesFromTriangles( triangles );
	}
	
	void assertVertexCount( int vertexCount ){
		if ( vertexCount <= 4 )
		{
			throw new DeadlyImportError( "Expected more than 4 vertices for tessellation" );
		}
	}
	
	void copy3DVertices( MLoop[] polyLoop, int vertexCount, List<MVert> vertices, ArrayList<PointP2T> points ){
		points.ensureCapacity(vertexCount);
		for ( int i = 0; i < vertexCount; ++i )
		{
			MLoop loop = polyLoop[i];
			MVert vert = vertices.get(loop.v);

			PointP2T point = new PointP2T();
			point.point3D.set( vert.co[ 0 ], vert.co[ 1 ], vert.co[ 2 ] );
			point.index = loop.v;
			point.magic = BLEND_TESS_MAGIC;
			points.add(point);
		}
	}
	
	Matrix4f generatePointTransformMatrix(PlaneP2T plane ){
		Vector3f sideA = new Vector3f( 1.0f, 0.0f, 0.0f );
		if ( Math.abs( Vector3f.dot(plane.normal, sideA) ) > 0.999f )
		{
			sideA.set( 0.0f, 1.0f, 0.0f );
		}

//		aiVector3D sideB( plane.normal ^ sideA );
		Vector3f sideB = Vector3f.cross(plane.normal, sideA, null);
		sideB.normalise( );
//		sideA = sideB ^ plane.normal;
		Vector3f.cross(sideB, plane.normal, sideA);

		Matrix4f result = new Matrix4f();
		result.m00 = sideA.x;
		result.m10 = sideA.y;
		result.m20 = sideA.z;
		result.m01 = sideB.x;
		result.m11 = sideB.y;
		result.m21 = sideB.z;
		result.m02 = plane.normal.x;
		result.m12 = plane.normal.y;
		result.m22 = plane.normal.z;
		result.m30 = plane.centre.x;
		result.m31 = plane.centre.y;
		result.m32 = plane.centre.z;
//		result.inverse( );
		Matrix4f.invert(result, result);

		return result;
	}
	
	void transformAndFlattenVectices( Matrix4f transform, List<PointP2T> vertices ){
		for (int i = 0; i < vertices.size( ); ++i )
		{
			PointP2T point = vertices.get(i);
//			point.point3D = transform * point.point3D;
			Matrix4f.transformVector(transform, point.point3D, point.point3D);
			point.point2D.set( point.point3D.y, point.point3D.z );
			
			PointP2T prev = maps.put(point.point2D, point);
			if(DefaultLogger.LOG_OUT &&prev != null){
				DefaultLogger.warn("dumplicate Point");
			}
		}
	}
	
	void referencePoints(List<PointP2T> points, ArrayList<Point > pointRefs ){
		pointRefs.ensureCapacity(points.size());
		
		for(PointP2T p : points)
			pointRefs.add(p.point2D);
	}
	
	PointP2T getActualPointStructure(Point point ){
		return maps.get(point);
	}
	
	void makeFacesFromTriangles(List<Triangle> triangles ){
		for (int i = 0; i < triangles.size( ); ++i )
		{
			Triangle triangle = triangles.get( i );

			PointP2T pointA = getActualPointStructure( triangle.getPoint( 0 ) );
			PointP2T pointB = getActualPointStructure( triangle.getPoint( 1 ) );
			PointP2T pointC = getActualPointStructure( triangle.getPoint( 2 ) );

			converter.addFace( pointA.index, pointB.index, pointC.index ,0);
		}
	}

	// Adapted from: http://missingbytes.blogspot.co.uk/2012/06/fitting-plane-to-point-cloud.html
	float findLargestMatrixElem(Matrix3f mtx ){
		float result = 0.0f;

		for ( int x = 0; x < 3; ++x )
		{
			for ( int y = 0; y < 3; ++y )
			{
				result = Math.max( Math.abs( /*mtx[ x ][ y ]*/ mtx.get(x, y, false) ), result );
			}
		}

		return result;
	}
	void scaleMatrix(Matrix3f mtx, float scale, Matrix3f result){
		for ( int x = 0; x < 3; ++x )
		{
			for ( int y = 0; y < 3; ++y )
			{
//				result[ x ][ y ] = mtx[ x ][ y ] * scale;
				result.set(x, y, mtx.get(x, y, false), false);
			}
		}
		
	}
	
	Vector3f getEigenVectorFromLargestEigenValue(Matrix3f mtx ){
		float scale = findLargestMatrixElem( mtx );
		Matrix3f mc = new Matrix3f();
		/*aiMatrix3x3 mc = S*/scaleMatrix( mtx, 1.0f / scale, mc );
//		mc = mc * mc * mc;
		Matrix3f tmp = Matrix3f.mul(mc, mc, null);
		Matrix3f.mul(tmp, mc, mc);

//		aiVector3D v( 1.0f );
//		aiVector3D lastV = v;
		Vector3f v = new Vector3f(1, 1, 1);
		Vector3f lastV = new Vector3f(1, 1, 1);
		
		for ( int i = 0; i < 100; ++i )
		{
//			v = mc * v;
			Matrix3f.transform(mc, v, v);
			v.normalise( );
			if ( Vector3f.distanceSquare( v , lastV ) < 1e-16f )
			{
				break;
			}
			lastV.set(v);
		}
		return v;
	}
	
	
	PlaneP2T findLLSQPlane(List<PointP2T> points ){
		PlaneP2T result = new PlaneP2T();

		Vector3f sum = new Vector3f();
		for (int i = 0; i < points.size( ); ++i )
		{
//			sum += points[ i ].point3D;
			Vector3f.add(sum, points.get(i).point3D, sum);
		}
//		result.centre = sum * ( 1.0f / points.size( ) );
		Vector3f.scale(sum, 1.0f / points.size( ), result.centre);

		float sumXX = 0.0f;
		float sumXY = 0.0f;
		float sumXZ = 0.0f;
		float sumYY = 0.0f;
		float sumYZ = 0.0f;
		float sumZZ = 0.0f;
		for (int i = 0; i < points.size( ); ++i )
		{
//			aiVector3D offset = points[ i ].point3D - result.centre;
			Vector3f offset  =Vector3f.sub(points.get(i).point3D, result.centre, sum);
			sumXX += offset.x * offset.x;
			sumXY += offset.x * offset.y;
			sumXZ += offset.x * offset.z;
			sumYY += offset.y * offset.y;
			sumYZ += offset.y * offset.z;
			sumZZ += offset.z * offset.z;
		}

//		aiMatrix3x3 mtx( sumXX, sumXY, sumXZ, sumXY, sumYY, sumYZ, sumXZ, sumYZ, sumZZ );
		Matrix3f mtx = new Matrix3f();
		mtx.setRow(0, sumXX, sumXY, sumXZ);
		mtx.setRow(1, sumXY, sumYY, sumYZ);
		mtx.setRow(2, sumXZ, sumYZ, sumZZ);

		float det = mtx.determinant( );
		if ( det == 0.0f )
		{
//			result.normal = aiVector3D( 0.0f );
			result.normal.set(0, 0, 0);
		}
		else
		{
//			aiMatrix3x3 invMtx = mtx;
//			invMtx.Inverse( );
			Matrix3f.invert(mtx, mtx);
			result.normal.set(getEigenVectorFromLargestEigenValue( mtx ));
		}

		return result;
	}
	
	public static void main(String[] args) {
		byte[] bytes = {'A', 'B', 0, 0};
		String string = new String(bytes);
		System.out.println("length = " + string.length());
		System.out.println(string);
		string = string.trim();
		System.out.println("length = " + string.length());
		System.out.println(string);
	}
}
