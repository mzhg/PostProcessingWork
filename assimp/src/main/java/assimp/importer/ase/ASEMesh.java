package assimp.importer.ase;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import assimp.common.Mesh;

/** Helper structure to represent an ASE file mesh */
final class ASEMesh extends BaseNode{

	//! Vertex positions
	FloatBuffer mPositions;

	//! Face lists
	final List<ASEFace> mFaces = new ArrayList<ASEFace>();

	//! List of normal vectors
	FloatBuffer mNormals;
	
	//! List of all texture coordinate sets
	final FloatBuffer[] amTexCoords = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];

	//! List of all vertex color sets.
	FloatBuffer mVertexColors;

	//! List of all bone vertices
	final List<BoneVertex> mBoneVertices = new ArrayList<BoneVertex>();

	//! List of all bones
	final List<ASEBone> mBones = new ArrayList<ASEBone>();

	//! Material index of the mesh
	int iMaterialIndex;

	//! Number of vertex components for each UVW set
	final int[] mNumUVComponents = new int[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];

	//! used internally
	boolean bSkip;
	
	public ASEMesh() {
		super(MESH);
		
		Arrays.fill(mNumUVComponents, 2);
		// setup the default material index by default
		iMaterialIndex = ASEFace.DEFAULT_MATINDEX;
	}
	
	int getNumPostions() { return mPositions != null ? mPositions.remaining()/3 : 0;}
}
