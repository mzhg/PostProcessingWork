package assimp.importer.collada;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import assimp.common.Mesh;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/** Contains data for a single mesh */
final class COLMesh {

	String mName;

	// just to check if there's some sophisticated addressing involved...
	// which we don't support, and therefore should warn about.
	String mVertexID; 

	// Vertex data addressed by vertex indices
	final ArrayList<InputChannel> mPerVertexData = new ArrayList<>(); 

	// actual mesh data, assembled on encounter of a <p> element. Verbose format, not indexed
	FloatBuffer mPositions;
	FloatBuffer mNormals;
	FloatBuffer mTangents;
	FloatBuffer mBitangents;
	final FloatBuffer[] mTexCoords = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];
	final FloatBuffer[] mColors = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_COLOR_SETS];

	final int[] mNumUVComponents = new int[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];

	// Faces. Stored are only the number of vertices for each face.
	// 1 == point, 2 == line, 3 == triangle, 4+ == poly
	IntArrayList mFaceSize;
	
	// Position indices for all faces in the sequence given in mFaceSize - 
	// necessary for bone weight assignment
	IntArrayList mFacePosIndices;

	// Submeshes in this mesh, each with a given material
	final ArrayList<SubMesh> mSubMeshes = new ArrayList<>();
}
