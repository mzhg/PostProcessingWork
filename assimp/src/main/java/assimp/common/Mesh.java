package assimp.common;

import java.nio.FloatBuffer;
import java.util.Arrays;

/** A mesh represents a geometry or model with a single material. <p>
*
* It usually consists of a number of vertices and a series of primitives/faces 
* referencing the vertices. In addition there might be a series of bones, each 
* of them addressing a number of vertices with a certain weight. Vertex data 
* is presented in channels with each channel containing a single per-vertex 
* information such as a set of texture coords or a normal vector.
* If a data pointer is non-null, the corresponding data stream is present.
* From C++-programs you can also use the comfort functions has*() to
* test for the presence of various data streams.<p>
*
* A Mesh uses only a single material which is referenced by a material ID.
* @note The mPositions member is usually not optional. However, vertex positions 
* *could* be missing if the {@link #AI_SCENE_FLAGS_INCOMPLETE} flag is set in 
* <pre>
* Scene.mFlags
* </pre>
*/
public class Mesh implements Copyable<Mesh>{

	/** Maximum number of indices per face (polygon). */
	public static final int AI_MAX_FACE_INDICES = 0x7fff;
	
	/** Maximum number of indices per face (polygon). */
	public static final int AI_MAX_BONE_WEIGHTS = 0x7fffffff;
	
	/**  Maximum number of vertices per mesh. */
	public static final int AI_MAX_VERTICES = 0x7fffffff;
	
	/** Maximum number of faces per mesh. */
	public static final int AI_MAX_FACES = 0x7fffffff;
	
	/** Supported number of vertex color sets per mesh. */
	public static final int AI_MAX_NUMBER_OF_COLOR_SETS = 0x8;
	
	/** Supported number of texture coord sets (UV(W) channels) per mesh */
	public static final int AI_MAX_NUMBER_OF_TEXTURECOORDS = 0x8;
	
	/* @brief Enumerates the types of geometric primitives supported by Assimp.
	 *  
	 *  @see aiFace Face data structure
	 *  @see aiProcess_SortByPType Per-primitive sorting of meshes
	 *  @see aiProcess_Triangulate Automatic triangulation
	 *  @see AI_CONFIG_PP_SBP_REMOVE Removal of specific primitive types.
	 */
//	enum aiPrimitiveType
//	{
	/** A point primitive. <p>
	 *
	 * This is just a single vertex in the virtual world, 
	 * #aiFace contains just one index for such a primitive.
	 */
	public static final int aiPrimitiveType_POINT       = 0x1;

	/** A line primitive. <p>
	 *
	 * This is a line defined through a start and an end position.
	 * #aiFace contains exactly two indices for such a primitive.
	 */
	public static final int aiPrimitiveType_LINE        = 0x2;

	/** A triangular primitive. <p>
	 *
	 * A triangle consists of three indices.
	 */
	public static final int aiPrimitiveType_TRIANGLE    = 0x4;

	/** A higher-level polygon with more than 3 edges.<p>
	 *
	 * A triangle is a polygon, but polygon in this context means
	 * "all polygons that are not triangles". The "Triangulate"-Step
	 * is provided for your convenience, it splits all polygons in
	 * triangles (which are much easier to handle).
	 */
	public static final int aiPrimitiveType_POLYGON     = 0x8;
	
	/** Bitwise combination of the members of the #aiPrimitiveType enum.
	 * This specifies which types of primitives are present in the mesh.
	 * The "SortByPrimitiveType"-Step can be used to make sure the 
	 * output meshes consist of one primitive type each.
	 */
	public int mPrimitiveTypes;

	/** The number of vertices in this mesh. <p>
	* This is also the size of all of the per-vertex data arrays.
	* The maximum value for this member is #AI_MAX_VERTICES.
	*/
	public int mNumVertices;

	/** Vertex positions. <p>
	* This array is always present in a mesh. The array is 
	* mNumVertices in size. 
	*/
	public FloatBuffer mVertices;

	/** Vertex normals. 
	* The array contains normalized vectors, null if not present. 
	* The array is mNumVertices in size. Normals are undefined for
	* point and line primitives. A mesh consisting of points and
	* lines only may not have normal vectors. Meshes with mixed
	* primitive types (i.e. lines and triangles) may have normals,
	* but the normals for vertices that are only referenced by
	* point or line primitives are undefined and set to QNaN (WARN:
	* qNaN compares to inequal to *everything*, even to qNaN itself.
	* Using code like this to check whether a field is qnan is:
	* @code
	* #define IS_QNAN(f) (f != f)
	* @endcode
	* still dangerous because even 1.f == 1.f could evaluate to false! (
	* remember the subtleties of IEEE754 artithmetics). Use stuff like
	* @c fpclassify instead.
	* @note Normal vectors computed by Assimp are always unit-length.
	* However, this needn't apply for normals that have been taken
	*   directly from the model file.
	*/
	public FloatBuffer mNormals;

	/** Vertex tangents. 
	* The tangent of a vertex points in the direction of the positive 
	* X texture axis. The array contains normalized vectors, null if
	* not present. The array is mNumVertices in size. A mesh consisting 
	* of points and lines only may not have normal vectors. Meshes with 
	* mixed primitive types (i.e. lines and triangles) may have 
	* normals, but the normals for vertices that are only referenced by
	* point or line primitives are undefined and set to qNaN.  See
	* the #mNormals member for a detailled discussion of qNaNs.
	* @note If the mesh contains tangents, it automatically also 
	* contains bitangents.
	*/
	public FloatBuffer mTangents;

	/** Vertex bitangents. 
	* The bitangent of a vertex points in the direction of the positive 
	* Y texture axis. The array contains normalized vectors, null if not
	* present. The array is mNumVertices in size. 
	* @note If the mesh contains tangents, it automatically also contains
	* bitangents.  
	*/
	public FloatBuffer mBitangents;

	/** Vertex color sets. 
	* A mesh may contain 0 to #AI_MAX_NUMBER_OF_COLOR_SETS vertex 
	* colors per vertex. null if not present. Each array is
	* mNumVertices in size if present.
	*/
//	public Vector4f[][] mColors = new Vector4f[AI_MAX_NUMBER_OF_COLOR_SETS][];
	public final FloatBuffer[] mColors = new FloatBuffer[AI_MAX_NUMBER_OF_COLOR_SETS];

	/** Vertex texture coords, also known as UV channels.
	* A mesh may contain 0 to AI_MAX_NUMBER_OF_TEXTURECOORDS per
	* vertex. null if not present. The array is mNumVertices in size. 
	*/
//	public Vector3f[][] mTextureCoords = new Vector3f[AI_MAX_NUMBER_OF_TEXTURECOORDS][];
	public final FloatBuffer[] mTextureCoords = new FloatBuffer[AI_MAX_NUMBER_OF_TEXTURECOORDS];

	/** Specifies the number of components for a given UV channel.
	* Up to three channels are supported (UVW, for accessing volume
	* or cube maps). If the value is 2 for a given channel n, the
	* component p.z of mTextureCoords[n][p] is set to 0.0f.
	* If the value is 1 for a given channel, p.y is set to 0.0f, too.
	* @note 4D coords are not supported 
	*/
	public int mNumUVComponents[] = new int[AI_MAX_NUMBER_OF_TEXTURECOORDS];

	/** The faces the mesh isructed from. <p>
	* Each face refers to a number of vertices by their indices. 
	* This array is always present in a mesh, its size is given 
	* in mNumFaces. If the #AI_SCENE_FLAGS_NON_VERBOSE_FORMAT
	* is NOT set each face references an unique set of vertices.
	*/
	public Face[] mFaces;

	/** The bones of this mesh. <p>
	* A bone consists of a name by which it can be found in the
	* frame hierarchy and a set of vertex weights.
	*/
	public Bone[] mBones;

	/** The material used by this mesh. <p>
	 * A mesh does use only a single material. If an imported model uses
	 * multiple materials, the import splits up the mesh. Use this value 
	 * as index into the scene's material list.
	 */
	public int mMaterialIndex;

	/** Name of the mesh. Meshes can be named, but this is not a
	 *  requirement and leaving this field empty is totally fine.
	 *  There are mainly three uses for mesh names: <ul>
	 *  <li> some formats name nodes and meshes independently.
	 *  <li> importers tend to split meshes up to meet the
	 *      one-material-per-mesh requirement. Assigning
	 *      the same (dummy) name to each of the result meshes
	 *      aids the caller at recovering the original mesh
	 *      partitioning.
	 *  <li> Vertex animations refer to meshes by their names.
	 *  </ul>
	 **/
	public String mName;


	/** NOT CURRENTLY IN USE. The number of attachment meshes */
	public int mNumAnimMeshes;

	/** NOT CURRENTLY IN USE. Attachment meshes for this mesh, for vertex-based animation. 
	 *  Attachment meshes carry replacement data for some of the
	 *  mesh'es vertex components (usually positions, normals). */
	public AnimMesh[] mAnimMeshes;
	
	/** Internal Use. */
	public Object tag;
	public int msg1;
	
	public Mesh() {
//		for(int i = 0; i < mTextureCoords.length; i++)
//			mTextureCoords[i] = new Vector3f();
//		
//		for(int i = 0; i < mColors.length; i++)
//			mColors[i] = new Vector4f();
	}
	
	@Override
	public Mesh copy() {
		Mesh mesh = new Mesh();
		mesh.mVertices = AssUtil.copyOf(mVertices);
		mesh.mBitangents = AssUtil.copyOf(mBitangents);
		mesh.mNormals = AssUtil.copyOf(mNormals);
		mesh.mTangents = AssUtil.copyOf(mTangents);
		
		int n = 0;
		while(mTextureCoords[n] != null){
			mesh.mTextureCoords[n] = AssUtil.copyOf(mTextureCoords[n]);
			n++;
		}
		
		n = 0;
		while(mColors[n] != null){
			mesh.mColors[n] = AssUtil.copyOf(mColors[n]);
			n++;
		}
		
		mesh.mBones = AssUtil.copyOf(mBones);
		mesh.mFaces = AssUtil.copyOf(mFaces);
		
		mesh.mAnimMeshes = mAnimMeshes;
		mesh.mMaterialIndex = mMaterialIndex;
		mesh.mName = mName;
		mesh.mNumAnimMeshes = mNumAnimMeshes;
		if(mNumUVComponents != null)
			mesh.mNumUVComponents = Arrays.copyOf(mNumUVComponents, mNumUVComponents.length);
		mesh.mNumVertices = mNumVertices;
		mesh.mPrimitiveTypes = mPrimitiveTypes;
		return mesh;
	}
	
	/** Check whether the mesh contains positions. Provided no special
	 * scene flags are set (such as #AI_SCENE_FLAGS_ANIM_SKELETON_ONLY), 
	 * this will always be true */
	public boolean hasPositions() 
		{ return mVertices != null && mNumVertices > 0; }

	/** Check whether the mesh contains faces. If no special scene flags
	 * are set this should always return true */
	public boolean hasFaces() 
		{ return mFaces != null && mFaces.length > 0; }
	
	/** Return the number of primitives (triangles, polygons, lines) in this  mesh. 
	* This is also the size of the mFaces array.<p>
	* The maximum value for this member is #AI_MAX_FACES.
	*/
	public int getNumFaces(){ return mFaces != null ? mFaces.length : 0;}

	/** Check whether the mesh contains normal vectors */
	public boolean hasNormals() 
		{ return mNormals != null && mNumVertices > 0; }

	/** Check whether the mesh contains tangent and bitangent vectors
	 * It is not possible that it contains tangents and no bitangents
	 * (or the other way round). The existence of one of them
	 * implies that the second is there, too.
	 */
	public boolean hasTangentsAndBitangents() 
		{ return mTangents != null && mBitangents != null && mNumVertices > 0; }

	/** Check whether the mesh contains a vertex color set
	 * @param pIndex Index of the vertex color set
	 */
	public boolean hasVertexColors( int pIndex)
	{ 
		if( pIndex >= AI_MAX_NUMBER_OF_COLOR_SETS) 
			return false; 
		else 
			return mColors[pIndex] != null && mNumVertices > 0; 
	}

	/** Check whether the mesh contains a texture coordinate set
	 * @param pIndex Index of the texture coordinates set
	 */
	public boolean hasTextureCoords( int pIndex)
	{ 
		if( pIndex >= AI_MAX_NUMBER_OF_TEXTURECOORDS) 
			return false; 
		else 
			return mTextureCoords[pIndex] != null && mNumVertices > 0; 
	}

	/** Get the number of UV channels the mesh contains */
	public int getNumUVChannels() 
	{
		int n = 0;
		while (n < AI_MAX_NUMBER_OF_TEXTURECOORDS && mTextureCoords[n] != null)++n;
		return n;
	}

	/** Get the number of vertex color channels the mesh contains */
	public int getNumColorChannels() 
	{
		int n = 0;
		while (n < AI_MAX_NUMBER_OF_COLOR_SETS && mColors[n] != null)++n;
		return n;
	}

	/** Check whether the mesh contains bones */
	public boolean hasBones()
		{ return mBones != null && mBones.length > 0; }
	
	/** Return the number of bones this mesh contains. 
	* Can be 0, in which case the mBones array is null. 
	*/
	public int getNumBones(){return mBones != null ? mBones.length : 0;}
	
}
