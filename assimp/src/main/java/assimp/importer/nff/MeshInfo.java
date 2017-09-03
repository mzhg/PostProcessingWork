package assimp.importer.nff;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.nio.FloatBuffer;

import org.lwjgl.util.vector.Vector3f;

final class MeshInfo {

	static final int 
		PatchType_Simple = 0x0,
		PatchType_Normals = 0x1,
		PatchType_UVAndNormals = 0x2;
	
	ShadingInfo shader;
	byte pType;
	boolean bLocked;

	// for spheres, cones and cylinders: center point of the object
	final Vector3f center = new Vector3f();
	final Vector3f radius = new Vector3f(1.f,1.f,1.f);
	final Vector3f dir = new Vector3f(0.f,1.f,0.f);

	final byte[] name = new byte[128];

	FloatBuffer vertices, normals, uvs;
	IntArrayList faces;

	// for NFF2
	FloatBuffer  colors; 
	int matIndex;
	
	public MeshInfo(byte type, boolean bL) {
		pType = type;
		bLocked = bL;
	}
}
