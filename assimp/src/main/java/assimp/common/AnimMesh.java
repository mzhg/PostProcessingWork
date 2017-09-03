package assimp.common;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/** An AnimMesh is an attachment to an <code>Mesh</code> stores per-vertex 
 *  animations for a particular frame.<p>
 *  
 *  You may think of an <code>AnimMesh</code> as a `patch` for the host mesh, which
 *  replaces only certain vertex data streams at a particular time. 
 *  Each mesh stores n attached attached meshes (#aiMesh::mAnimMeshes).
 *  The actual relationship between the time line and anim meshes is 
 *  established by #aiMeshAnim, which references singular mesh attachments
 *  by their ID and binds them to a time offset.
*/
public class AnimMesh{

	/** Replacement for aiMesh::mVertices. If this array is non-null, 
	 *  it <b>must</b> contain mNumVertices entries. The corresponding
	 *  array in the host mesh must be non-null as well - animation
	 *  meshes may neither add or nor remove vertex components (if
	 *  a replacement array is null and the corresponding source
	 *  array is not, the source data is taken instead)*/
	public FloatBuffer mVertices;

	/** Replacement for Mesh::mNormals.  */
	public FloatBuffer mNormals;

	/** Replacement for Mesh::mTangents. */
	public FloatBuffer mTangents;

	/** Replacement for Mesh::mBitangents. */
	public FloatBuffer mBitangents;

	/** Replacement for Mesh::mColors */
	public Vector4f[] mColors = new Vector4f[Mesh.AI_MAX_NUMBER_OF_COLOR_SETS];

	/** Replacement for aiMesh::mTextureCoords */
	public Vector3f[] mTextureCoords = new Vector3f[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];

	/** The number of vertices in the aiAnimMesh, and thus the length of all
	 * the member arrays.<p>
	 *
	 * This has always the same value as the mNumVertices property in the
	 * corresponding aiMesh. It is duplicated here merely to make the length
	 * of the member arrays accessible even if the aiMesh is not known, e.g.
	 * from language bindings.
	 */
	public int mNumVertices;
	
	public AnimMesh() {
	}
	
	/** Check whether the anim mesh overrides the vertex positions 
	 *  of its host mesh*/ 
	public boolean hasPositions() {return mVertices != null;}

	/** Check whether the anim mesh overrides the vertex normals
	 *  of its host mesh*/ 
	public boolean hasNormals() {  return mNormals != null; }

	/** Check whether the anim mesh overrides the vertex tangents
	 *  and bitangents of its host mesh. As for aiMesh,
	 *  tangents and bitangents always go together. */ 
	public boolean hasTangentsAndBitangents() { return mTangents != null; }

	/** Check whether the anim mesh overrides a particular
	 * set of vertex colors on his host mesh. 
	 *  @param pIndex 0<index<AI_MAX_NUMBER_OF_COLOR_SETS */ 
	public boolean hasVertexColors( int pIndex)	{ 
		return pIndex >= Mesh.AI_MAX_NUMBER_OF_COLOR_SETS ? false : mColors[pIndex] != null; 
	}

	/** Check whether the anim mesh overrides a particular
	 * set of texture coordinates on his host mesh. 
	 *  @param pIndex 0<index<AI_MAX_NUMBER_OF_TEXTURECOORDS */ 
	public boolean hasTextureCoords( int pIndex)	{ 
		return pIndex >= Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS ? false : mTextureCoords[pIndex] != null; 
	}
}
