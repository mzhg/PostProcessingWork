package assimp.importer.obj;

import java.util.List;

final class ObjMesh {

	static final int NO_MATERIAL = ~0;

	///	Array with pointer to all stored faces
	List<ObjFace> m_Faces;
	///	Assigned material
	ObjMaterial m_pMaterial;
	///	Number of stored indices.
	int m_uiNumIndices;
	/// Number of UV
	int[] m_uiUVCoordinates = new int[assimp.common.Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS ];
	///	Material index.
	int m_uiMaterialIndex = NO_MATERIAL;
	///	True, if normals are stored.
	boolean m_hasNormals;
}
