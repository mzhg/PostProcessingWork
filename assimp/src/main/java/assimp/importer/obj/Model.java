package assimp.importer.obj;

import it.unimi.dsi.fastutil.ints.IntList;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Data structure to store all obj-specific model datas */
/*public*/final class Model {

	//!	Model name
	String m_ModelName;
	//!	List ob assigned objects
	List<ObjObject> m_Objects;
	//!	Pointer to current object
	ObjObject m_pCurrent;
	//!	Pointer to current material
	ObjMaterial m_pCurrentMaterial;
	//!	Pointer to default material
	ObjMaterial m_pDefaultMaterial;
	//!	Vector with all generated materials
	final List<String> m_MaterialLib = new ArrayList<String>();
	//!	Vector with all generated group
	List<String> m_GroupLib;
	//!	Vector with all generated vertices
	FloatBuffer m_Vertices;
	//!	vector with all generated normals
	FloatBuffer m_Normals;
	//!	Group map
	Map<String, IntList> m_Groups;
	//!	Group to face id assignment
	IntList m_pGroupFaceIDs;
	//!	Active group
	String m_strActiveGroup;
	//!	Vector with generated texture coordinates
	FloatBuffer m_TextureCoord;
	//!	Current mesh instance
	ObjMesh m_pCurrentMesh;
	//!	Vector with stored meshes
	List<ObjMesh> m_Meshes;
	//!	Material map
	final Map<String, ObjMaterial> m_MaterialMap = new HashMap<String, ObjMaterial>();
	
	int _getNumVertices() { return m_Vertices != null ? m_Vertices.position()/3 : 0;}
	int getNumVertices() { return m_Vertices != null ? m_Vertices.remaining()/3 : 0;}
	int _getNumNormals() { return m_Normals != null ? m_Normals.position()/3 : 0;}
	int getNumNormals() { return m_Normals != null ? m_Normals.remaining()/3 : 0;}
	int _getNumTextureCoords() { return m_TextureCoord != null ? m_TextureCoord.position()/3 : 0;}
	int getNumTextureCoords() { return m_TextureCoord != null ? m_TextureCoord.remaining()/3 : 0;}
}
