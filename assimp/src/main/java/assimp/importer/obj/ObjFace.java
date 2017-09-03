package assimp.importer.obj;

import it.unimi.dsi.fastutil.ints.IntList;

/** Data structure for a simple obj-face, describes discredit,l.ation and materials*/
/*public*/ final class ObjFace {

	//!	Primitive type
	int m_PrimitiveType;
	//!	Vertex indices
	IntList m_pVertices;
	//!	Normal indices
	IntList m_pNormals;
	//!	Texture coordinates indices
	IntList m_pTexturCoords;
	//!	Pointer to assigned material
	ObjMaterial m_pMaterial;
	
	ObjFace( IntList pVertices, 
			IntList pNormals, 
			IntList pTexCoords,
			int pt /*= aiPrimitiveType_POLYGON*/){ 
		m_PrimitiveType = ( pt );
		m_pVertices = ( pVertices ); 
		m_pNormals = ( pNormals );
		m_pTexturCoords = ( pTexCoords ); 
		m_pMaterial = null;
	}
}
