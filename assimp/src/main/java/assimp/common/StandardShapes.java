package assimp.common;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

/**
 * Helper class to generate vertex buffers for standard geometric
 * shapes, such as cylinders, cones, boxes, spheres, elipsoids ... .
 */
public final class StandardShapes {
	
	private StandardShapes(){}
	
	public interface GenerateFunc1{
		int generate(List<Vector3f> positions);
	}
	
	public interface GenerateFunc2{
		int generate(List<Vector3f> positions, boolean b);
	}
	
	public interface GenerateFunc3{
		void generate(int num, List<Vector3f> positions);
	}

	// ----------------------------------------------------------------
	/** Generates a mesh from an array of vertex positions.
	 *
	 *  @param positions List of vertex positions
	 *  @param numIndices Number of indices per primitive
	 *  @return Output mesh
	 */
	public static Mesh makeMesh(List<Vector3f> positions, int numIndices){
		if(positions == null || positions.isEmpty() || numIndices == 0)
			return null;

		// Determine which kinds of primitives the mesh consists of
		Mesh out = new Mesh();
		switch (numIndices)
		{
		case 1: 
			out.mPrimitiveTypes = Mesh.aiPrimitiveType_POINT;
			break;
		case 2:
			out.mPrimitiveTypes = Mesh.aiPrimitiveType_LINE;
			break;
		case 3:
			out.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;
			break;
		default:
			out.mPrimitiveTypes = Mesh.aiPrimitiveType_POLYGON;
			break;
		};

//		out.mNumFaces = (unsigned int)positions.size() / numIndices;
		out.mFaces = new Face[positions.size()/numIndices];
		for (int i = 0, a = 0; i < out.mFaces.length;++i)
		{
			Face f = out.mFaces[i] = Face.createInstance(numIndices);
//			f.mNumIndices = numIndices;
//			f.mIndices = new unsigned int[numIndices];
			for (int j = 0; j < numIndices;++j,++a)
//				f.mIndices[i] = a;
				f.set(j, a);
		}
		out.mNumVertices = positions.size();
//		out.mVertices = new Vector3f[out.mNumVertices];
		out.mVertices = MemoryUtil.createFloatBuffer(out.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
//		::memcpy(out->mVertices,&positions[0],out->mNumVertices*sizeof(new Vector3f));
		for(Vector3f v : positions){
			v.store(out.mVertices);
		}
		out.mVertices.flip();
		return out;
	}

	public static Mesh makeMesh ( GenerateFunc1 func){
		List<Vector3f> positions =new ArrayList<Vector3f>();
		int num = func.generate(positions);
		return makeMesh(positions, num);
	}

	public static Mesh makeMesh( GenerateFunc2 func){
		List<Vector3f> positions =new ArrayList<Vector3f>();
		int num = func.generate(positions, true);
		return makeMesh(positions, num);
	}

	public static Mesh makeMesh ( int n,  GenerateFunc3 func){
		List<Vector3f> positions =new ArrayList<Vector3f>();
		func.generate(n, positions);
		return makeMesh(positions, 3);
	}
	
	private static void add_triangle(List<Vector3f> positions, Vector3f n0, Vector3f n1, Vector3f n2){
		positions.add(n0);
		positions.add(n1);
		positions.add(n2);
	}
	
	private static void add_pentagon(List<Vector3f> positions, boolean polygons, Vector3f n0, Vector3f n1, Vector3f n2, Vector3f n3, Vector3f n4){
		if(polygons){
			positions.add(n0);
			positions.add(n1);
			positions.add(n2);
			positions.add(n3);
			positions.add(n4);
		}else{
			add_triangle(positions, n0, n1, n2);
			add_triangle(positions, n0, n2, n3);
			add_triangle(positions, n0, n3, n4);
		}
	}
	
	private static void add_quad(List<Vector3f> positions, boolean polygons, Vector3f n0, Vector3f n1, Vector3f n2, Vector3f n3){
		if(polygons){
			positions.add(n0);
			positions.add(n1);
			positions.add(n2);
			positions.add(n3);
		}else{
			add_triangle(positions, n0, n1, n2);
			add_triangle(positions, n0, n2, n3);
		}
	}
	
	// Fast subdivision for a mesh whose verts have a magnitude of 1
	static void subdivide(List<Vector3f> positions)
	{
		// assume this to be constant - (fixme: must be 1.0? I think so)
		final float fl1 = positions.get(0).length();
		int origSize = positions.size();
		for (int i = 0 ; i < origSize ; i+=3)
		{
			Vector3f tv0 = positions.get(i);
			Vector3f tv1 = positions.get(i+1);
			Vector3f tv2 = positions.get(i+2);

			Vector3f a = new Vector3f(tv0), b = new Vector3f(tv1), c = new Vector3f(tv2);
			Vector3f v1 = new Vector3f(a.x+b.x, a.y+b.y, a.z+b.z);
			v1.normalise(); v1.scale(fl1);
			Vector3f v2 = new Vector3f(a.x+c.x, a.y+c.y, a.z+c.z);
			v2.normalise(); v2.scale(fl1);
			Vector3f v3 = new Vector3f(b.x+c.x, b.y+c.y, b.z+c.z);
			v3.normalise(); v3.scale(fl1);

			tv0.set(v1); tv1.set(v3); tv2.set(v2); // overwrite the original
			add_triangle(positions, v1, v2, a);
			add_triangle(positions, v2, v3, c);
			add_triangle(positions, v3, v1, b);
		}
	}

	// ----------------------------------------------------------------
	/** 
	 *  Generates a hexahedron (cube)<p>
	 *
	 *  Hexahedrons can be scaled on all axes.
	 *  @param positions Receives output triangles.
	 *  @param polygons If you pass true here quads will be returned
	 *  @return Number of vertices per face
	 */
	public static int makeHexahedron( List<Vector3f> positions, boolean polygons/* = false*/){
		final float length = 1.f/1.73205080f;

		Vector3f v0  = new Vector3f(-1.f,-1.f,-1.f);
		Vector3f v1  = new Vector3f(1.f,-1.f,-1.f);
		Vector3f v2  = new Vector3f(1.f,1.f,-1.f);
		Vector3f v3  = new Vector3f(-1.f,1.f,-1.f);
		Vector3f v4  = new Vector3f(-1.f,-1.f,1.f);
		Vector3f v5  = new Vector3f(1.f,-1.f,1.f);
		Vector3f v6  = new Vector3f(1.f,1.f,1.f);
		Vector3f v7  = new Vector3f(-1.f,1.f,1.f);
		
		v0.scale(length);
		v1.scale(length);
		v2.scale(length);
		v3.scale(length);
		v4.scale(length);
		v5.scale(length);
		v6.scale(length);
		v7.scale(length);

		add_quad(positions,polygons,v0,v3,v2,v1);
		add_quad(positions,polygons,v0,v1,v5,v4);
		add_quad(positions,polygons,v0,v4,v7,v3);
		add_quad(positions,polygons,v6,v5,v1,v2);
		add_quad(positions,polygons,v6,v2,v3,v7);
		add_quad(positions,polygons,v6,v7,v4,v5);
		return (polygons ? 4 : 3);
	}

	// ----------------------------------------------------------------
	/** @brief Generates an icosahedron
	 *
	 *  @param positions Receives output triangles.
	 *  @return Number of vertices per face
	 */
	public static int makeIcosahedron(List<Vector3f> positions){
		final float t = (1.f + 2.236067977f)/2.f;
		final float s = (float) Math.sqrt(1.f + t*t);
		
		Vector3f v0  = new Vector3f(t,1.f, 0.f);
		Vector3f v1  = new Vector3f(-t,1.f, 0.f);
		Vector3f v2  = new Vector3f(t,-1.f, 0.f);
		Vector3f v3  = new Vector3f(-t,-1.f, 0.f);
		Vector3f v4  = new Vector3f(1.f, 0.f, t);
		Vector3f v5  = new Vector3f(1.f, 0.f,-t);
		Vector3f v6  = new Vector3f(-1.f, 0.f,t);
		Vector3f v7  = new Vector3f(-1.f, 0.f,-t);
		Vector3f v8  = new Vector3f(0.f, t, 1.f);
		Vector3f v9  = new Vector3f(0.f,-t, 1.f);
		Vector3f v10 = new Vector3f(0.f, t,-1.f);
		Vector3f v11 = new Vector3f(0.f,-t,-1.f);
		
		float _s = 1.0f/s;
		v0.scale(_s);
		v1.scale(_s);
		v2.scale(_s);
		v3.scale(_s);
		v4.scale(_s);
		v5.scale(_s);
		v6.scale(_s);
		v7.scale(_s);
		v8.scale(_s);
		v9.scale(_s);
		v10.scale(_s);
		v11.scale(_s);
 
		List<Vector3f> pos;
		if(positions.isEmpty())
			pos = positions;
		else
			pos = new ArrayList<Vector3f>(60);
		add_triangle(pos,v0,v8,v4);
		add_triangle(pos,v0,v5,v10);
		add_triangle(pos,v2,v4,v9);
		add_triangle(pos,v2,v11,v5);

		add_triangle(pos,v1,v6,v8);
		add_triangle(pos,v1,v10,v7);
		add_triangle(pos,v3,v9,v6);
		add_triangle(pos,v3,v7,v11);

		add_triangle(pos,v0,v10,v8);
		add_triangle(pos,v1,v8,v10);
		add_triangle(pos,v2,v9,v11);
		add_triangle(pos,v3,v11,v9);

		add_triangle(pos,v4,v2,v0);
		add_triangle(pos,v5,v0,v2);
		add_triangle(pos,v6,v1,v3);
		add_triangle(pos,v7,v3,v1);

		add_triangle(pos,v8,v6,v4);
		add_triangle(pos,v9,v4,v6);
		add_triangle(pos,v10,v5,v7);
		add_triangle(pos,v11,v7,v5);
		
		if(positions != pos)
			positions.addAll(pos);
		return 3;
	}


	// ----------------------------------------------------------------
	/** @brief Generates a dodecahedron
	 *
	 *  @param positions Receives output triangles
	 *  @param polygons If you pass true here pentagons will be returned
	 *  @return Number of vertices per face
	 */
	public static int makeDodecahedron(List<Vector3f> positions, boolean polygons/* = false*/){
		final float a = 1.f / 1.7320508f;
		final float b = (float) Math.sqrt((3.f-2.23606797f)/6.f);
		final float c = (float) Math.sqrt((3.f+2.23606797f)/6.f);

		Vector3f v0  = new Vector3f(a,a,a);
		Vector3f v1  = new Vector3f(a,a,-a);
		Vector3f v2  = new Vector3f(a,-a,a);
		Vector3f v3  = new Vector3f(a,-a,-a);
		Vector3f v4  = new Vector3f(-a,a,a);
		Vector3f v5  = new Vector3f(-a,a,-a);
		Vector3f v6  = new Vector3f(-a,-a,a);
		Vector3f v7  = new Vector3f(-a,-a,-a);
		Vector3f v8  = new Vector3f(b,c,0.f);
		Vector3f v9  = new Vector3f(-b,c,0.f);
		Vector3f v10 = new Vector3f(b,-c,0.f);
		Vector3f v11 = new Vector3f(-b,-c,0.f);
		Vector3f v12 = new Vector3f(c, 0.f, b);
		Vector3f v13 = new Vector3f(c, 0.f, -b);
		Vector3f v14 = new Vector3f(-c, 0.f, b);
		Vector3f v15 = new Vector3f(-c, 0.f, -b);
		Vector3f v16 = new Vector3f(0.f, b, c);
		Vector3f v17 = new Vector3f(0.f, -b, c);
		Vector3f v18 = new Vector3f(0.f, b, -c);
		Vector3f v19 = new Vector3f(0.f, -b, -c);
		
		List<Vector3f> pos;
		if(positions.isEmpty())
			pos = positions;
		else
			pos = new ArrayList<Vector3f>(108);

		add_pentagon(pos,polygons,v0, v8, v9, v4, v16);
		add_pentagon(pos,polygons,v0, v12, v13, v1, v8);
		add_pentagon(pos,polygons,v0, v16, v17, v2, v12);
		add_pentagon(pos,polygons,v8, v1, v18, v5, v9);
		add_pentagon(pos,polygons,v12, v2, v10, v3, v13);
		add_pentagon(pos,polygons,v16, v4, v14, v6, v17);
		add_pentagon(pos,polygons,v9, v5, v15, v14, v4);

		add_pentagon(pos,polygons,v6, v11, v10, v2, v17);
		add_pentagon(pos,polygons,v3, v19, v18, v1, v13);
		add_pentagon(pos,polygons,v7, v15, v5, v18, v19);
		add_pentagon(pos,polygons,v7, v11, v6, v14, v15);
		add_pentagon(pos,polygons,v7, v19, v3, v10, v11);
		
		if(pos != positions)
			positions.addAll(pos);
		return (polygons ? 5 : 3);
	}


	// ----------------------------------------------------------------
	/** @brief Generates an octahedron
	 *
	 *  @param positions Receives output triangles.
	 *  @return Number of vertices per face
	 */
	public static int makeOctahedron(List<Vector3f> positions){
		Vector3f v0  = new Vector3f(1.0f, 0.f, 0.f) ;
		Vector3f v1  = new Vector3f(-1.0f, 0.f, 0.f);
		Vector3f v2  = new Vector3f(0.f, 1.0f, 0.f);
		Vector3f v3  = new Vector3f(0.f, -1.0f, 0.f);
		Vector3f v4  = new Vector3f(0.f, 0.f, 1.0f);
		Vector3f v5  = new Vector3f(0.f, 0.f, -1.0f);

		add_triangle(positions,v4,v0,v2);
		add_triangle(positions,v4,v2,v1);
		add_triangle(positions,v4,v1,v3);
		add_triangle(positions,v4,v3,v0);

		add_triangle(positions,v5,v2,v0);
		add_triangle(positions,v5,v1,v2);
		add_triangle(positions,v5,v3,v1);
		add_triangle(positions,v5,v0,v3);
		return 3;
	}


	// ----------------------------------------------------------------
	/** @brief Generates a tetrahedron
	 *
	 *  @param positions Receives output triangles.
	 *  @return Number of vertices per face
	 */
	public static int makeTetrahedron(List<Vector3f> positions){
		final float a = 1.41421f/3.f;
		final float b = 2.4494f/3.f;

		Vector3f v0  = new Vector3f(0.f,0.f,1.f);
		Vector3f v1  = new Vector3f(2*a,0,-1.f/3.f);
		Vector3f v2  = new Vector3f(-a,b,-1.f/3.f);
		Vector3f v3  = new Vector3f(-a,-b,-1.f/3.f);

		add_triangle(positions,v0,v1,v2);
		add_triangle(positions,v0,v2,v3);
		add_triangle(positions,v0,v3,v1);
		add_triangle(positions,v1,v3,v2);
		return 3;
	}



	// ----------------------------------------------------------------
	/** @brief Generates a sphere
	 *
	 *  @param tess Number of subdivions - 0 generates a octahedron
	 *  @param positions Receives output triangles.
	 */
	public static void makeSphere( int tess, List<Vector3f> positions){
		// Reserve enough storage. Every subdivision
		// splits each triangle in 4, the icosahedron consists of 60 verts
//		positions.reserve(positions.size()+60 * integer_pow(4, tess));
		List<Vector3f> pos = new ArrayList<Vector3f>((int)(60 + Math.pow(4, tess)));

		// Construct an icosahedron to start with 
		makeIcosahedron(pos);

		positions.addAll(pos);
		// ... and subdivide it until the requested output
		// tesselation is reached
		for (int i = 0; i<tess;++i)
			subdivide(positions);
	}


	// ----------------------------------------------------------------
	/** 
	 * Generates a cone or a cylinder, either open or closed.
	 *
	 *  <pre>
	 *
	 *       |-----|       <- radius 1
	 *
	 *        __x__        <- ]               ^
	 *       /     \          | height        |
	 *      /       \         |               Y                 
	 *     /         \        |
	 *    /	          \       |
	 *   /______x______\   <- ] <- end cap
	 *
	 *   |-------------|   <- radius 2
	 *
	 *  </pre>
	 *
	 *  @param height Height of the cone
	 *  @param radius1 First radius
	 *  @param radius2 Second radius
	 *  @param tess Number of triangles.
	 *  @param bOpened true for an open cone/cylinder. An open shape has
	 *    no 'end caps'
	 *  @param positions Receives output triangles
	 */
	public static void makeCone(float height,float radius1, float radius2, int tess, List<Vector3f> positions,boolean bOpen){
		// Sorry, a cone with less than 3 segments makes ABSOLUTELY NO SENSE
		if (tess < 3 || height == 0)
			return;

		int old = positions.size();

		// No negative radii
		radius1 = Math.abs(radius1);
		radius2 = Math.abs(radius2);

		float halfHeight = height / 2;

		// radius1 is always the smaller one 
		if (radius2 > radius1)
		{
//			std::swap(radius2,radius1);
			float t = radius2;
			radius2 = radius1;
			radius1 = t;
			halfHeight = -halfHeight;
		}
		else old = -1;

		// Use a large epsilon to check whether the cone is pointy
		if (radius1 < (radius2-radius1)*10e-3f)radius1 = 0.f;

		// We will need 3*2 verts per segment + 3*2 verts per segment
		// if the cone is closed
		final int mem = tess*6 + (!bOpen ? tess*3 * (radius1!=0 ? 2 : 1) : 0);
//		positions.reserve(positions.size () + mem);
		List<Vector3f> pos = new ArrayList<Vector3f>(mem);

		// Now construct all segments
		float angle_delta = (float)Math.PI * 2.0f / tess;
		float angle_max   = (float)Math.PI * 2.0f;

		float s = 1.f; // cos(angle == 0);
		float t = 0.f; // sin(angle == 0);

		for (float angle = 0.f; angle < angle_max; )
		{
			Vector3f v1 = new Vector3f(s * radius1, -halfHeight, t * radius1 );
			Vector3f v2 = new Vector3f(s * radius2,  halfHeight, t * radius2 );

			final float next = angle + angle_delta;
			float s2 = (float) Math.cos(next);
			float t2 = (float) Math.sin(next);

			final Vector3f v3 = new Vector3f (s2 * radius2,  halfHeight, t2 * radius2 );
			final Vector3f v4 = new Vector3f (s2 * radius1, -halfHeight, t2 * radius1 );

			pos.add(v1);
			pos.add(v2);
			pos.add(v3);
			pos.add(v4);
			pos.add(v1);
			pos.add(v3);

			if (!bOpen)
			{
				// generate the end 'cap'
				pos.add(new Vector3f(s * radius2,  halfHeight, t * radius2 ));
				pos.add(new Vector3f(s2 * radius2,  halfHeight, t2 * radius2 ));
				pos.add(new Vector3f(0.f, halfHeight, 0.f));
				

				if (radius1 > 0)
				{
					// generate the other end 'cap'
					pos.add(new Vector3f(s * radius1,  -halfHeight, t * radius1 ));
					pos.add(new Vector3f(s2 * radius1,  -halfHeight, t2 * radius1 ));
					pos.add(new Vector3f(0.f, -halfHeight, 0.f));
					
				}
			}
			s = s2;
			t = t2;
			angle = next;
		}

		positions.addAll(pos);
		// Need to flip face order?
		if ( -1 != old )	{
			for (int k = old; k < positions.size();k += 3) {
//				std::swap(positions[s],positions[s+1]);
				Vector3f v = positions.get(k);
				Vector3f u = positions.get(k + 1);
				
				positions.set(k, u);
				positions.set(k + 1, v);
			}
		}
	}


	// ----------------------------------------------------------------
	/** @brief Generates a flat circle
	 *
	 *  The circle is constructed in the planed formed by the x,z
	 *  axes of the cartesian coordinate system.
	 *  
	 *  @param radius Radius of the circle
	 *  @param tess Number of segments.
	 *  @param positions Receives output triangles.
	 */
	public static void makeCircle(float radius, int tess, List<Vector3f> positions){
		// Sorry, a circle with less than 3 segments makes ABSOLUTELY NO SENSE
		if (tess < 3 || radius == 0)
			return;

		radius = Math.abs(radius);

		// We will need 3 vertices per segment 
//		positions.reserve(positions.size()+);
		List<Vector3f> pos;
		int capacity = tess*3;
		if(capacity > 30){
			pos = new ArrayList<Vector3f>(capacity);
		}else
			pos = positions;

		float angle_delta = (float)Math.PI * 2.0f / tess;
		float angle_max   = (float)Math.PI * 2.0f;

		float s = 1.f; // cos(angle == 0);
		float t = 0.f; // sin(angle == 0);

		for (float angle = 0.f; angle < angle_max;  )
		{
			pos.add(new Vector3f(s * radius,0.f,t * radius));
			angle += angle_delta;
			s = (float) Math.cos(angle);
			t = (float) Math.sin(angle);
			pos.add(new Vector3f(s * radius,0.f,t * radius));
			pos.add(new Vector3f(0.f,0.f,0.f));
		}
		
		positions.addAll(pos);
	}
}
