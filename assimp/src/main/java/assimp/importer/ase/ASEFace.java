package assimp.importer.ase;

import assimp.common.Mesh;
import assimp.importer.d3ds.D3DSFace;

/** Helper structure to represent an ASE file face */
final class ASEFace extends D3DSFace{

	//! special value to indicate that no material index has
	//! been assigned to a face. The default material index
	//! will replace this value later.
	static final int DEFAULT_MATINDEX = 0xFFFFFFFF;

	//! Indices into each list of texture coordinates
	final int[][] amUVIndices = new int[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS][3];

	//! Index into the list of vertex colors
	final int[] mColorIndices = new int[3];

	//! (Sub)Material index to be assigned to this face
	int iMaterial = DEFAULT_MATINDEX;

	//! Index of the face. It is not specified whether it is
	//! a requirement of the file format that all faces are
	//! written in sequential order, so we have to expect this case
	int iFace;
}
