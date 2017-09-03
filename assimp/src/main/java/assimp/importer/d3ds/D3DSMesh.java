package assimp.importer.d3ds;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/** Helper structure to represent a 3ds file mesh */
final class D3DSMesh{

	//! Vertex positions
	final List<Vector3f> mPositions = new ArrayList<Vector3f>();

	//! Face lists
	final List<D3DSFace> mFaces = new ArrayList<D3DSFace>();

	//! List of normal vectors
	final List<Vector3f> mNormals = new ArrayList<Vector3f>();
		
	//! Name of the mesh
	String mName;

	//! Texture coordinates
	final List<Vector3f> mTexCoords = new ArrayList<Vector3f>();

	//! Face materials
	final IntArrayList mFaceMaterials = new IntArrayList();

	//! Local transformation matrix
	Matrix4f mMat;
}
