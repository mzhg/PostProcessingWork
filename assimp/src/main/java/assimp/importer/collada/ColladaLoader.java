package assimp.importer.collada;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.BaseImporter;
import assimp.common.Bone;
import assimp.common.Camera;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.ImporterDesc;
import assimp.common.IntPair;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.Pair;
import assimp.common.QuatKey;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.SkeletonMeshBuilder;
import assimp.common.Texture;
import assimp.common.TextureMapMode;
import assimp.common.TextureType;
import assimp.common.VectorKey;
import assimp.common.VertexWeight;

/** Loader class to read Collada scenes. Collada is over-engineered to death, with every new iteration bringing
 * more useless stuff, so I limited the data to what I think is useful for games. 
*/
final class ColladaLoader extends BaseImporter{
	
	static final ImporterDesc desc = new ImporterDesc(
		"Collada Importer",
		"",
		"",
		"http://collada.org",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		1,
		3,
		1,
		5,
		"dae" 
	);

	/** Filename, for a verbose error message */
	String mFileName;

	/** Which mesh-material compound was stored under which mesh ID */
	Object2IntOpenHashMap <ColladaMeshIndex> mMeshIndexByID;

	/** Which material was stored under which index in the scene */
	Object2IntOpenHashMap<String> mMaterialIndexByName;

	/** Accumulated meshes for the target scene */
	ArrayList<Mesh> mMeshes;

	/** Temporary material list */
//	std::vector<std::pair<Collada::Effect*, aiMaterial*> > newMats;
	ArrayList<Pair<Effect, Material>> newMats;

	/** Temporary camera list */
	ArrayList<Camera> mCameras;

	/** Temporary light list */
	ArrayList<Light> mLights;

	/** Temporary texture list */
	ArrayList<Texture> mTextures;

	/** Accumulated animations for the target scene */
	ArrayList<Animation>mAnims;

	boolean noSkeletonMesh;
	boolean ignoreUpDirection;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) throws IOException{
		// check file extension 
		String extension = getExtension(pFile);
		
		if( extension.endsWith("dae"))
			return true;

		// XML - too generic, we need to open the file and search for typical keywords
		if( extension.equals("xml") || extension.length() == 0|| checkSig)	{
			/*  If CanRead() is called in order to check whether we
			 *  support a specific file extension in general pIOHandler
			 *  might be NULL and it's our duty to return true here.
			 */
			if (pIOHandler == null)return true;
			String tokens[] = {"collada"};
			return searchFileHeaderForToken(pIOHandler,pFile,tokens);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		mFileName = pFile.getName();

		// clean all member arrays - just for safety, it should work even if we did not
//		mMeshIndexByID.clear();
//		mMaterialIndexByName.clear();
//		mMeshes.clear();
//		newMats.clear();
//		mLights.clear();
//		mCameras.clear();
//		mTextures.clear();
//		mAnims.clear();

		// parse the input file
//		ColladaParser parser( pIOHandler, pFile);
		ColladaParser parser = null;
		try {
			parser = new ColladaParser(pFile);
		} catch (IOException e) {
			// TODO Need throw out
			e.printStackTrace();
		}

		if( parser.mRootNode == null)
			throw new DeadlyImportError( "Collada: File came out empty. Something is wrong here.");

		// reserve some storage to avoid unnecessary reallocs
		newMats = new ArrayList<>(parser.mMaterialLibrary.size()*2);
		mMeshes = new ArrayList<>(parser.mMeshLibrary.size()*2);

		mCameras = new ArrayList<>(parser.mCameraLibrary.size());
		mLights = new ArrayList<>(parser.mLightLibrary.size());

		// create the materials first, for the meshes to find
		buildMaterials( parser, pScene);

		// build the node hierarchy from it
		pScene.mRootNode = buildHierarchy( parser, parser.mRootNode);

		// ... then fill the materials with the now adjusted settings
		fillMaterials(parser, pScene);

        // Apply unitsize scale calculation
//        pScene.mRootNode.mTransformation *= aiMatrix4x4(parser.mUnitSize, 0,  0,  0, 
//                                                          0,  parser.mUnitSize,  0,  0,
//                                                          0,  0,  parser.mUnitSize,  0,
//                                                          0,  0,  0,  1);
		pScene.mRootNode.mTransformation.scale(parser.mUnitSize, parser.mUnitSize, parser.mUnitSize);
        if( !ignoreUpDirection ) {
	        // Convert to Y_UP, if different orientation
			if( parser.mUpDirection == ColladaParser.UP_X){
//				pScene->mRootNode->mTransformation *= aiMatrix4x4( 
//					 0, -Material. 0,  0, 
//					 Material. 0,  0,  0,
//					 0,  0,  Material. 0,
//					 0,  0,  0,  1);
				Matrix4f mat = new Matrix4f();
				mat.setRow(0, 0, -1,0, 0);
				mat.setRow(1,1,0, 0, 0);
				mat.setRow(2, 0, 0, 1,0);
				mat.setRow(3, 0, 0, 0, 1);
				Matrix4f.mul(pScene.mRootNode.mTransformation, mat, pScene.mRootNode.mTransformation);
			}else if( parser.mUpDirection == ColladaParser.UP_Z){
//				pScene->mRootNode->mTransformation *= aiMatrix4x4( 
//					 Material. 0,  0,  0, 
//					 0,  0,  Material. 0,
//					 0, -Material. 0,  0,
//					 0,  0,  0,  1);
				
				Matrix4f mat = new Matrix4f();
				mat.setRow(0, 1,0, 0, 0);
				mat.setRow(1,0, 0, 1,0);
				mat.setRow(2, 0, -1,0, 0);
				mat.setRow(3, 0, 0, 0, 1);
				Matrix4f.mul(pScene.mRootNode.mTransformation, mat, pScene.mRootNode.mTransformation);
			}
        }
		// store all meshes
		storeSceneMeshes( pScene);

		// store all materials
		storeSceneMaterials( pScene);

		// store all lights
		storeSceneLights( pScene);

		// store all cameras
		storeSceneCameras( pScene);

		// store all animations
		storeAnimations( pScene, parser);


		// If no meshes have been loaded, it's probably just an animated skeleton.
		if (pScene.mMeshes != null) {
		
			if (!noSkeletonMesh) {
				 new SkeletonMeshBuilder(pScene,null, false);
			}
			pScene.mFlags |= Scene.AI_SCENE_FLAGS_INCOMPLETE;
		}
	}
	
	/** Recursively constructs a scene node for the given parser node and returns it. */
	Node buildHierarchy(ColladaParser pParser, COLNode pNode){
		// create a node for it
		Node node = new Node();

		// find a name for the new node. It's more complicated than you might think
		node.mName = findNameForNode( pNode);

		// calculate the transformation matrix for it
		pParser.calculateResultTransform( pNode.mTransforms, node.mTransformation);

		// now resolve node instances
//		std::vector<COLNode> instances;
		ArrayList<COLNode> instances = new ArrayList<COLNode>();
		resolveNodeInstances(pParser,pNode,instances);

		// add children. first the *real* ones
//		node->mNumChildren = pNode->mChildren.size()+instances.size();
//		node->mChildren = new aiNode*[node->mNumChildren];
		node.mChildren = new Node[pNode.mChildren.size() + instances.size()];

		for( int a = 0; a < pNode.mChildren.size(); a++)
		{
			node.mChildren[a] = buildHierarchy( pParser, pNode.mChildren.get(a));
			node.mChildren[a].mParent = node;
		}

		// ... and finally the resolved node instances
		for( int a = 0; a < instances.size(); a++)
		{
			node.mChildren[pNode.mChildren.size() + a] = buildHierarchy( pParser, instances.get(a));
			node.mChildren[pNode.mChildren.size() + a].mParent = node;
		}

		// construct meshes
		buildMeshesForNode( pParser, pNode, node);

		// construct cameras
		buildCamerasForNode(pParser, pNode, node);

		// construct lights
		buildLightsForNode(pParser, pNode, node);
		return node;
	}

	/** Resolve node instances */
	void resolveNodeInstances(ColladaParser pParser, COLNode pNode, ArrayList<COLNode> resolved){
		// reserve enough storage
		resolved.ensureCapacity(pNode.mNodeInstances.size());

		// ... and iterate through all nodes to be instanced as children of pNode
//		for (std::vector<Collada::NodeInstance>::const_iterator it = pNode->mNodeInstances.begin(),
//			 end = pNode->mNodeInstances.end(); it != end; ++it)
		for (NodeInstance it : pNode.mNodeInstances)
		{
			// find the corresponding node in the library
//			const ColladaParser::NodeLibrary::const_iterator itt = pParser.mNodeLibrary.find((*it).mNode);
//			COLNode nd = itt == pParser.mNodeLibrary.end() ? NULL : (*itt).second;
			COLNode itt = pParser.mNodeLibrary.get(it.mNode);
			

			// FIX for http://sourceforge.net/tracker/?func=detail&aid=3054873&group_id=226462&atid=1067632
			// need to check for both name and ID to catch all. To avoid breaking valid files,
			// the workaround is only enabled when the first attempt to resolve the node has failed.
			if (itt == null) {
				itt = findNode(pParser.mRootNode,it.mNode);
			}
			if (itt == null) 
				DefaultLogger.error("Collada: Unable to resolve reference to instanced node " + it.mNode);
			
			else {
				//	attach this node to the list of children
				resolved.add(itt);
			}
		}
	}

	/** Builds meshes for the given node and references them */
	void buildMeshesForNode(ColladaParser pParser, COLNode pNode, Node pTarget){
		// accumulated mesh references by this node
//		std::vector<size_t> newMeshRefs;
//		newMeshRefs.reserve(pNode->mMeshes.size());
		IntArrayList newMeshRefs = new IntArrayList(pNode.mMeshes.size());

		// add a mesh for each subgroup in each collada mesh
//		BOOST_FOREACH( const Collada::MeshInstance& mid, pNode->mMeshes)
		for (MeshInstance mid : pNode.mMeshes)
		{
			COLMesh srcMesh = null;
			Controller srcController = null;

			// find the referred mesh
//			ColladaParser::MeshLibrary::const_iterator srcMeshIt = pParser.mMeshLibrary.find( mid.mMeshOrController);
			srcMesh = pParser.mMeshLibrary.get(mid.mMeshOrController);
//			if( srcMeshIt == pParser.mMeshLibrary.end())
			if( srcMesh == null)
			{
				// if not found in the mesh-library, it might also be a controller referring to a mesh
//				ColladaParser::ControllerLibrary::const_iterator srcContrIt = pParser.mControllerLibrary.find( mid.mMeshOrController);
				srcController = pParser.mControllerLibrary.get(mid.mMeshOrController);
//				if( srcContrIt != pParser.mControllerLibrary.end())
				if( srcController != null)
				{
//					srcController = &srcContrIt->second;
					srcMesh = pParser.mMeshLibrary.get( srcController.mMeshId);
//					if( srcMeshIt != pParser.mMeshLibrary.end())
//						srcMesh = srcMeshIt->second;
				}

				if( srcMesh == null)
				{
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn(String.format("Collada: Unable to find geometry for ID \"%s\". Skipping.", mid.mMeshOrController));
					continue;
				}
			} else
			{
				// ID found in the mesh library -> direct reference to an unskinned mesh
//				srcMesh = srcMeshIt->second;
			}

			// build a mesh for each of its subgroups
			int vertexStart = 0, faceStart = 0;
			for( int sm = 0; sm < srcMesh.mSubMeshes.size(); ++sm)
			{
//				const Collada::SubMesh& submesh = srcMesh->mSubMeshes[sm];
				SubMesh submesh = srcMesh.mSubMeshes.get(sm);
				if( submesh.mNumFaces == 0)
					continue;

				// find material assigned to this submesh
				String meshMaterial = null;
//				std::map<std::string, Collada::SemanticMappingTable >::const_iterator meshMatIt = mid.mMaterials.find( submesh.mMaterial);
				SemanticMappingTable table = mid.mMaterials.get(submesh.mMaterial);

//				const Collada::SemanticMappingTable* table = NULL;
//				if( meshMatIt != mid.mMaterials.end())
				if( table != null)
				{
//					table = &meshMatIt->second;
					meshMaterial = table.mMatName;
				}
				else 
				{
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn(String.format("Collada: No material specified for subgroup <%s> in geometry <%s>.", submesh.mMaterial , mid.mMeshOrController));
					if( !mid.mMaterials.isEmpty() )
						meshMaterial = /*mid.mMaterials.begin()->second.mMatName*/ mid.mMaterials.values().iterator().next().mMatName;
				}

				// OK ... here the *real* fun starts ... we have the vertex-input-to-effect-semantic-table
				// given. The only mapping stuff which we do actually support is the UV channel.
//				std::map<std::string, size_t>::const_iterator matIt = mMaterialIndexByName.find( meshMaterial);
//				unsigned int matIdx;
//				if( matIt != mMaterialIndexByName.end())
//					matIdx = matIt->second;
//				else
//					matIdx = 0;
				
				int matIdx = mMaterialIndexByName.getInt(meshMaterial);

				if (table != null && !table.mMap.isEmpty() ) {
//					std::pair<Collada::Effect*, aiMaterial*>&  mat = newMats[matIdx];
					Pair<Effect, Material> mat = newMats.get(matIdx);

					// Iterate through all texture channels assigned to the effect and
					// check whether we have mapping information for it.
					applyVertexToEffectSemanticMapping(mat.first.mTexDiffuse,    table);
					applyVertexToEffectSemanticMapping(mat.first.mTexAmbient,    table);
					applyVertexToEffectSemanticMapping(mat.first.mTexSpecular,   table);
					applyVertexToEffectSemanticMapping(mat.first.mTexEmissive,   table);
					applyVertexToEffectSemanticMapping(mat.first.mTexTransparent,table);
					applyVertexToEffectSemanticMapping(mat.first.mTexBump,       table);
				}

				// built lookup index of the Mesh-Submesh-Material combination
				ColladaMeshIndex index = new ColladaMeshIndex( mid.mMeshOrController, sm, meshMaterial);

				// if we already have the mesh at the library, just add its index to the node's array
//				std::map<ColladaMeshIndex, size_t>::const_iterator dstMeshIt = mMeshIndexByID.find( index);
				int dstMeshIt = AssUtil.getInt(mMeshIndexByID, index, -1)/*mMeshIndexByID.getInt( index)*/ ;
				if( dstMeshIt != /*mMeshIndexByID.end()*/ - 1)	{
					newMeshRefs.add( dstMeshIt/*->second*/);
				} 
				else
				{
					// else we have to add the mesh to the collection and store its newly assigned index at the node
					Mesh dstMesh = createMesh( pParser, srcMesh, submesh, srcController, vertexStart, faceStart);

					// store the mesh, and store its new index in the node
					newMeshRefs.add( mMeshes.size());
//					mMeshIndexByID[index] = mMeshes.size();
					mMeshIndexByID.put(index, mMeshes.size());
					mMeshes.add( dstMesh);
					vertexStart += dstMesh.mNumVertices; faceStart += submesh.mNumFaces;

					// assign the material index
					dstMesh.mMaterialIndex = matIdx;
	                if(AssUtil.isEmpty(dstMesh.mName))
	                {
	                    dstMesh.mName = mid.mMeshOrController;
	                }
	      }
			}
		}

		// now place all mesh references we gathered in the target node
//		pTarget.mNumMeshes = newMeshRefs.size();
		if( newMeshRefs.size() > 0)
		{
//			pTarget->mMeshes = new unsigned int[pTarget->mNumMeshes];
//			std::copy( newMeshRefs.begin(), newMeshRefs.end(), pTarget->mMeshes);
			pTarget.mMeshes = newMeshRefs.toIntArray();
		}
	}

	/** Creates a mesh for the given ColladaMesh face subset and returns the newly created mesh */
	Mesh createMesh(ColladaParser pParser, COLMesh pSrcMesh, SubMesh pSubMesh, 
		Controller pSrcController, int pStartVertex, int pStartFace){
		Mesh dstMesh = new Mesh();
	    
	    dstMesh.mName = pSrcMesh.mName;

		// count the vertices addressed by its faces
//		final int numVertices = std::accumulate( pSrcMesh.mFaceSize.begin() + pStartFace,
//			pSrcMesh.mFaceSize.begin() + pStartFace + pSubMesh.mNumFaces, 0);
	    int numVertices = 0;
	    for(int i = 0; i < pSubMesh.mNumFaces; i++){
	    	numVertices += pSrcMesh.mFaceSize.getInt(pStartFace + i);
	    }

		// copy positions
		dstMesh.mNumVertices = numVertices;
//		dstMesh.mVertices = new aiVector3D[numVertices];
//		std::copy( pSrcMesh->mPositions.begin() + pStartVertex, pSrcMesh->mPositions.begin() + 
//			pStartVertex + numVertices, dstMesh->mVertices);
		dstMesh.mVertices = MemoryUtil.subBuffer(pSrcMesh.mPositions, pStartVertex * 3, (pStartVertex + numVertices) * 3);

		// normals, if given. HACK: (thom) Due to the glorious Collada spec we never 
		// know if we have the same number of normals as there are positions. So we 
		// also ignore any vertex attribute if it has a different count
		if( pSrcMesh.mNormals != null && pSrcMesh.mNormals.remaining()/3 >= pStartVertex + numVertices)
		{
//			dstMesh->mNormals = new aiVector3D[numVertices];
//			std::copy( pSrcMesh->mNormals.begin() + pStartVertex, pSrcMesh->mNormals.begin() +
//				pStartVertex + numVertices, dstMesh->mNormals);
			dstMesh.mNormals = MemoryUtil.subBuffer(pSrcMesh.mNormals, pStartVertex * 3, (pStartVertex + numVertices) * 3);
		}

		// tangents, if given. 
		if( pSrcMesh.mTangents != null && pSrcMesh.mTangents.remaining()/3 >= pStartVertex + numVertices)
		{
//			dstMesh->mTangents = new aiVector3D[numVertices];
//			std::copy( pSrcMesh->mTangents.begin() + pStartVertex, pSrcMesh->mTangents.begin() + 
//				pStartVertex + numVertices, dstMesh->mTangents);
			dstMesh.mTangents = MemoryUtil.subBuffer(pSrcMesh.mTangents, pStartVertex * 3, (pStartVertex + numVertices) * 3);
		}

		// bitangents, if given. 
		if( pSrcMesh.mBitangents != null && pSrcMesh.mBitangents.remaining()/3 >= pStartVertex + numVertices)
		{
//			dstMesh->mBitangents = new aiVector3D[numVertices];
//			std::copy( pSrcMesh->mBitangents.begin() + pStartVertex, pSrcMesh->mBitangents.begin() + 
//				pStartVertex + numVertices, dstMesh->mBitangents);
			dstMesh.mBitangents = MemoryUtil.subBuffer(pSrcMesh.mBitangents, pStartVertex * 3, (pStartVertex + numVertices) * 3);
		}

		// same for texturecoords, as many as we have
		// empty slots are not allowed, need to pack and adjust UV indexes accordingly
		for( int a = 0, real = 0; a < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; a++)
		{
			if( pSrcMesh.mTexCoords[a] != null && pSrcMesh.mTexCoords[a].remaining()/3 >= pStartVertex + numVertices)
			{
//				dstMesh.mTextureCoords[real] = new aiVector3D[numVertices];
//				for( size_t b = 0; b < numVertices; ++b)
//					dstMesh->mTextureCoords[real][b] = pSrcMesh->mTexCoords[a][pStartVertex+b];
				dstMesh.mTextureCoords[real] = MemoryUtil.subBuffer(pSrcMesh.mTexCoords[a], pStartVertex * 3, (pStartVertex + numVertices) * 3);
				dstMesh.mNumUVComponents[real] = pSrcMesh.mNumUVComponents[a];
				++real;
			}
		}

		// same for vertex colors, as many as we have. again the same packing to avoid empty slots
		for( int a = 0, real = 0; a < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; a++)
		{
			if( pSrcMesh.mColors[a] != null && pSrcMesh.mColors[a].remaining()/4 >= pStartVertex + numVertices)
			{
//				dstMesh->mColors[real] = new aiColor4D[numVertices];
//				std::copy( pSrcMesh->mColors[a].begin() + pStartVertex, pSrcMesh->mColors[a].begin() + pStartVertex + numVertices,dstMesh->mColors[real]);
				dstMesh.mColors[real] = MemoryUtil.subBuffer(pSrcMesh.mColors[a], pStartVertex * 4, (pStartVertex + numVertices) * 4);
				++real;
			}
		}

		// create faces. Due to the fact that each face uses unique vertices, we can simply count up on each vertex
		int vertex = 0;
//		dstMesh->mNumFaces = pSubMesh.mNumFaces;
//		dstMesh->mFaces = new aiFace[dstMesh->mNumFaces];
		dstMesh.mFaces = new Face[pSubMesh.mNumFaces];
		for( int a = 0; a < dstMesh.mFaces.length; ++a)
		{
			int s = pSrcMesh.mFaceSize.get( pStartFace + a);
			Face face = dstMesh.mFaces[a] = Face.createInstance(s);
//			face.mNumIndices = s;
//			face.mIndices = new unsigned int[s];
			for( int b = 0; b < s; ++b)
//				face.mIndices[b] = vertex++;
				face.set(b, vertex++);
		}

		// create bones if given
		if( pSrcController != null)
		{
			Matrix4f bindShapeMatrix = new Matrix4f();
			// refuse if the vertex count does not match
//			if( pSrcController->mWeightCounts.size() != dstMesh->mNumVertices)
//				throw DeadlyImportError( "Joint Controller vertex count does not match mesh vertex count");

			// resolve references - joint names
			Accessor jointNamesAcc = ColladaParser.resolveLibraryReference( pParser.mAccessorLibrary, pSrcController.mJointNameSource);
			Data jointNames = ColladaParser.resolveLibraryReference( pParser.mDataLibrary, jointNamesAcc.mSource);
			// joint offset matrices
			Accessor jointMatrixAcc = ColladaParser.resolveLibraryReference( pParser.mAccessorLibrary, pSrcController.mJointOffsetMatrixSource);
			Data jointMatrices = ColladaParser.resolveLibraryReference( pParser.mDataLibrary, jointMatrixAcc.mSource);
			// joint vertex_weight name list - should refer to the same list as the joint names above. If not, report and reconsider
			Accessor weightNamesAcc = ColladaParser.resolveLibraryReference( pParser.mAccessorLibrary, pSrcController.mWeightInputJoints.mAccessor);
			if( weightNamesAcc != jointNamesAcc)
				throw new DeadlyImportError( "Temporary implementational lazyness. If you read this, please report to the author.");
			// vertex weights
			Accessor weightsAcc = ColladaParser.resolveLibraryReference( pParser.mAccessorLibrary, pSrcController.mWeightInputWeights.mAccessor);
			Data weights = ColladaParser.resolveLibraryReference( pParser.mDataLibrary, weightsAcc.mSource);

			if( !jointNames.mIsStringArray || jointMatrices.mIsStringArray || weights.mIsStringArray)
				throw new DeadlyImportError( "Data type mismatch while resolving mesh joints");
			// sanity check: we rely on the vertex weights always coming as pairs of BoneIndex-WeightIndex
			if( pSrcController.mWeightInputJoints.mOffset != 0 || pSrcController.mWeightInputWeights.mOffset != 1)
				throw new DeadlyImportError( "Unsupported vertex_weight addressing scheme. ");

			// create containers to collect the weights for each bone
			int numBones = jointNames.mStrings.size();
//			std::vector<std::vector<aiVertexWeight> > dstBones( numBones);
			List<VertexWeight>[] dstBones = new List[numBones];
			AssUtil.initArray(dstBones);

			// build a temporary array of pointers to the start of each vertex's weights
//			typedef std::vector< std::pair<size_t, size_t> > IndexPairVector;
//			std::vector<IndexPairVector::const_iterator> weightStartPerVertex;
//			weightStartPerVertex.resize(pSrcController->mWeightCounts.size(),pSrcController->mWeights.end());
			int[] weightStartPerVertex = new int[pSrcController.mWeightCounts.size()];
//			IndexPairVector::const_iterator pit = pSrcController->mWeights.begin();
//			for( size_t a = 0; a < pSrcController->mWeightCounts.size(); ++a)
			int pit = 0;
			for (int a = 0; a < pSrcController.mWeightCounts.size(); ++a)
			{
				weightStartPerVertex[a] = /*pSrcController.mWeights.get(a)*/ pit;
				pit += pSrcController.mWeightCounts.getInt(a);
			}

			// now for each vertex put the corresponding vertex weights into each bone's weight collection
			for( int a = pStartVertex; a < pStartVertex + numVertices; ++a)
			{
				// which position index was responsible for this vertex? that's also the index by which
				// the controller assigns the vertex weights
				int orgIndex = pSrcMesh.mFacePosIndices.getInt(a);
				// find the vertex weights for this vertex
				/*IndexPairVector::const_iterator*/int iit = weightStartPerVertex[orgIndex];
				int pairCount = pSrcController.mWeightCounts.getInt(orgIndex);

				for( int b = 0; b < pairCount; ++b, ++iit)
				{
					IntPair ip = pSrcController.mWeights.get(iit);
					int jointIndex = ip.first;
					int vertexIndex = ip.second;

					float weight = readFloat( weightsAcc, weights, vertexIndex, 0);

					// one day I gonna kill that XSI Collada exporter
					if( weight > 0.0f)
					{
						VertexWeight w = new VertexWeight();
						w.mVertexId = a - pStartVertex;
						w.mWeight = weight;
						dstBones[jointIndex].add( w);
					}
				}
			}

			// count the number of bones which influence vertices of the current submesh
			int numRemainingBones = 0;
//			for( std::vector<std::vector<aiVertexWeight> >::const_iterator it = dstBones.begin(); it != dstBones.end(); ++it)
			for (List<VertexWeight> it : dstBones)
				if( it.size() > 0)
					numRemainingBones++;

			// create bone array and copy bone weights one by one
//			dstMesh->mNumBones = numRemainingBones;
			dstMesh.mBones = new Bone[numRemainingBones];
			int boneCount = 0;
			for( int a = 0; a < numBones; ++a)
			{
				// omit bones without weights
				if( dstBones[a].size() == 0)
					continue;

				// create bone with its weights
				Bone bone = new Bone();
				bone.mName = readString( jointNamesAcc, jointNames, a);
				bone.mOffsetMatrix.m00 = readFloat( jointMatrixAcc, jointMatrices, a, 0);
				bone.mOffsetMatrix.m10 = readFloat( jointMatrixAcc, jointMatrices, a, 1);
				bone.mOffsetMatrix.m20 = readFloat( jointMatrixAcc, jointMatrices, a, 2);
				bone.mOffsetMatrix.m30 = readFloat( jointMatrixAcc, jointMatrices, a, 3);
				bone.mOffsetMatrix.m01 = readFloat( jointMatrixAcc, jointMatrices, a, 4);
				bone.mOffsetMatrix.m11 = readFloat( jointMatrixAcc, jointMatrices, a, 5);
				bone.mOffsetMatrix.m21 = readFloat( jointMatrixAcc, jointMatrices, a, 6);
				bone.mOffsetMatrix.m31 = readFloat( jointMatrixAcc, jointMatrices, a, 7);
				bone.mOffsetMatrix.m02 = readFloat( jointMatrixAcc, jointMatrices, a, 8);
				bone.mOffsetMatrix.m12 = readFloat( jointMatrixAcc, jointMatrices, a, 9);
				bone.mOffsetMatrix.m22 = readFloat( jointMatrixAcc, jointMatrices, a, 10);
				bone.mOffsetMatrix.m32 = readFloat( jointMatrixAcc, jointMatrices, a, 11);
//				bone.mNumWeights = dstBones[a].size();
//				bone.mWeights = new VertexWeight[bone.mNumWeights];
//				std::copy( dstBones[a].begin(), dstBones[a].end(), bone.mWeights);
				bone.mWeights = AssUtil.toArray(dstBones[a], VertexWeight.class);

				// apply bind shape matrix to offset matrix
//				aiMatrix4x4 bindShapeMatrix;
//				bindShapeMatrix.a1 = pSrcController->mBindShapeMatrix[0];
//				bindShapeMatrix.a2 = pSrcController->mBindShapeMatrix[1];
//				bindShapeMatrix.a3 = pSrcController->mBindShapeMatrix[2];
//				bindShapeMatrix.a4 = pSrcController->mBindShapeMatrix[3];
//				bindShapeMatrix.b1 = pSrcController->mBindShapeMatrix[4];
//				bindShapeMatrix.b2 = pSrcController->mBindShapeMatrix[5];
//				bindShapeMatrix.b3 = pSrcController->mBindShapeMatrix[6];
//				bindShapeMatrix.b4 = pSrcController->mBindShapeMatrix[7];
//				bindShapeMatrix.c1 = pSrcController->mBindShapeMatrix[8];
//				bindShapeMatrix.c2 = pSrcController->mBindShapeMatrix[9];
//				bindShapeMatrix.c3 = pSrcController->mBindShapeMatrix[10];
//				bindShapeMatrix.c4 = pSrcController->mBindShapeMatrix[11];
//				bindShapeMatrix.d1 = pSrcController->mBindShapeMatrix[12];
//				bindShapeMatrix.d2 = pSrcController->mBindShapeMatrix[13];
//				bindShapeMatrix.d3 = pSrcController->mBindShapeMatrix[14];
//				bindShapeMatrix.d4 = pSrcController->mBindShapeMatrix[15];
//				bone.mOffsetMatrix *= bindShapeMatrix;
				
				bindShapeMatrix.loadTranspose(pSrcController.mBindShapeMatrix, 0);
				Matrix4f.mul(bone.mOffsetMatrix, bindShapeMatrix, bone.mOffsetMatrix);

				// HACK: (thom) Some exporters address the bone nodes by SID, others address them by ID or even name.
				// Therefore I added a little name replacement here: I search for the bone's node by either name, ID or SID,
				// and replace the bone's name by the node's name so that the user can use the standard
				// find-by-name method to associate nodes with bones.
				COLNode bnode = findNode( pParser.mRootNode, bone.mName);
				if(bnode == null)
					bnode = findNodeBySID( pParser.mRootNode, bone.mName);

				// assign the name that we would have assigned for the source node
				if( bnode != null)
					bone.mName = findNameForNode( bnode);
				else
					DefaultLogger.warn(String.format("ColladaLoader::CreateMesh(): could not find corresponding node for joint \"%s\".", bone.mName));

				// and insert bone
				dstMesh.mBones[boneCount++] = bone;
			}
		}

		return dstMesh;
	}

	/** Builds cameras for the given node and references them */
	void buildCamerasForNode(ColladaParser pParser, COLNode pNode, Node pTarget){
//		BOOST_FOREACH( const Collada::CameraInstance& cid, pNode->mCameras)
		for (CameraInstance cid : pNode.mCameras)
		{
			// find the referred light
//			ColladaParser::CameraLibrary::const_iterator srcCameraIt = pParser.mCameraLibrary.find( cid.mCamera);
			COLCamera srcCamera = pParser.mCameraLibrary.get(cid.mCamera);
//			if( srcCameraIt == pParser.mCameraLibrary.end())
			if(srcCamera == null)
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Collada: Unable to find camera for ID \"" + cid.mCamera + "\". Skipping.");
				continue;
			}
//			const Collada::Camera* srcCamera = &srcCameraIt->second;

			// orthographic cameras not yet supported in Assimp
			if (DefaultLogger.LOG_OUT && srcCamera.mOrtho) {
				DefaultLogger.warn("Collada: Orthographic cameras are not supported.");
			}

			// now fill our ai data structure
			Camera out = new Camera();
			out.mName = pTarget.mName;

			// collada cameras point in -Z by default, rest is specified in node transform
			out.mLookAt.set(0.f,0.f,-1.f);

			// near/far z is already ok
			out.mClipPlaneFar = srcCamera.mZFar;
			out.mClipPlaneNear = srcCamera.mZNear;

			// ... but for the rest some values are optional 
			// and we need to compute the others in any combination. 
			 if (srcCamera.mAspect != 10e10f)
				out.mAspect = srcCamera.mAspect;

			if (srcCamera.mHorFov != 10e10f) {
				out.mHorizontalFOV = srcCamera.mHorFov; 

				if (srcCamera.mVerFov != 10e10f && srcCamera.mAspect == 10e10f) {
					out.mAspect = (float) (Math.tan(Math.toRadians(srcCamera.mHorFov)) /
	                    Math.tan(Math.toRadians(srcCamera.mVerFov)));
				}
			}
			else if (srcCamera.mAspect != 10e10f && srcCamera.mVerFov != 10e10f)	{
				out.mHorizontalFOV = (float) (2.0f * Math.toDegrees(Math.atan(srcCamera.mAspect *
						Math.tan(Math.toRadians(srcCamera.mVerFov) * 0.5f))));
			}

			// Collada uses degrees, we use radians
			out.mHorizontalFOV = (float) Math.toRadians(out.mHorizontalFOV);

			// add to camera list
			mCameras.add(out);
		}
	}

	/** Builds lights for the given node and references them */
	void buildLightsForNode(ColladaParser pParser, COLNode pNode, Node pTarget){
//		BOOST_FOREACH( const Collada::LightInstance& lid, pNode->mLights)
		for (LightInstance lid : pNode.mLights)
		{
			// find the referred light
//			ColladaParser::LightLibrary::const_iterator srcLightIt = pParser.mLightLibrary.find( lid.mLight);
			COLLight srcLightIt = pParser.mLightLibrary.get(lid.mLight);
			if( /*srcLightIt == pParser.mLightLibrary.end()*/ srcLightIt == null)
			{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Collada: Unable to find light for ID \"" + lid.mLight + "\". Skipping.");
				continue;
			}
//			const Collada::Light* srcLight = &srcLightIt->second;
			COLLight srcLight = srcLightIt;
			if (srcLight.mType == ColladaParser.aiLightSource_AMBIENT) {
				DefaultLogger.error("Collada: Skipping ambient light for the moment");
				continue;
			}
			
			// now fill our ai data structure
			Light out = new Light();
			out.mName = pTarget.mName;
			out.mType = LightSourceType.values()[srcLight.mType];

			// collada lights point in -Z by default, rest is specified in node transform
			out.mDirection.set(0.f,0.f,-1.f);

			out.mAttenuationConstant  = srcLight.mAttConstant;
			out.mAttenuationLinear    = srcLight.mAttLinear;
			out.mAttenuationQuadratic = srcLight.mAttQuadratic;

			// collada doesn't differenciate between these color types
//			out->mColorDiffuse = out->mColorSpecular = out->mColorAmbient = srcLight->mColor*srcLight->mIntensity;
			Vector3f v = out.mColorDiffuse;
			v.set(srcLight.mColor);
			v.scale(srcLight.mIntensity);
			out.mColorAmbient.set(v);
			out.mColorSpecular.set(v);

			// convert falloff angle and falloff exponent in our representation, if given
			if (out.mType == LightSourceType.aiLightSource_SPOT) {
				
				out.mAngleInnerCone = (float)Math.toRadians( srcLight.mFalloffAngle );

				// ... some extension magic. 
				if (srcLight.mOuterAngle >= ColladaParser.ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET*(1-1e-6f))
				{
					// ... some deprecation magic. 
					if (srcLight.mPenumbraAngle >= ColladaParser.ASSIMP_COLLADA_LIGHT_ANGLE_NOT_SET*(1-1e-6f))
					{
						// Need to rely on falloff_exponent. I don't know how to interpret it, so I need to guess ....
						// epsilon chosen to be 0.1
						out.mAngleOuterCone = (float)Math.toRadians (Math.acos(Math.pow(0.1,1./srcLight.mFalloffExponent))+
							srcLight.mFalloffAngle);
					}
					else {
						out.mAngleOuterCone = out.mAngleInnerCone + (float)Math.toRadians(  srcLight.mPenumbraAngle );
						if (out.mAngleOuterCone < out.mAngleInnerCone){
//							std::swap(out.mAngleInnerCone,out.mAngleOuterCone);
							float t = out.mAngleInnerCone;
							out.mAngleInnerCone = out.mAngleOuterCone;
							out.mAngleOuterCone = t;
						}
					}
				}
				else out.mAngleOuterCone = (float)Math.toRadians(  srcLight.mOuterAngle );
			}

			// add to light list
			mLights.add(out);
		}
	}

	/** Stores all meshes in the given scene */
	void storeSceneMeshes(Scene pScene){
//		pScene->mNumMeshes = mMeshes.size();
//		if( mMeshes.size() > 0)
//		{
//			pScene->mMeshes = new aiMesh*[mMeshes.size()];
//			std::copy( mMeshes.begin(), mMeshes.end(), pScene->mMeshes);
//			mMeshes.clear();
//		}
		
		pScene.mMeshes = AssUtil.toArray(mMeshes, Mesh.class);
	}

	/** Stores all materials in the given scene */
	void storeSceneMaterials( Scene pScene){
		if (!AssUtil.isEmpty(newMats)) {
			pScene.mMaterials = new Material[newMats.size()];
			for (int i = 0; i < newMats.size();++i)
				pScene.mMaterials[i] = newMats.get(i).second;

			newMats.clear();
		}
	}

	/** Stores all lights in the given scene */
	void storeSceneLights( Scene pScene){
//		pScene->mNumLights = mLights.size();
//		if( mLights.size() > 0)
//		{
//			pScene->mLights = new aiLight*[mLights.size()];
//			std::copy( mLights.begin(), mLights.end(), pScene->mLights);
//			mLights.clear();
//		}
		
		pScene.mLights = AssUtil.toArray(mLights, Light.class);
	}

	/** Stores all cameras in the given scene */
	void storeSceneCameras( Scene pScene){
//		pScene->mNumCameras = mCameras.size();
//		if( mCameras.size() > 0)
//		{
//			pScene->mCameras = new aiCamera*[mCameras.size()];
//			std::copy( mCameras.begin(), mCameras.end(), pScene->mCameras);
//			mCameras.clear();
//		}
		
		pScene.mCameras = AssUtil.toArray(mCameras, Camera.class);
	}

	/** Stores all textures in the given scene */
	void storeSceneTextures( Scene pScene){
//		pScene->mNumTextures = mTextures.size();
//		if( mTextures.size() > 0)
//		{
//			pScene->mTextures = new aiTexture*[mTextures.size()];
//			std::copy( mTextures.begin(), mTextures.end(), pScene->mTextures);
//			mTextures.clear();
//		}
		
		pScene.mTextures = AssUtil.toArray(mTextures, Texture.class);
	}

	/** Stores all animations 
	 * @param pScene target scene to store the anims
	 */
	void storeAnimations( Scene pScene,ColladaParser pParser){
		// recursivly collect all animations from the collada scene
		storeAnimations( pScene, pParser, pParser.mAnims, "");

		// catch special case: many animations with the same length, each affecting only a single node.
		// we need to unite all those single-node-anims to a proper combined animation
		for( int a = 0; a < mAnims.size(); ++a)
		{
			Animation templateAnim = mAnims.get(a);
			if( templateAnim.getNumChannels() == 1)
			{
				// search for other single-channel-anims with the same duration
//				std::vector<size_t> collectedAnimIndices;
				IntArrayList collectedAnimIndices = new IntArrayList();
				for( int b = a+1; b < mAnims.size(); ++b)
				{
					Animation other = mAnims.get(b);
					if( other.getNumChannels() == 1 && other.mDuration == templateAnim.mDuration && other.mTicksPerSecond == templateAnim.mTicksPerSecond )
						collectedAnimIndices.add( b);
				}

				// if there are other animations which fit the template anim, combine all channels into a single anim
				if( !collectedAnimIndices.isEmpty() )
				{
					Animation combinedAnim = new Animation();
					combinedAnim.mName = "combinedAnim_"+ (char)( '0' + a);
					combinedAnim.mDuration = templateAnim.mDuration;
					combinedAnim.mTicksPerSecond = templateAnim.mTicksPerSecond;
//					combinedAnim.mNumChannels = collectedAnimIndices.size() + 1;
					combinedAnim.mChannels = new NodeAnim[collectedAnimIndices.size() + 1/*combinedAnim.mNumChannels*/];
					// add the template anim as first channel by moving its aiNodeAnim to the combined animation
					combinedAnim.mChannels[0] = templateAnim.mChannels[0];
					templateAnim.mChannels[0] = null;
//					delete templateAnim;
					// combined animation replaces template animation in the anim array
					mAnims.set(a, combinedAnim);

					// move the memory of all other anims to the combined anim and erase them from the source anims
					for( int b = 0; b < collectedAnimIndices.size(); ++b)
					{
						Animation srcAnimation = mAnims.get(collectedAnimIndices.getInt(b));
						combinedAnim.mChannels[1 + b] = srcAnimation.mChannels[0];
						srcAnimation.mChannels[0] = null;
//						delete srcAnimation;
					}

					// in a second go, delete all the single-channel-anims that we've stripped from their channels
					// back to front to preserve indices - you know, removing an element from a vector moves all elements behind the removed one
					while( !collectedAnimIndices.isEmpty() )
					{
//						mAnims.erase( mAnims.begin() + collectedAnimIndices.back());
//						collectedAnimIndices.pop_back();
						mAnims.remove(collectedAnimIndices.popInt());
					}
				}
			}
		}

		// now store all anims in the scene
		if( !mAnims.isEmpty())
		{
//			pScene->mNumAnimations = mAnims.size();
//			pScene->mAnimations = new aiAnimation*[mAnims.size()];
//			std::copy( mAnims.begin(), mAnims.end(), pScene->mAnimations);
			pScene.mAnimations = AssUtil.toArray(mAnims, Animation.class);
		}

		mAnims.clear();
	}

	/** Stores all animations for the given source anim and its nested child animations
	 * @param pScene target scene to store the anims
	 * @param pSrcAnim the source animation to process
	 * @param pPrefix Prefix to the name in case of nested animations
	 */
	void storeAnimations( Scene pScene,ColladaParser pParser, COLAnimation pSrcAnim, String pPrefix){
		String animName = pPrefix.isEmpty() ? pSrcAnim.mName : pPrefix + "_" + pSrcAnim.mName;

		// create nested animations, if given
//		for( std::vector<Collada::Animation*>::const_iterator it = pSrcAnim->mSubAnims.begin(); it != pSrcAnim->mSubAnims.end(); ++it)
		for (COLAnimation it : pSrcAnim.mSubAnims)
			storeAnimations( pScene, pParser, it, animName);

		// create animation channels, if any
		if( !AssUtil.isEmpty(pSrcAnim.mChannels))
			createAnimation( pScene, pParser, pSrcAnim, animName);
	}

	/** Constructs the animation for the given source anim */
	void createAnimation( Scene pScene,ColladaParser pParser, COLAnimation pSrcAnim, String pName){
		// collect a list of animatable nodes
//		std::vector<const aiNode*> nodes;
		ArrayList<Node> nodes = new ArrayList<Node>();
		collectNodes( pScene.mRootNode, nodes);

//		std::vector<aiNodeAnim*> anims;
		ArrayList<NodeAnim> anims = new ArrayList<NodeAnim>();
//		for( std::vector<const aiNode*>::const_iterator nit = nodes.begin(); nit != nodes.end(); ++nit)
		for (Node nit : nodes)
		{
			// find all the collada anim channels which refer to the current node
//			std::vector<Collada::ChannelEntry> entries;
			ArrayList<ChannelEntry> entries = new ArrayList<ChannelEntry>();
			String nodeName = nit.mName;

			// find the collada node corresponding to the aiNode
			COLNode srcNode = findNode( pParser.mRootNode, nodeName);
//			ai_assert( srcNode != NULL);
			if( srcNode == null)
				continue;

			// now check all channels if they affect the current node
//			for( std::vector<Collada::AnimationChannel>::const_iterator cit = pSrcAnim->mChannels.begin();
//				cit != pSrcAnim->mChannels.end(); ++cit)
			for (AnimationChannel srcChannel : pSrcAnim.mChannels)
			{
				ChannelEntry entry = new ChannelEntry();

				// we expect the animation target to be of type "nodeName/transformID.subElement". Ignore all others
				// find the slash that separates the node name - there should be only one
				int slashPos = srcChannel.mTarget.indexOf( '/');
				if( slashPos ==  -1/*std::string::npos*/)
					continue;
				if( srcChannel.mTarget.indexOf( '/', slashPos+1) != -1/*std::string::npos*/)
					continue;
				String targetID = srcChannel.mTarget.substring( 0, slashPos);
				if( targetID.compareTo(srcNode.mID) != 0)
					continue;

				// find the dot that separates the transformID - there should be only one or zero
				int dotPos = srcChannel.mTarget.indexOf( '.');
				if( dotPos !=  -1/*std::string::npos*/)
				{
					if( srcChannel.mTarget.indexOf( '.', dotPos+1) != -1/*std::string::npos*/)
						continue;

					entry.mTransformId = srcChannel.mTarget.substring( slashPos+1,dotPos - slashPos - 1);

					String subElement = srcChannel.mTarget.substring( dotPos+1);
					if( subElement.equals("ANGLE"))
						entry.mSubElement = 3; // last number in an Axis-Angle-Transform is the angle
					else if( subElement.equals("X"))
						entry.mSubElement = 0;
					else if( subElement.equals("Y"))
						entry.mSubElement = 1;
					else if( subElement.equals("Z"))
						entry.mSubElement = 2;
					else if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn(String.format("Unknown anim subelement <%s>. Ignoring", subElement));
				} else
				{
					// no subelement following, transformId is remaining string
					entry.mTransformId = srcChannel.mTarget.substring( slashPos+1);
				}

				// determine which transform step is affected by this channel
				entry.mTransformIndex = -1;
				for( int a = 0; a < srcNode.mTransforms.size(); ++a)
					if( srcNode.mTransforms.get(a).mID.equals(entry.mTransformId))
						entry.mTransformIndex = a;

				if( entry.mTransformIndex == -1) {
					continue;
				}

				entry.mChannel = srcChannel/*&(*cit)*/;
				entries.add( entry);
			}

			// if there's no channel affecting the current node, we skip it
			if( entries.isEmpty())
				continue;

			// resolve the data pointers for all anim channels. Find the minimum time while we're at it
			float startTime = 1e20f, endTime = -1e20f;
//			for( std::vector<Collada::ChannelEntry>::iterator it = entries.begin(); it != entries.end(); ++it)
			for( ChannelEntry e : entries)
			{
//				Collada::ChannelEntry& e = *it;
				e.mTimeAccessor = ColladaParser.resolveLibraryReference( pParser.mAccessorLibrary, e.mChannel.mSourceTimes);
				e.mTimeData = ColladaParser.resolveLibraryReference( pParser.mDataLibrary, e.mTimeAccessor.mSource);
				e.mValueAccessor = ColladaParser.resolveLibraryReference( pParser.mAccessorLibrary, e.mChannel.mSourceValues);
				e.mValueData = ColladaParser.resolveLibraryReference( pParser.mDataLibrary, e.mValueAccessor.mSource);

				// time count and value count must match
				if( e.mTimeAccessor.mCount != e.mValueAccessor.mCount)
					throw new DeadlyImportError(String.format("Time count / value count mismatch in animation channel \"%s\".", e.mChannel.mTarget));

		        if( e.mTimeAccessor.mCount > 0 )
		        {
					  // find bounding times
					  startTime = Math.min( startTime, readFloat( e.mTimeAccessor, e.mTimeData, 0, 0));
		  			   endTime = Math.max( endTime, readFloat( e.mTimeAccessor, e.mTimeData, e.mTimeAccessor.mCount-1,0));
		        }
			}

//	    std::vector<aiMatrix4x4> resultTrafos;
	    ArrayList<Matrix4f> resultTrafos = new ArrayList<Matrix4f>();
	    if( !entries.isEmpty() && entries.get(0).mTimeAccessor.mCount > 0 )
	    {
			  // create a local transformation chain of the node's transforms
//			  std::vector<Collada::Transform> transforms = srcNode->mTransforms;
	    	  ArrayList<Transform> transforms = new ArrayList<Transform>(srcNode.mTransforms.size());
	    	  for(Transform t : srcNode.mTransforms)
	    		  transforms.add(new Transform(t));  // value copy

			  // now for every unique point in time, find or interpolate the key values for that time
			  // and apply them to the transform chain. Then the node's present transformation can be calculated.
			  float time = startTime;
			  while( true)
			  {
//				  for( std::vector<Collada::ChannelEntry>::iterator it = entries.begin(); it != entries.end(); ++it)
				  for (ChannelEntry e : entries)
				  {
//					  Collada::ChannelEntry& e = *it;

					  // find the keyframe behind the current point in time
					  int pos = 0;
					  float postTime = 0.f;
					  while(true)
					  {
						  if( pos >= e.mTimeAccessor.mCount)
							  break;
						  postTime = readFloat( e.mTimeAccessor, e.mTimeData, pos, 0);
						  if( postTime >= time)
							  break;
						  ++pos;
					  }

					  pos = Math.min( pos, e.mTimeAccessor.mCount-1);

					  // read values from there
					  float[] temp = new float[16];
					  for( int c = 0; c < e.mValueAccessor.mSize; ++c)
						  temp[c] = readFloat(e.mValueAccessor, e.mValueData, pos, c);

					  // if not exactly at the key time, interpolate with previous value set
					  if( postTime > time && pos > 0)
					  {
						  float preTime = readFloat(e.mTimeAccessor,e.mTimeData, pos-1,0);
						  float factor = (time - postTime) / (preTime - postTime);

						  for( int c = 0; c < e.mValueAccessor.mSize; ++c)
						  {
							  float v = readFloat( e.mValueAccessor, e.mValueData, pos-1,c);
							  temp[c] += (v - temp[c]) * factor;
						  }
					  }

					  // Apply values to current transformation
//					  std::copy( temp, temp + e.mValueAccessor->mSize, transforms[e.mTransformIndex].f + e.mSubElement);
					  System.arraycopy(temp, 0, transforms.get(e.mTransformIndex).f, 0, e.mValueAccessor.mSize);
				  }

				  // Calculate resulting transformation
				  Matrix4f mat = new Matrix4f();
				  pParser.calculateResultTransform( transforms, mat);
				  // out of lazyness: we store the time in matrix.d4
				  mat.m33 = time;
				  resultTrafos.add(mat);

				  // find next point in time to evaluate. That's the closest frame larger than the current in any channel
				  float nextTime = 1e20f;
//				  for( std::vector<Collada::ChannelEntry>::iterator it = entries.begin(); it != entries.end(); ++it)
				  for (ChannelEntry e : entries)
				  {
//					  Collada::ChannelEntry& e = *it;

					  // find the next time value larger than the current
					  int pos = 0;
					  while( pos < e.mTimeAccessor.mCount)
					  {
						  float t = readFloat(e.mTimeAccessor, e.mTimeData, pos, 0);
						  if( t > time)
						  {
							  nextTime = Math.min( nextTime, t);
							  break;
						  }
						  ++pos;
					  }
				  }

				  // no more keys on any channel after the current time -> we're done
				  if( nextTime > 1e19)
					  break;

				  // else construct next keyframe at this following time point
				  time = nextTime;
			  }
	    }

			// there should be some keyframes, but we aren't that fixated on valid input data
//			ai_assert( resultTrafos.size() > 0);

			// build an animation channel for the given node out of these trafo keys
	    if( !resultTrafos.isEmpty() )
	    {
			  NodeAnim dstAnim = new NodeAnim();
			  dstAnim.mNodeName = nodeName;
//			  dstAnim.mNumPositionKeys = resultTrafos.size();
//			  dstAnim.mNumRotationKeys= resultTrafos.size();
//			  dstAnim.mNumScalingKeys = resultTrafos.size();
			  dstAnim.mPositionKeys = AssUtil.initArray(new VectorKey[resultTrafos.size()]);
			  dstAnim.mRotationKeys = AssUtil.initArray(new QuatKey[resultTrafos.size()]);
			  dstAnim.mScalingKeys = AssUtil.initArray(new VectorKey[resultTrafos.size()]);

			  for( int a = 0; a < resultTrafos.size(); ++a)
			  {
				  Matrix4f mat = resultTrafos.get(a);
				  float time =  mat.m33; // remember? time is stored in mat.d4
				  mat.m33 = 1.0f;

				  dstAnim.mPositionKeys[a].mTime = time;
				  dstAnim.mRotationKeys[a].mTime = time;
				  dstAnim.mScalingKeys[a].mTime = time;
//				  mat.decompose( dstAnim.mScalingKeys[a].mValue, dstAnim.mRotationKeys[a].mValue, dstAnim.mPositionKeys[a].mValue);
				  AssUtil.decompose(mat, dstAnim.mScalingKeys[a].mValue, dstAnim.mRotationKeys[a].mValue, dstAnim.mPositionKeys[a].mValue);
			  }

			  anims.add( dstAnim);
	    } else
	    {
	    	if(DefaultLogger.LOG_OUT)
	    		DefaultLogger.warn( "Collada loader: found empty animation channel, ignored. Please check your exporter.");
	    }
		}

		if( !anims.isEmpty())
		{
			Animation anim = new Animation();
			anim.mName = ( pName);
//			anim.mNumChannels = anims.size();
//			anim.mChannels = new NodeAnim*[anims.size()];
//			std::copy( anims.begin(), anims.end(), anim.mChannels);
			anim.mChannels = AssUtil.toArray(anims, NodeAnim.class);
			anim.mDuration = 0.0f;
			for( int a = 0; a < anims.size(); ++a)
			{
				NodeAnim _a = anims.get(a);
				anim.mDuration = Math.max( anim.mDuration, _a.mPositionKeys[_a.getNumPositionKeys()-1].mTime);
				anim.mDuration = Math.max( anim.mDuration, _a.mRotationKeys[_a.getNumRotationKeys()-1].mTime);
				anim.mDuration = Math.max( anim.mDuration, _a.mScalingKeys[_a.getNumScalingKeys()-1].mTime);
			}
			anim.mTicksPerSecond = 1;
			mAnims.add( anim);
		}
	}
	
	/** Constructs materials from the collada material definitions */
	void buildMaterials( ColladaParser pParser, Scene pScene){
//		newMats.reserve(pParser.mMaterialLibrary.size());
		if(newMats == null)
			newMats = new ArrayList<Pair<Effect,Material>>(pParser.mMaterialLibrary.size());
		else
			newMats.ensureCapacity(pParser.mMaterialLibrary.size());

//		for( ColladaParser::MaterialLibrary::const_iterator matIt = pParser.mMaterialLibrary.begin(); matIt != pParser.mMaterialLibrary.end(); ++matIt)
		for( Map.Entry<String, COLMaterial> matIt : pParser.mMaterialLibrary.entrySet())
		{
			COLMaterial material = matIt.getValue();
			// a material is only a reference to an effect
//			ColladaParser::EffectLibrary::iterator effIt = pParser.mEffectLibrary.find( material.mEffect);
//			if( effIt == pParser.mEffectLibrary.end())
//				continue;
//			Collada::Effect& effect = effIt->second;
			Effect effect = pParser.mEffectLibrary.get(material.mEffect);
			if(effect == null)
				continue;

			// create material
			Material mat = new Material();
//			aiString name( matIt->first);
			mat.addProperty(matIt.getKey(),Material.AI_MATKEY_NAME, 0,0);

			// store the material
//			mMaterialIndexByName[matIt->first] = newMats.size();
//			newMats.push_back( std::pair<Collada::Effect*, aiMaterial*>( &effect,mat) );
			mMaterialIndexByName.put(matIt.getKey(), newMats.size());
			newMats.add(new Pair<Effect, Material>(effect, mat));
		}
	}

	/** Fill materials from the collada material definitions */
	void fillMaterials(ColladaParser pParser, Scene pScene){
//		for (std::vector<std::pair<Collada::Effect*, aiMaterial*> >::iterator it = newMats.begin(),
//				end = newMats.end(); it != end; ++it)
		final ShadingMode[] modes = ShadingMode.values();
		for (Pair<Effect, Material> it : newMats)
			{
//				aiMaterial&  mat = (aiMaterial&)*it->second; 
//				Collada::Effect& effect = *it->first;
				Effect effect = it.first;
				Material mat = it.second;

				// resolve shading mode
				ShadingMode shadeMode;
				if (effect.mFaceted) /* fixme */
					shadeMode = ShadingMode.aiShadingMode_Flat;
				else {
					switch( effect.mShadeType)
					{
					case COLEnum.Shade_Constant: 
						shadeMode = ShadingMode.aiShadingMode_NoShading; 
						break;
					case COLEnum.Shade_Lambert:
						shadeMode = ShadingMode.aiShadingMode_Gouraud; 
						break;
					case COLEnum.Shade_Blinn: 
						shadeMode = ShadingMode.aiShadingMode_Blinn;
						break;
					case COLEnum.Shade_Phong: 
						shadeMode = ShadingMode.aiShadingMode_Phong; 
						break;

					default:
						if(DefaultLogger.LOG_OUT)
							DefaultLogger.warn("Collada: Unrecognized shading mode, using gouraud shading");
						shadeMode = ShadingMode.aiShadingMode_Gouraud; 
						break;
					}
				}
				mat.addProperty(shadeMode.ordinal(), Material.AI_MATKEY_SHADING_MODEL, 0,0);

				// double-sided?
				shadeMode = modes[effect.mDoubleSided ? 1 : 0 + 1];
				mat.addProperty(shadeMode.ordinal(), Material.AI_MATKEY_TWOSIDED, 0, 0);

				// wireframe?
				shadeMode = modes[effect.mWireframe ? 1 : 0 + 1];
				mat.addProperty(shadeMode.ordinal(), Material.AI_MATKEY_ENABLE_WIREFRAME, 0,0);

				// add material colors
				mat.addProperty( effect.mAmbient, Material.AI_MATKEY_COLOR_AMBIENT, 0,0);
				mat.addProperty( effect.mDiffuse, Material.AI_MATKEY_COLOR_DIFFUSE, 0,0);
				mat.addProperty( effect.mSpecular, Material.AI_MATKEY_COLOR_SPECULAR, 0,0);
				mat.addProperty( effect.mEmissive, Material.AI_MATKEY_COLOR_EMISSIVE, 0,0);
				mat.addProperty( effect.mTransparent, Material.AI_MATKEY_COLOR_TRANSPARENT, 0,0);
				mat.addProperty( effect.mReflective, Material.AI_MATKEY_COLOR_REFLECTIVE, 0,0);

				// scalar properties
				mat.addProperty( effect.mShininess, Material.AI_MATKEY_SHININESS, 0,0);
				mat.addProperty( effect.mReflectivity, Material.AI_MATKEY_REFLECTIVITY, 0,0);
				mat.addProperty( effect.mRefractIndex, Material.AI_MATKEY_REFRACTI, 0,0);

				// transparency, a very hard one. seemingly not all files are following the
				// specification here .. but we can trick.
				if (effect.mTransparency >= 0.f && effect.mTransparency < 1.f) {
					effect.mTransparency = 1.f- effect.mTransparency;
					mat.addProperty( effect.mTransparency, Material.AI_MATKEY_OPACITY , 0,0);
					mat.addProperty( effect.mTransparent, Material.AI_MATKEY_COLOR_TRANSPARENT, 0,0 );
				}

				// add textures, if given
				if( !effect.mTexAmbient.mName.isEmpty()) 
					 /* It is merely a lightmap */
					addTexture( mat, pParser, effect, effect.mTexAmbient, TextureType.aiTextureType_LIGHTMAP.ordinal(),0);

				if( !effect.mTexEmissive.mName.isEmpty())
					addTexture( mat, pParser, effect, effect.mTexEmissive, TextureType.aiTextureType_EMISSIVE.ordinal(),0);

				if( !effect.mTexSpecular.mName.isEmpty())
					addTexture( mat, pParser, effect, effect.mTexSpecular, TextureType.aiTextureType_SPECULAR.ordinal(),0);

				if( !effect.mTexDiffuse.mName.isEmpty())
					addTexture( mat, pParser, effect, effect.mTexDiffuse, TextureType.aiTextureType_DIFFUSE.ordinal(),0);

				if( !effect.mTexBump.mName.isEmpty())
					addTexture( mat, pParser, effect, effect.mTexBump, TextureType.aiTextureType_NORMALS.ordinal(),0);

				if( !effect.mTexTransparent.mName.isEmpty())
					addTexture( mat, pParser, effect, effect.mTexTransparent, TextureType.aiTextureType_OPACITY.ordinal(),0);

				if( !effect.mTexReflective.mName.isEmpty())
					addTexture( mat, pParser, effect, effect.mTexReflective, TextureType.aiTextureType_REFLECTION.ordinal(),0);
			}
	}

	/** Resolve UV channel mappings*/
	void applyVertexToEffectSemanticMapping(Sampler sampler, SemanticMappingTable table){
//		std::map<std::string, Collada::InputSemanticMapEntry>::const_iterator it = table.mMap.find(sampler.mUVChannel);
//		if (it != table.mMap.end()) {
//			if (it->second.mType != Collada::IT_Texcoord)
//				DefaultLogger::get()->error("Collada: Unexpected effect input mapping");
//
//			sampler.mUVId = it->second.mSet;
//		}
		
		InputSemanticMapEntry it = table.mMap.get(sampler.mUVChannel);
		if(it != null){
			if(it.mType != COLEnum.IT_Texcoord)
				DefaultLogger.error("Collada: Unexpected effect input mapping");
			
			sampler.mUVId = it.mSet;
		}
	}

	/** Add a texture and all of its sampling properties to a material*/
	void addTexture ( Material mat,ColladaParser pParser, Effect effect, Sampler sampler, int/*TextureType*/ type, int idx /*= 0*/){
		// first of all, basic file name
		String name = findFilenameForEffectTexture( pParser, effect, sampler.mName );
		mat.addProperty(name, Material._AI_MATKEY_TEXTURE_BASE, type, idx );

		// mapping mode
		int map = TextureMapMode.aiTextureMapMode_Clamp.ordinal();
		if (sampler.mWrapU)
			map = TextureMapMode.aiTextureMapMode_Wrap.ordinal();
		if (sampler.mWrapU && sampler.mMirrorU)
			map = TextureMapMode.aiTextureMapMode_Mirror.ordinal();

		mat.addProperty(map, Material._AI_MATKEY_MAPPINGMODE_U_BASE, type, idx);

		map = TextureMapMode.aiTextureMapMode_Clamp.ordinal();
		if (sampler.mWrapV)
			map = TextureMapMode.aiTextureMapMode_Wrap.ordinal();
		if (sampler.mWrapV && sampler.mMirrorV)
			map = TextureMapMode.aiTextureMapMode_Mirror.ordinal();

		mat.addProperty(map, Material._AI_MATKEY_MAPPINGMODE_V_BASE, type, idx);

		// UV transformation
		mat.addProperty(sampler.mTransform,
				Material._AI_MATKEY_UVTRANSFORM_BASE, type, idx);

		// Blend mode
		mat.addProperty(sampler.mOp.ordinal() ,
				Material._AI_MATKEY_TEXBLEND_BASE, type, idx);

		// Blend factor
		mat.addProperty(sampler.mWeighting ,
				Material._AI_MATKEY_TEXBLEND_BASE, type, idx);

		// UV source index ... if we didn't resolve the mapping, it is actually just 
		// a guess but it works in most cases. We search for the frst occurence of a
		// number in the channel name. We assume it is the zero-based index into the
		// UV channel array of all corresponding meshes. It could also be one-based
		// for some exporters, but we won't care of it unless someone complains about.
		if (sampler.mUVId != -1)
			map = sampler.mUVId;
		else {
			map = -1;
//			for (std::string::const_iterator it = sampler.mUVChannel.begin();it != sampler.mUVChannel.end(); ++it){
//				if (IsNumeric(*it)) {
//					map = strtoul10(&(*it));
//					break;
//				}
//			}
			for(int l = 0; l < sampler.mUVChannel.length(); l++){
				char c = sampler.mUVChannel.charAt(l);
				if(Character.isDigit(c)){
					map = c - '0';
					break;
				}
			}
			
			if (-1 == map) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("Collada: unable to determine UV channel for texture");
				map = 0;
			}
		}
		mat.addProperty(map, Material._AI_MATKEY_UVWSRC_BASE,type,idx);
	}

	/** Resolves the texture name for the given effect texture entry */
	String findFilenameForEffectTexture(ColladaParser pParser, Effect pEffect, String pName){
		// recurse through the param references until we end up at an image
		String name = pName;
		while( true)
		{
			// the given string is a param entry. Find it
//			Collada::Effect::ParamLibrary::const_iterator it = pEffect.mParams.find( name);
			EffectParam it = pEffect.mParams.get(name);
			// if not found, we're at the end of the recursion. The resulting string should be the image ID
			if( it == null/*pEffect.mParams.end()*/)
				break;

			// else recurse on
			name = it.mReference;
		}

		// find the image referred by this name in the image library of the scene
//		ColladaParser::ImageLibrary::const_iterator imIt = pParser.mImageLibrary.find( name);
		COLImage imIt = pParser.mImageLibrary.get(name);
		if( imIt == null/*pParser.mImageLibrary.end()*/) 
		{
			throw new DeadlyImportError( String.format("Collada: Unable to resolve effect texture entry \"%s\", ended up at ID \"%s\".", pName , name));
		}

		String result;

		// if this is an embedded texture image setup an aiTexture for it
		if (AssUtil.isEmpty(imIt.mFileName)) 
		{
//			if (imIt->second.mImageData.empty())  {
			if(imIt.mImageData == null){
				throw new DeadlyImportError("Collada: Invalid texture, no data or file reference given");
			}

			Texture tex = new Texture();

			// setup format hint
			if (DefaultLogger.LOG_OUT && imIt.mEmbeddedFormat.length() > 3) {
				DefaultLogger.warn("Collada: texture format hint is too long, truncating to 3 characters");
			}
//			strncpy(tex.achFormatHint,imIt->second.mEmbeddedFormat.c_str(),3);
			tex.achFormatHint = imIt.mEmbeddedFormat;

			// and copy texture data
			tex.mHeight = 0;
			tex.mWidth = imIt.mImageData.remaining();
//			tex.pcData = (aiTexel*)new char[tex.mWidth];
//			memcpy(tex.pcData,&imIt->second.mImageData[0],tex.mWidth);
			tex.pcData = imIt.mImageData.asIntBuffer();

			// setup texture reference string
//			result.data[0] = '*';
//			result.length = 1 + ASSIMP_itoa10(result.data+1,MAXLEN-1,mTextures.size());
			result = "*" + Integer.toString(mTextures.size());

			// and add this texture to the list
			mTextures.add(tex);
		}
		else 
		{
//			result.Set( imIt->second.mFileName );
//			convertPath(result);
			result = convertPath(imIt.mFileName);
		}
		return result;
	}

	/** Converts a path read from a collada file to the usual representation */
	String convertPath(String ss){
		if(ss.startsWith("file:///")){
			ss = ss.substring(8);
		}else if(ss.startsWith("file://")){
			ss = ss.substring(7);
		}
		
		// find and convert all %xy special chars
		StringBuilder sb = new StringBuilder();
		int pre = 0;
		while(true){
			int cur = ss.indexOf('%', pre);
			if(cur >= 0){
				sb.append(ss.substring(pre, cur));
				
				if(cur + 3 < ss.length()){
					sb.append((char)(Integer.parseInt(ss.substring(cur + 1, cur + 3), 16) & 0xFF));
				}else{
					sb.append(ss.charAt(cur));
				}
				
				pre = cur + 1;
			}else{
//				sb.append(ss.substring(pre, ss.length()));
				break;
			}
		}
		
		if(pre == 0)
			return ss;
		else
			return sb.toString();
	}

	/** Reads a float value from an accessor and its data array.
	 * @param pAccessor The accessor to use for reading
	 * @param pData The data array to read from
	 * @param pIndex The index of the element to retrieve
	 * @param pOffset Offset into the element, for multipart elements such as vectors or matrices
	 * @return the specified value
	 */
	float readFloat( Accessor pAccessor, Data pData, int pIndex, int pOffset){
		// FIXME: (thom) Test for data type here in every access? For the moment, I leave this to the caller
		int pos = pAccessor.mStride * pIndex + pAccessor.mOffset + pOffset;
//		ai_assert( pos < pData.mValues.size());
		return pData.mValues.getFloat(pos);
	}

	/** Reads a string value from an accessor and its data array.
	 * @param pAccessor The accessor to use for reading
	 * @param pData The data array to read from
	 * @param pIndex The index of the element to retrieve
	 * @return the specified value
	 */
	String readString( Accessor pAccessor, Data pData, int pIndex){
		int pos = pAccessor.mStride * pIndex + pAccessor.mOffset;
//		ai_assert( pos < pData.mStrings.size());
		return pData.mStrings.get(pos);
	}

	/** Recursively collects all nodes into the given array */
	void collectNodes( Node pNode, List<Node> poNodes){
		poNodes.add( pNode);

		for( int a = 0; a < pNode.getNumChildren(); ++a)
			collectNodes( pNode.mChildren[a], poNodes);
	}

	/** Finds a node in the collada scene by the given name */
	COLNode findNode( COLNode pNode, String pName){
		if( pNode.mName.equals(pName) || pNode.mID.equals(pName))
			return pNode;

		for( int a = 0; a < pNode.mChildren.size(); ++a)
		{
			COLNode node = findNode( pNode.mChildren.get(a), pName);
			if( node != null)
				return node;
		}

		return null;
	}
	/** Finds a node in the collada scene by the given SID */
	COLNode findNodeBySID( COLNode pNode, String pSID){
		if( pNode.mSID.equals(pSID))
		    return pNode;

		  for( int a = 0; a < pNode.mChildren.size(); ++a)
		  {
		    COLNode node = findNodeBySID( pNode.mChildren.get(a), pSID);
		    if( node != null)
		      return node;
		  }

		  return null;
	}

	/** Finds a proper name for a node derived from the collada-node's properties */
	String findNameForNode( COLNode pNode){
		// now setup the name of the node. We take the name if not empty, otherwise the collada ID
		// FIX: Workaround for XSI calling the instanced visual scene 'untitled' by default.
		if (!pNode.mName.isEmpty() && pNode.mName.compareTo("untitled") !=0)
			return pNode.mName;
		else if (!pNode.mID.isEmpty())
			return pNode.mID;
		else if (!pNode.mSID.isEmpty())
	    return pNode.mSID;
	  else
		{
			// No need to worry. Unnamed nodes are no problem at all, except
			// if cameras or lights need to be assigned to them.
//	    return boost::str( boost::format( "$ColladaAutoName$_%d") % clock());
		  return String.format("$ColladaAutoName$_%d", System.currentTimeMillis());
		}
	}

	
}
