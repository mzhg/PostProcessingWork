package assimp.importer.obj;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.TextureType;

/** Imports a waveform obj file */
public class ObjFileImporter extends BaseImporter{

	static final ImporterDesc desc = new ImporterDesc(
			"Wavefront Object Importer",
			"",
			"",
			"surfaces not supported",
			ImporterDesc.aiImporterFlags_SupportTextFlavour,
			0,
			0,
			0,
			0,
			"obj"
			);
	
	static final int ObjMinSize = 16;
	static final String[] pTokens = { "mtllib", "usemtl", "v ", "vt ", "vn ", "o ", "g ", "s ", "f " };
	
	//!	Data buffer
    ByteBuffer m_Buffer;
	//!	Pointer to root object instance
	Object m_pRootObject;
	//!	Absolute pathname of model in file system
	String m_strAbsPath;
	
	@Override
	public boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) {
		if(!checkSig) //Check File Extension
		{
			return simpleExtensionCheck(pFile,"obj", null, null);
		}
		else //Check file Header
		{
			try {
				return BaseImporter.searchFileHeaderForToken(pIOHandler, pFile, pTokens, 9, 200, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return false;
		}
	}
	
	//! \brief	Appends the supported extension.
	public ImporterDesc getInfo () { return desc;}

	//!	\brief	File import implementation.
	public void internReadFile(File pFile, Scene pScene){
		ByteBuffer buf = FileUtils.loadText(pFile, true, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		String filename = pFile.getName();
		// TODO
	}
	
	//!	\brief	Create the data from imported content.
	void createDataFromImport(Model pModel, Scene pScene){
		if( null == pModel ) {
	        return;
	    }
			
		// Create the root node of the scene
		pScene.mRootNode = new Node();
		if ( !pModel.m_ModelName.isEmpty() )
		{
			// Set the name of the scene
			pScene.mRootNode.mName = (pModel.m_ModelName);
		}
		else
		{
			// This is a fatal error, so break down the application
//			ai_assert(false);
			throw new AssertionError();
		} 

		// Create nodes for the whole scene	
		List<Mesh> meshArray = new ArrayList<>();
		for (int index = 0; index < pModel.m_Objects.size(); index++)
		{
			createNodes(pModel, pModel.m_Objects.get(index), pScene.mRootNode, pScene, meshArray);
		}

		// Create mesh pointer buffer for this scene
		if (pScene.getNumMeshes() > 0)
		{
			pScene.mMeshes = new Mesh[ meshArray.size() ];
			meshArray.toArray(pScene.mMeshes);
//			for (int index =0; index < meshArray.size(); index++)
//			{
//				pScene.mMeshes [ index ] = meshArray[ index ];
//			}
		}

		// Create all materials
		createMaterials( pModel, pScene );
	}
	
	//!	\brief	Creates all nodes stored in imported content.
	Node createNodes(Model pModel, ObjObject pObject,
		Node pParent, Scene pScene, List<Mesh> meshArray){
		if( null == pObject ) {
	        return null;
	    }
		
		// Store older mesh size to be able to computes mesh offsets for new mesh instances
		final int oldMeshSize = meshArray.size();
		Node pNode = new Node();

		pNode.mName = pObject.m_strObjName;
		
		// If we have a parent node, store it
	    if( pParent != null ) {
	        appendChildToParentNode( pParent, pNode );
	    }

		for (int i=0; i< pObject.m_Meshes.size(); i++ )
		{
			int meshId = pObject.m_Meshes.getInt(i);
			Mesh pMesh = new Mesh();
			createTopology( pModel, pObject, meshId, pMesh );	
			if ( pMesh.mNumVertices > 0 ) 
			{
				meshArray.add( pMesh );
			}
			else
			{
//				delete pMesh;
			}
		}

		// Create all nodes from the sub-objects stored in the current object
		if ( !pObject.m_SubObjects.isEmpty() )
		{
			int numChilds = pObject.m_SubObjects.size();
//			pNode.mNumChildren = static_cast<unsigned int>( numChilds );
			pNode.mChildren = new Node[ numChilds ];
//			pNode.mNumMeshes = 1;
			pNode.mMeshes = new int[ 1 ];
		}

		// Set mesh instances into scene- and node-instances
		final int meshSizeDiff = meshArray.size()- oldMeshSize;
		if ( meshSizeDiff > 0 )
		{
			pNode.mMeshes = new int[ meshSizeDiff ];
//			pNode.mNumMeshes = static_cast<unsigned int>( meshSizeDiff );
			int index = 0;
			for (int i = oldMeshSize; i < meshArray.size(); i++)
			{
				pNode.mMeshes[ index ] = pScene.getNumMeshes();
//				pScene.mNumMeshes++;  TODO
				index++;
			}
		}
		
		return pNode;
	}

	//!	\brief	Creates topology data like faces and meshes for the geometry.
	void createTopology(Model pModel, ObjObject pData, int uiMeshIndex, Mesh pMesh){
		if( null == pData ) {
	        return;
	    }

		// Create faces
		ObjMesh pObjMesh = pModel.m_Meshes.get(uiMeshIndex);
//		ai_assert( NULL != pObjMesh );

//		pMesh.mNumFaces = 0;
		int pMesh_numfaces = 0;
		for (int index = 0; index < pObjMesh.m_Faces.size(); index++)
		{
			ObjFace inp = pObjMesh.m_Faces.get(index);
		
			if (inp.m_PrimitiveType == Mesh.aiPrimitiveType_LINE) {
				pMesh_numfaces += inp.m_pVertices.size() - 1;
				pMesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_LINE;
			}
			else if (inp.m_PrimitiveType == Mesh.aiPrimitiveType_POINT) {
				pMesh_numfaces += inp.m_pVertices.size();
				pMesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POINT;
			} else {
//				++pMesh.mNumFaces;
				pMesh_numfaces ++;
				if (inp.m_pVertices.size() > 3) {
					pMesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POLYGON;
				}
				else {
					pMesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_TRIANGLE;
				}
			}
		}

		int uiIdxCount = 0;
		if ( pMesh_numfaces > 0 )
		{
			pMesh.mFaces = new Face[ pMesh_numfaces ];
			if ( pObjMesh.m_uiMaterialIndex != ObjMesh.NO_MATERIAL )
			{
				pMesh.mMaterialIndex = pObjMesh.m_uiMaterialIndex;
			}

			int outIndex = 0;

			// Copy all data from all stored meshes
			for (int index = 0; index < pObjMesh.m_Faces.size(); index++)
			{
				ObjFace inp = pObjMesh.m_Faces.get(index);
				if (inp.m_PrimitiveType == Mesh.aiPrimitiveType_LINE) {
					for(int i = 0; i < inp.m_pVertices.size() - 1; ++i) {
//						Face f = pMesh.mFaces[ outIndex++ ];
						uiIdxCount += /*f.mNumIndices =*/ 2;
//						f.mIndices = new  int[2];
						pMesh.mFaces[outIndex++] = Face.createInstance(2);
					}
					continue;
				}
				else if (inp.m_PrimitiveType == Mesh.aiPrimitiveType_POINT) {
					for(int i = 0; i < inp.m_pVertices.size(); ++i) {
//						aiFace& f = pMesh.mFaces[ outIndex++ ];
						uiIdxCount += /*f.mNumIndices =*/ 1;
//						f.mIndices = new unsigned int[1];
						pMesh.mFaces[ outIndex++ ] = Face.createInstance(1);
					}
					continue;
				}

//				Face pFace = &pMesh.mFaces[ outIndex++ ];
				final int uiNumIndices = pObjMesh.m_Faces.get(index).m_pVertices.size();
				uiIdxCount +=/* pFace.mNumIndices = (unsigned int)*/ uiNumIndices;
//				if (pFace.mNumIndices > 0) {
//					pFace.mIndices = new unsigned int[ uiNumIndices ];			
//				}
				
				pMesh.mFaces[outIndex++] = Face.createInstance(uiNumIndices);
			}
		}

		// Create mesh vertices
		createVertexArray(pModel, pData, uiMeshIndex, pMesh, uiIdxCount);
	}
	
	//!	\brief	Creates vertices from model.
	void createVertexArray(Model pModel, ObjObject pCurrentObject, int uiMeshIndex, Mesh pMesh,int uiIdxCount){
		// Break, if no faces are stored in object
		if ( pCurrentObject.m_Meshes.isEmpty() )
			return;

		// Get current mesh
		ObjMesh pObjMesh = pModel.m_Meshes.get(uiMeshIndex);
		if ( null == pObjMesh || pObjMesh.m_uiNumIndices < 1)
			return;

		// Copy vertices of this mesh instance
		pMesh.mNumVertices = uiIdxCount;
		pMesh.mVertices = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		
		// Allocate buffer for normal vectors
		if ( /*!pModel.m_Normals.isEmpty() && */pObjMesh.m_hasNormals )
			pMesh.mNormals = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		
		// Allocate buffer for texture coordinates
		if ( /*!pModel.m_TextureCoord.empty() &&*/ pObjMesh.m_uiUVCoordinates[0] > 0)
		{
			pMesh.mNumUVComponents[ 0 ] = 2;
			pMesh.mTextureCoords[ 0 ] = MemoryUtil.createFloatBuffer(pMesh.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		}
		
		// Copy vertices, normals and textures into aiMesh instance
		int newIndex = 0, outIndex = 0;
		for ( int index=0; index < pObjMesh.m_Faces.size(); index++ )
		{
			// Get source face
			ObjFace pSourceFace = pObjMesh.m_Faces.get(index); 

			// Copy all index arrays
			for (int vertexIndex = 0, outVertexIndex = 0; vertexIndex < pSourceFace.m_pVertices.size(); vertexIndex++ )
			{
				final int vertex = pSourceFace.m_pVertices.get( vertexIndex );
				if ( vertex >= pModel.m_Vertices.remaining()/3 ) 
					throw new DeadlyImportError( "OBJ: vertex index out of range" );
				
//				pMesh.mVertices[ newIndex ] = pModel.m_Vertices[ vertex ];
				pMesh.mVertices.put(3 * newIndex + 0, pModel.m_Vertices.get(3 * vertex + 0));
				pMesh.mVertices.put(3 * newIndex + 1, pModel.m_Vertices.get(3 * vertex + 1));
				pMesh.mVertices.put(3 * newIndex + 2, pModel.m_Vertices.get(3 * vertex + 2));
				
				// Copy all normals 
				if ( pModel.m_Normals.remaining()> 0 && vertexIndex < pSourceFace.m_pNormals.size())
				{
					final int normal = pSourceFace.m_pNormals.get( vertexIndex );
					if ( normal >= pModel.m_Normals.remaining()/3 )
						throw new DeadlyImportError("OBJ: vertex normal index out of range");

//					pMesh.mNormals[ newIndex ] = pModel.m_Normals[ normal ];
					pMesh.mNormals.put(3 * newIndex + 0, pModel.m_Normals.get(3 * normal + 0));
					pMesh.mNormals.put(3 * newIndex + 1, pModel.m_Normals.get(3 * normal + 1));
					pMesh.mNormals.put(3 * newIndex + 2, pModel.m_Normals.get(3 * normal + 2));
				}
				
				// Copy all texture coordinates
				if ( pModel.m_TextureCoord != null && vertexIndex < pSourceFace.m_pTexturCoords.size())
				{
					final int tex = pSourceFace.m_pTexturCoords.get( vertexIndex );
//					ai_assert( tex < pModel.m_TextureCoord.size() );
						
					if ( tex >= pModel.m_TextureCoord.remaining()/3 )
						throw new DeadlyImportError("OBJ: texture coordinate index out of range");

//					const aiVector3D &coord3d = pModel.m_TextureCoord[ tex ];
//	                pMesh.mTextureCoords[ 0 ][ newIndex ] = aiVector3D( coord3d.x, coord3d.y, coord3d.z );
					pMesh.mTextureCoords[ 0 ].put(3 * newIndex + 0, pModel.m_TextureCoord.get(3 * tex + 0));
					pMesh.mTextureCoords[ 0 ].put(3 * newIndex + 1, pModel.m_TextureCoord.get(3 * tex + 1));
					pMesh.mTextureCoords[ 0 ].put(3 * newIndex + 2, pModel.m_TextureCoord.get(3 * tex + 2));
				}

//				ai_assert( pMesh.mNumVertices > newIndex );

				// Get destination face
				Face pDestFace = pMesh.mFaces[ outIndex ];

				final boolean last = ( vertexIndex == pSourceFace.m_pVertices.size() - 1 ); 
				if (pSourceFace.m_PrimitiveType != Mesh.aiPrimitiveType_LINE || !last) 
				{
//					pDestFace.mIndices[ outVertexIndex ] = newIndex;
					pDestFace.set(outVertexIndex, newIndex);
					outVertexIndex++;
				}

				if (pSourceFace.m_PrimitiveType == Mesh.aiPrimitiveType_POINT) 
				{
					outIndex++;
					outVertexIndex = 0;
				}
				else if (pSourceFace.m_PrimitiveType == Mesh.aiPrimitiveType_LINE) 
				{
					outVertexIndex = 0;

					if(!last) 
						outIndex++;

					if (vertexIndex != 0) {
						if(!last) {
//							pMesh.mVertices[ newIndex+1 ] = pMesh.mVertices[ newIndex ];
							pMesh.mVertices.put(3 * (newIndex + 1) + 0, pMesh.mVertices.get(3 * newIndex) + 0);
							pMesh.mVertices.put(3 * (newIndex + 1) + 1, pMesh.mVertices.get(3 * newIndex) + 1);
							pMesh.mVertices.put(3 * (newIndex + 1) + 2, pMesh.mVertices.get(3 * newIndex) + 2);
							
							if ( !pSourceFace.m_pNormals.isEmpty() && pModel.m_Normals != null) {
//								pMesh.mNormals[ newIndex+1 ] = pMesh.mNormals[newIndex ];
								MemoryUtil.arraycopy(pMesh.mNormals, 3 * newIndex, pMesh.mNormals, 3 * (newIndex + 1), 3);
							}
							if ( pModel.m_TextureCoord != null ) {
								for ( int i=0; i < pMesh.getNumUVChannels(); i++ ) {
//									pMesh.mTextureCoords[ i ][ newIndex+1 ] = pMesh.mTextureCoords[ i ][ newIndex ];
									MemoryUtil.arraycopy(pMesh.mTextureCoords[ i ], 3 * newIndex, pMesh.mTextureCoords[ i ], 3 * (newIndex + 1), 3);
								}
							}
							++newIndex;
						}

//						pDestFace[-1].mIndices[1] = newIndex;
						pMesh.mFaces[ outIndex -1].set(1, newIndex);
					}
				}
				else if (last) {
					outIndex++;
				}
				++newIndex;
			}
		}	
	}

	//!	\brief	Object counter helper method.
	int countObjects(List<ObjObject> rObjects){
		int iNumMeshes = 0;
		if ( rObjects == null || rObjects.isEmpty() )	
			return 0;

		iNumMeshes += rObjects.size();
		for (ObjObject it : rObjects)
		{
			if (!it.m_SubObjects.isEmpty())
			{
				iNumMeshes += countObjects(it.m_SubObjects);
			}
		}
		
		return iNumMeshes;
	}

	//!	\brief	Material creation.
	void createMaterials(Model pModel, Scene pScene){
		if ( null == pScene )
			return;

		final int numMaterials = pModel.m_MaterialLib.size();
		int pScene_mNumMaterials = 0;
		if ( pModel.m_MaterialLib.isEmpty() ) {
			DefaultLogger.debug("OBJ: no materials specified");
			return;
		}
		
		pScene.mMaterials = new Material[ numMaterials ];
		for ( int matIndex = 0; matIndex < numMaterials; matIndex++ )
		{		
			// Store material name
//			std::map<std::string, ObjFile::Material*>::const_iterator it;
			ObjMaterial second = pModel.m_MaterialMap.get( pModel.m_MaterialLib.get(matIndex) );
			
			// No material found, use the default material
//			if ( pModel.m_MaterialMap.end() == it )
			if(second == null)
				continue;

			Material mat = new Material();
			ObjMaterial pCurrentMaterial = second;
//			mat.addProperty( &pCurrentMaterial.MaterialName, AI_MATKEY_NAME );
			mat.addProperty(pCurrentMaterial.materialName, Material.AI_MATKEY_NAME,0,0);

			// convert illumination model
			ShadingMode sm = null;
			switch (pCurrentMaterial.illumination_model) 
			{
			case 0:
				sm = ShadingMode.aiShadingMode_NoShading;
				break;
			case 1:
				sm = ShadingMode.aiShadingMode_Gouraud;
				break;
			case 2:
				sm = ShadingMode.aiShadingMode_Phong;
				break;
			default:
				sm = ShadingMode.aiShadingMode_Gouraud;
				DefaultLogger.error("OBJ: unexpected illumination model (0-2 recognized)");
			}
		
//			mat.AddProperty<int>( &sm, 1, AI_MATKEY_SHADING_MODEL);
			mat.addProperty(sm.ordinal(), "$mat.shadingm",0,0);

			// multiplying the specular exponent with 2 seems to yield better results
			pCurrentMaterial.shineness *= 4.f;

			// Adding material colors
			mat.addProperty( pCurrentMaterial.ambient, Material.AI_MATKEY_COLOR_AMBIENT,0,0 );
			mat.addProperty( pCurrentMaterial.diffuse, Material.AI_MATKEY_COLOR_DIFFUSE,0,0 );
			mat.addProperty( pCurrentMaterial.specular, Material.AI_MATKEY_COLOR_SPECULAR,0,0 );
			mat.addProperty( pCurrentMaterial.emissive, Material.AI_MATKEY_COLOR_EMISSIVE,0,0 );
			mat.addProperty( pCurrentMaterial.shineness, Material.AI_MATKEY_SHININESS,0,0 );
			mat.addProperty( pCurrentMaterial.alpha, Material.AI_MATKEY_OPACITY,0,0 );

			// Adding refraction index
			mat.addProperty( pCurrentMaterial.ior, Material.AI_MATKEY_REFRACTI,0,0  );

			// Adding textures
			if ( 0 != pCurrentMaterial.texture.length() ) 
			{
				mat.addProperty(pCurrentMaterial.texture, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureDiffuseType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_DIFFUSE);
				}
			}

			if ( 0 != pCurrentMaterial.textureAmbient.length() )
			{
				mat.addProperty(pCurrentMaterial.textureAmbient, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_AMBIENT.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureAmbientType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_AMBIENT);
				}
			}

			if ( 0 != pCurrentMaterial.textureEmissive.length() )
				mat.addProperty( pCurrentMaterial.textureEmissive, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_EMISSIVE.ordinal(), 0);

			if ( 0 != pCurrentMaterial.textureSpecular.length() )
			{
				mat.addProperty( pCurrentMaterial.textureSpecular, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_SPECULAR.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureSpecularType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_SPECULAR);
				}
			}

			if ( 0 != pCurrentMaterial.textureBump.length() )
			{
				mat.addProperty(pCurrentMaterial.textureBump, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_HEIGHT.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureBumpType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_HEIGHT);
				}
			}

			if ( 0 != pCurrentMaterial.textureNormal.length() )
			{
				mat.addProperty( pCurrentMaterial.textureNormal, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_NORMALS.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureNormalType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_NORMALS);
				}
			}

			if ( 0 != pCurrentMaterial.textureDisp.length() )
			{
				mat.addProperty(pCurrentMaterial.textureDisp, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DISPLACEMENT.ordinal(), 0 );
				if (pCurrentMaterial.clamp[ObjMaterial.TextureDispType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_DISPLACEMENT);
				}
			}

			if ( 0 != pCurrentMaterial.textureOpacity.length() )
			{
				mat.addProperty(pCurrentMaterial.textureOpacity, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_OPACITY.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureOpacityType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_OPACITY);
				}
			}

			if ( 0 != pCurrentMaterial.textureSpecularity.length() )
			{
				mat.addProperty(pCurrentMaterial.textureSpecularity, Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_SHININESS.ordinal(), 0);
				if (pCurrentMaterial.clamp[ObjMaterial.TextureSpecularityType])
				{
					addTextureMappingModeProperty(mat, TextureType.aiTextureType_SHININESS);
				}
			}
			
			// Store material property info in material array in scene
			pScene.mMaterials[ pScene_mNumMaterials ] = mat;
			pScene_mNumMaterials++;
		}
		
		// Test number of created materials.
//		ai_assert( pScene.mNumMaterials == numMaterials );
		if(pScene_mNumMaterials != numMaterials)
			throw new AssertionError();
	}
	
	void addTextureMappingModeProperty(Material mat, TextureType type){
		addTextureMappingModeProperty(mat, type, 1);
	}
	
	void addTextureMappingModeProperty(Material mat, TextureType type, int clampMode /*= 1*/){
//		mat.AddProperty<int>(&clampMode, 1, AI_MATKEY_MAPPINGMODE_U(type, 0));
//		mat.AddProperty<int>(&clampMode, 1, AI_MATKEY_MAPPINGMODE_V(type, 0));
		mat.addProperty(clampMode, "$tex.mapmodeu", type.ordinal(), 0);
		mat.addProperty(clampMode, "$tex.mapmodev", type.ordinal(), 0);
	}

	//!	\brief	Appends a child node to a parent node and updates the data structures.
	void appendChildToParentNode(Node pParent, Node pChild){
		// Assign parent to child
		pChild.mParent = pParent;
		
		if(pParent.mChildren == null){
			pParent.mChildren = new Node[]{pChild};
		}else{
			int length = pParent.mChildren.length;
			pParent.mChildren = Arrays.copyOf(pParent.mChildren, length + 1);
			pParent.mChildren[length] = pChild;
		}
		
		// If already children was assigned to the parent node, store them in a 
//		List<Node> temp = new ArrayList<Node>();
//		if (pParent.mChildren != null)
//		{
////			ai_assert( 0 != pParent.mNumChildren );
//			for (int index = 0; index < pParent.getNumChildren(); index++)
//			{
//				temp.add(pParent.mChildren [ index ] );
//			}
////			delete [] pParent.mChildren;
//		}
//		
//		// Copy node instances into parent node
//		pParent.mNumChildren++;
//		pParent.mChildren = new aiNode*[ pParent.mNumChildren ];
//		for (size_t index = 0; index < pParent.mNumChildren-1; index++)
//		{
//			pParent.mChildren[ index ] = temp [ index ];
//		}
//		pParent.mChildren[ pParent.mNumChildren-1 ] = pChild;
	}
}
