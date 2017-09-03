package assimp.importer.xfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Bone;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.ImporterDesc;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.QuatKey;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.TextureType;
import assimp.common.VectorKey;
import assimp.common.VertexWeight;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/** The XFileImporter is a worker class capable of importing a scene from a
 * DirectX file .x
 */
public class XFileImporter extends BaseImporter{
	
	private static final ImporterDesc desc = new ImporterDesc(
			"Direct3D XFile Importer",
			"",
			"",
			"",
			ImporterDesc.aiImporterFlags_SupportTextFlavour | ImporterDesc.aiImporterFlags_SupportBinaryFlavour | ImporterDesc.aiImporterFlags_SupportCompressedFlavour,
			1,
			3,
			1,
			5,
			"x" 
	);

	/** Buffer to hold the loaded file */
	ByteBuffer mBuffer;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler, boolean checkSig) throws IOException {
		String extension = getExtension(pFile);
		if(extension.equals("x")) {
			return true;
		}
		if (extension.length() == 0 || checkSig) {
//			uint32_t token[1];
//			token[0] = AI_MAKE_MAGIC("xof ");
			byte[] token = "xof ".getBytes();
			return checkMagicToken(new File(pFile),token, 0, 4/*,1,0*/);
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() { return desc;}

	/** Imports the given file into the given scene structure. 
	 * See BaseImporter.InternReadFile() for details
	 */
	@Override
	protected void internReadFile(File pFile, Scene pScene) {
//		// read file into memory
//		boost::scoped_ptr<IOStream> file( pIOHandler->Open( pFile));
//		if( file.get() == NULL)
//			throw DeadlyImportError( "Failed to open file " + pFile + ".");
//
//		size_t fileSize = file->FileSize();
//		if( fileSize < 16)
//			throw DeadlyImportError( "XFile is too small.");
//
//		// in the hope that binary files will never start with a BOM ...
//		mBuffer.resize( fileSize + 1);
//		file->Read( &mBuffer.front(), 1, fileSize);
//		ConvertToUTF8(mBuffer);
//
		// parse the file into a temporary representation
		XFileParser parser = null;
		try {
			parser = new XFileParser( mBuffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		mBuffer = FileUtils.loadText(pFile, false, AssimpConfig.LOADING_USE_NATIVE_MEMORY);   // TODO Don't forget the UTF-8

		// and create the proper return structures out of it
		createDataRepresentationFromImport( pScene, parser.getImportedData());

		// if nothing came from it, report it as error
		if( pScene.mRootNode == null)
			throw new DeadlyImportError( "XFile is ill-formatted - no content imported.");
	}

	// -------------------------------------------------------------------
	/** Constructs the return data structure out of the imported data.
	 * @param pScene The scene to construct the return data in.
	 * @param pData The imported data in the internal temporary 
	 *   representation.
	 */
	void createDataRepresentationFromImport(Scene pScene, XScene pData){
		// Read the global materials first so that meshes referring to them can find them later
		convertMaterials( pScene, pData.mGlobalMaterials);

		// copy nodes, extracting meshes and materials on the way
		pScene.mRootNode = createNodes( pScene, null, pData.mRootNode);

		// extract animations
		createAnimations( pScene, pData);

		// read the global meshes that were stored outside of any node
		if( pData.mGlobalMeshes.size() > 0)
		{
			// create a root node to hold them if there isn't any, yet
			if( pScene.mRootNode == null)
			{
				pScene.mRootNode = new Node();
				pScene.mRootNode.mName = "$dummy_node";
			}

			// convert all global meshes and store them in the root node.
			// If there was one before, the global meshes now suddenly have its transformation matrix...
			// Don't know what to do there, I don't want to insert another node under the present root node
			// just to avoid this.
			createMeshes( pScene, pScene.mRootNode, pData.mGlobalMeshes);
		}

		// Convert everything to OpenGL space... it's the same operation as the conversion back, so we can reuse the step directly
		MakeLeftHandedProcess convertProcess;
		convertProcess.Execute( pScene);

		FlipWindingOrderProcess flipper;
		flipper.Execute(pScene);

		// finally: create a dummy material if not material was imported
//		if( pScene.mNumMaterials == 0)
		if( pScene.mMaterials == null)
		{
//			pScene.mNumMaterials = 1;
			// create the Material
			Material mat = new Material();
			ShadingMode shadeMode = ShadingMode.aiShadingMode_Gouraud;
			mat.addProperty(shadeMode.ordinal(), Material.AI_MATKEY_SHADING_MODEL,0,0);
			// material colours
			int specExp = 1;

			Vector3f clr = new Vector3f( 0, 0, 0);
			mat.addProperty( clr, Material.AI_MATKEY_COLOR_EMISSIVE, 0,0);
			mat.addProperty( clr, Material.AI_MATKEY_COLOR_SPECULAR, 0,0);

			clr.set( 0.5f, 0.5f, 0.5f);
			mat.addProperty( clr, Material.AI_MATKEY_COLOR_DIFFUSE, 0,0);
			mat.addProperty( specExp, Material.AI_MATKEY_SHININESS, 0,0);

			pScene.mMaterials = new Material[]{mat};
//			pScene->mMaterials[0] = mat;
		}
	}

	// -------------------------------------------------------------------
	/** Recursively creates scene nodes from the imported hierarchy.
	 * The meshes and materials of the nodes will be extracted on the way.
	 * @param pScene The scene to construct the return data in.
	 * @param pParent The parent node where to create new child nodes
	 * @param pNode The temporary node to copy.
	 * @return The created node 
	 */
	Node createNodes( Scene pScene, Node pParent, XNode pNode){
		if( pNode == null)
			return null;

		// create node
		Node node = new Node();
//		node.mName.length = pNode.mName.length();
		node.mParent = pParent;
//		memcpy( node->mName.data, pNode->mName.c_str(), pNode->mName.length());
//		node->mName.data[node->mName.length] = 0;
		node.mName = pNode.mName;
		node.mTransformation.load(pNode.mTrafoMatrix);

		// convert meshes from the source node 
		createMeshes( pScene, node, pNode.mMeshes);

		// handle childs
		if( pNode.mChildren.size() > 0)
		{
//			node.mNumChildren = (int)pNode->mChildren.size();
			node.mChildren = new Node[/*node->mNumChildren*/ pNode.mChildren.size()];

			for(int a = 0; a < pNode.mChildren.size(); a++)
				node.mChildren[a] = createNodes( pScene, node, pNode.mChildren.get(a));
		}

		return node;
	}

	// -------------------------------------------------------------------
	/** Converts all meshes in the given mesh array. Each mesh is split 
	 * up per material, the indices of the generated meshes are stored in 
	 * the node structure.
	 * @param pScene The scene to construct the return data in.
	 * @param pNode The target node structure that references the
	 *   constructed meshes.
	 * @param pMeshes The array of meshes to convert
	 */
	void createMeshes(Scene pScene, Node pNode, List<XMesh> pMeshes){
		if( pMeshes.size() == 0)
			return;

		// create a mesh for each mesh-material combination in the source node
//		std::vector<aiMesh*> meshes;
		List<Mesh> meshes = new ArrayList<>();
		for(int a = 0; a < pMeshes.size(); a++)
		{
			XMesh sourceMesh = pMeshes.get(a);
			// first convert its materials so that we can find them with their index afterwards
			convertMaterials( pScene, sourceMesh.mMaterials);

			int numMaterials = Math.max(sourceMesh.mMaterials.size(), 1);
			for( int b = 0; b < numMaterials; b++)
			{
				// collect the faces belonging to this material
//				std::vector<int> faces;
				IntArrayList faces = new IntArrayList();
				int numVertices = 0;
				if( sourceMesh.mFaceMaterials.size() > 0)
				{
					// if there is a per-face material defined, select the faces with the corresponding material
					for( int c = 0; c < sourceMesh.mFaceMaterials.size(); c++)
					{
						if( sourceMesh.mFaceMaterials.getInt(c) == b)
						{
							faces.add( c);  // TODO I can't understand
							numVertices += sourceMesh.mPosFaces.get(c).mIndices.length;
						}
					}
				} else
				{
					// if there is no per-face material, place everything into one mesh
					for( int c = 0; c < sourceMesh.mPosFaces.size(); c++)
					{
						faces.add( c);  // TODO I can't understand
						numVertices += sourceMesh.mPosFaces.get(c).mIndices.length;
					}
				}

				// no faces/vertices using this material? strange...
				if( numVertices == 0)
					continue;

				// create a submesh using this material
				Mesh mesh = new Mesh();
				meshes.add( mesh);

				// find the material in the scene's material list. Either own material
				// or referenced material, it should already have a valid index
				if( sourceMesh.mFaceMaterials.size() > 0)
				{
					mesh.mMaterialIndex = sourceMesh.mMaterials.get(b).sceneIndex;
				} else
				{
					mesh.mMaterialIndex = 0;
				}

				// Create properly sized data arrays in the mesh. We store unique vertices per face,
				// as specified 
//				mesh->mNumVertices = numVertices;
//				mesh->mVertices = new aiVector3D[numVertices];
//				mesh->mNumFaces = (int)faces.size();
//				mesh->mFaces = new aiFace[mesh->mNumFaces];
				
				final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
				mesh.mVertices = MemoryUtil.createFloatBuffer(numVertices * 3, natived);
				mesh.mFaces = new Face[faces.size()];

				// normals?
				if( sourceMesh.mNormals != null){
//					mesh->mNormals = new aiVector3D[numVertices];
					mesh.mNormals = MemoryUtil.createFloatBuffer(numVertices * 3, natived);
				}
				// texture coords
				for( int c = 0; c < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; c++)
				{
					if( sourceMesh.mTexCoords[c] != null)
//						mesh->mTextureCoords[c] = new aiVector3D[numVertices];
						mesh.mTextureCoords[c] = MemoryUtil.createFloatBuffer(numVertices * 3, natived);
				}
				// vertex colors
				for( int c = 0; c < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; c++)
				{
					if( sourceMesh.mColors[c] != null)
//						mesh->mColors[c] = new aiColor4D[numVertices];
						mesh.mColors[c] = MemoryUtil.createFloatBuffer(numVertices * 3, natived);
				}

				// now collect the vertex data of all data streams present in the imported mesh
				int newIndex = 0;
//				std::vector<int> orgPoints; // from which original point each new vertex stems
//				orgPoints.resize( numVertices, 0);
				int[] orgPoints = new int[numVertices];

				for( int c = 0; c < faces.size(); c++)
				{
					int f = faces.getInt(c); // index of the source face
					final XFace pf = sourceMesh.mPosFaces.get(f); // position source face

					// create face. either triangle or triangle fan depending on the index count
					Face df = mesh.mFaces[c] = Face.createInstance(pf.mIndices.length); // destination face
//					df.mNumIndices = (int)pf.mIndices.size();
//					df.mIndices = new int[ df.mNumIndices];

					// collect vertex data for indices of this face
					for( int d = 0; d < df.getNumIndices(); d++)
					{
//						df.mIndices[d] = newIndex; 
						df.set(d, newIndex);

						// Position
//						mesh->mVertices[newIndex] = sourceMesh->mPositions[pf.mIndices[d]];
						int index = orgPoints[newIndex] = pf.mIndices[d];
						MemoryUtil.arraycopy(sourceMesh.mPositions, index * 3, mesh.mVertices, newIndex * 3, 3);
						// Normal, if present
						if( mesh.hasNormals()){
//							mesh->mNormals[newIndex] = sourceMesh->mNormals[sourceMesh->mNormFaces[f].mIndices[d]];
							index = sourceMesh.mNormFaces.get(f).mIndices[d];
							MemoryUtil.arraycopy(sourceMesh.mNormals, index * 3, mesh.mNormals, newIndex * 3, 3);
						}

						// texture coord sets
						for( int e = 0; e < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; e++)
						{
							if( mesh.hasTextureCoords( e))
							{
//								aiVector2D tex = sourceMesh->mTexCoords[e][pf.mIndices[d]];
//								mesh->mTextureCoords[e][newIndex] = aiVector3D( tex.x, 1.0f - tex.y, 0.0f);
								index = pf.mIndices[d];
								float texx = sourceMesh.mTexCoords[e].get(index * 2);
								float texy = sourceMesh.mTexCoords[e].get(index * 2 + 1);
								mesh.mTextureCoords[e].put(3 * index, texx);
								mesh.mTextureCoords[e].put(3 * index + 1, 1.0f - texy);
								mesh.mTextureCoords[e].put(3 * index + 2, 0);
							}
						}
						// vertex color sets
						for( int e = 0; e < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; e++)
							if( mesh.hasVertexColors( e)){
//								mesh.mColors[e][newIndex] = sourceMesh->mColors[e][pf.mIndices[d]];
								index = pf.mIndices[d];
								MemoryUtil.arraycopy(sourceMesh.mColors[e], index * 4, mesh.mColors[e], newIndex * 4, 4);
							}

						newIndex++;
					}
				}

				// there should be as much new vertices as we calculated before
				assert( newIndex == numVertices);

				// convert all bones of the source mesh which influence vertices in this newly created mesh
				final List<XBone> bones = sourceMesh.mBones;
				List<Bone> newBones = new ArrayList<>();
				for( int c = 0; c < bones.size(); c++)
				{
					final XBone obone = bones.get(c);
					// set up a vertex-linear array of the weights for quick searching if a bone influences a vertex
//					std::vector<float> oldWeights( sourceMesh->mPositions.size(), 0.0f);
					float[] oldWeights = new float[sourceMesh.mPosFaces.size()];
					for( int d = 0; d < obone.mWeights.size(); d++){
						XBoneWeight w = obone.mWeights.get(d);
						oldWeights[w.mVertex] = w.mWeight;
					}

					// collect all vertex weights that influence a vertex in the new mesh
//					std::vector<aiVertexWeight> newWeights;
//					newWeights.reserve( numVertices);
					List<VertexWeight> newWeights = new ArrayList<>(numVertices);
					for( int d = 0; d < orgPoints.length; d++)
					{
						// does the new vertex stem from an old vertex which was influenced by this bone?
						float w = oldWeights[orgPoints[d]];
						if( w > 0.0f)
							newWeights.add(new VertexWeight( d, w));
					}

					// if the bone has no weights in the newly created mesh, ignore it
					if( newWeights.size() == 0)
						continue;

					// create
					Bone nbone = new Bone();
					newBones.add( nbone);
					// copy name and matrix
					nbone.mName = obone.mName;
					nbone.mOffsetMatrix.load(obone.mOffsetMatrix);
//					nbone.mNumWeights = (int)newWeights.size();
//					nbone.mWeights = new VertexWeight[newWeights.size()/*nbone.mNumWeights*/];
//					for( int d = 0; d < newWeights.size(); d++)
//						nbone.mWeights[d] = newWeights[d];
					nbone.mWeights = AssUtil.toArray(newWeights, VertexWeight.class);
				}

				// store the bones in the mesh
//				mesh->mNumBones = (int)newBones.size();
//				if( newBones.size() > 0)
//				{
//					mesh->mBones = new aiBone*[mesh->mNumBones];
//					std::copy( newBones.begin(), newBones.end(), mesh->mBones);
//				}
				
				mesh.mBones = AssUtil.toArray(newBones, Bone.class);
			}
		}

		// reallocate scene mesh array to be large enough
		Mesh[] prevArray = pScene.mMeshes;
		pScene.mMeshes = new Mesh[pScene.getNumMeshes() + meshes.size()];
		if( prevArray != null)
		{
//			memcpy( pScene->mMeshes, prevArray, pScene->mNumMeshes * sizeof( aiMesh*));
//			delete [] prevArray;
			System.arraycopy(prevArray, 0, pScene.mMeshes, 0, prevArray.length);
		}

		// allocate mesh index array in the node
//		pNode.mNumMeshes = (int)meshes.size();
		pNode.mMeshes = new int[meshes.size()];

		// store all meshes in the mesh library of the scene and store their indices in the node
		int numMeshes = 0;
		for( int a = 0; a < meshes.size(); a++)
		{
			pScene.mMeshes[numMeshes] = meshes.get(a);		
			pNode.mMeshes[a] = numMeshes;
			numMeshes++;
		}
	}

	// -------------------------------------------------------------------
	/** Converts the animations from the given imported data and creates 
	*  them in the scene.
	 * @param pScene The scene to hold to converted animations
	 * @param pData The data to read the animations from
	 */
	void createAnimations( Scene pScene, XScene pData){
		List<Animation> newAnims = new ArrayList<>();

		Vector3f temp1 = new Vector3f();
		Vector3f temp2 = new Vector3f();
		Vector3f temp3 = new Vector3f();
		Matrix3f rot = new Matrix3f();
		for(int a = 0; a < pData.mAnims.size(); a++)
		{
			final XAnimation anim = pData.mAnims.get(a);
			// some exporters mock me with empty animation tags.
			if( anim.mAnims.size() == 0)
				continue;

			// create a new animation to hold the data
			Animation nanim = new Animation();
			newAnims.add( nanim);
			nanim.mName = ( anim.mName);
			// duration will be determined by the maximum length
			nanim.mDuration = 0;
			nanim.mTicksPerSecond = pData.mAnimTicksPerSecond;
//			nanim.mNumChannels = anim.mAnims.size();
			nanim.mChannels = new NodeAnim[anim.mAnims.size()];

			for(int b = 0; b < anim.mAnims.size(); b++)
			{
				XAnimBone bone = anim.mAnims.get(b);
				NodeAnim nbone = new NodeAnim();
				nbone.mNodeName = ( bone.mBoneName);
				nanim.mChannels[b] = nbone;

				// keyframes are given as combined transformation matrix keys
				if( bone.mTrafoKeys.size() > 0)
				{
//					nbone.mNumPositionKeys = bone.mTrafoKeys.size();
					nbone.mPositionKeys = new VectorKey[bone.mTrafoKeys.size()];
//					nbone.mNumRotationKeys = bone.mTrafoKeys.size();
					nbone.mRotationKeys = new QuatKey[bone.mTrafoKeys.size()];
//					nbone.mNumScalingKeys = bone.mTrafoKeys.size();
					nbone.mScalingKeys = new VectorKey[bone.mTrafoKeys.size()];

					for(int c = 0; c < bone.mTrafoKeys.size(); c++)
					{
						// deconstruct each matrix into separate position, rotation and scaling
						float time = bone.mTrafoKeys.get(c).mTime;
						Matrix4f trafo = bone.mTrafoKeys.get(c).mMatrix;

						// extract position
//						Vector3f pos = new Vector3f( trafo.a4, trafo.b4, trafo.c4);

						nbone.mPositionKeys[c].mTime = time;
						nbone.mPositionKeys[c].mValue.set( trafo.m30, trafo.m31, trafo.m32);

						// extract scaling
//						aiVector3D scale;
//						scale.x = aiVector3D( trafo.a1, trafo.b1, trafo.c1).Length();
//						scale.y = aiVector3D( trafo.a2, trafo.b2, trafo.c2).Length();
//						scale.z = aiVector3D( trafo.a3, trafo.b3, trafo.c3).Length();
						
						temp1.set(trafo.m00, trafo.m01, trafo.m02);
						temp2.set(trafo.m10, trafo.m11, trafo.m12);  // TODO
						temp3.set(trafo.m20, trafo.m21, trafo.m22);
						float scalex = temp1.length();
						float scaley = temp2.length();
						float scalez = temp3.length();
						nbone.mScalingKeys[c].mTime = time;
						nbone.mScalingKeys[c].mValue.set(scalex, scaley, scalez);

						// reconstruct rotation matrix without scaling
//						aiMatrix3x3 rotmat( 
//							trafo.a1 / scale.x, trafo.a2 / scale.y, trafo.a3 / scale.z,
//							trafo.b1 / scale.x, trafo.b2 / scale.y, trafo.b3 / scale.z,
//							trafo.c1 / scale.x, trafo.c2 / scale.y, trafo.c3 / scale.z);
						
						rot.m00 = trafo.m00 /scalex; rot.m10 = trafo.m10 /scaley; rot.m20 = trafo.m20 /scalez;
						rot.m01 = trafo.m01 /scalex; rot.m11 = trafo.m11 /scaley; rot.m21 = trafo.m21 /scalez;
						rot.m02 = trafo.m02 /scalex; rot.m12 = trafo.m12 /scaley; rot.m22 = trafo.m22 /scalez;
						
						// and convert it into a quaternion
						nbone.mRotationKeys[c].mTime = time;
						nbone.mRotationKeys[c].mValue.setFromMatrix(rot);  // TODO Does the rot matrix need transpose?
					}

					// longest lasting key sequence determines duration
					nanim.mDuration = Math.max( nanim.mDuration, AssUtil.back(bone.mTrafoKeys).mTime);
				} else
				{
					// separate key sequences for position, rotation, scaling
//					nbone.mNumPositionKeys = bone.mPosKeys.size(); 
					nbone.mPositionKeys = new VectorKey[bone.mPosKeys.size()];
					for(int c = 0; c < nbone.mPositionKeys.length; c++)
					{
						Vector3f pos = bone.mPosKeys.get(c).mValue;

						nbone.mPositionKeys[c] = new VectorKey();
						nbone.mPositionKeys[c].mTime = bone.mPosKeys.get(c).mTime;
						nbone.mPositionKeys[c].mValue.set(pos);
					}

					// rotation
//					nbone.mNumRotationKeys = bone.mRotKeys.size(); 
					nbone.mRotationKeys = new QuatKey[bone.mRotKeys.size()];
					for( int c = 0; c < nbone.mRotationKeys.length; c++)
					{
//						aiMatrix3x3 rotmat = bone->mRotKeys[c].mValue.GetMatrix();
//						bone.mRotKeys.get(c).mValue.toMatrix(rot);
						nbone.mRotationKeys[c] = new QuatKey();
						nbone.mRotationKeys[c].mTime = bone.mRotKeys.get(c).mTime;
//						nbone.mRotationKeys[c].mValue = aiQuaternion( rotmat);
						nbone.mRotationKeys[c].mValue.set(bone.mRotKeys.get(c).mValue);
						nbone.mRotationKeys[c].mValue.w *= -1.0f; // needs quat inversion
					}

					// scaling
//					nbone.mNumScalingKeys = bone.mScaleKeys.size(); 
					nbone.mScalingKeys = new VectorKey[bone.mScaleKeys.size()];
					for(int c = 0; c < nbone.mScalingKeys.length; c++)
						nbone.mScalingKeys[c] = bone.mScaleKeys.get(c);  // TODO reference copy

					// longest lasting key sequence determines duration
					if( bone.mPosKeys.size() > 0)
						nanim.mDuration = Math.max( nanim.mDuration, AssUtil.back(bone.mPosKeys).mTime);
					if( bone.mRotKeys.size() > 0)
						nanim.mDuration = Math.max( nanim.mDuration, AssUtil.back(bone.mRotKeys).mTime);
					if( bone.mScaleKeys.size() > 0)
						nanim.mDuration = Math.max( nanim.mDuration, AssUtil.back(bone.mScaleKeys).mTime);
				}
			}
		}

		// store all converted animations in the scene
		if( newAnims.size() > 0)
		{
//			pScene->mNumAnimations = newAnims.size();
			pScene.mAnimations = new Animation [newAnims.size()];
			for(int a = 0; a < newAnims.size(); a++)
				pScene.mAnimations[a] = newAnims.get(a);
		}
	}

	// -------------------------------------------------------------------
	/** Converts all materials in the given array and stores them in the
	 *  scene's material list.
	 * @param pScene The scene to hold the converted materials.
	 * @param pMaterials The material array to convert.
	 */
	void convertMaterials( Scene pScene, List<XMaterial> pMaterials){
		// count the non-referrer materials in the array
		int numNewMaterials = 0;
		for( int a = 0; a < pMaterials.size(); a++)
			if( !pMaterials.get(a).mIsReference)
				numNewMaterials++;

		int numMat = pScene.getNumMaterials();
		// resize the scene's material list to offer enough space for the new materials
		if( numNewMaterials > 0 )
		{
			Material[] prevMats = pScene.mMaterials;
			pScene.mMaterials = new Material[numMat + numNewMaterials];
			if( prevMats != null)
			{
	//  	    memcpy( pScene->mMaterials, prevMats, pScene->mNumMaterials * sizeof( aiMaterial*));
	//			delete [] prevMats;
				System.arraycopy(prevMats, 0, pScene.mMaterials, 0, prevMats.length);
			}
		 }

		// convert all the materials given in the array
		for( int k = 0; k < pMaterials.size(); k++)
		{
			XMaterial oldMat = pMaterials.get(k);
			if( oldMat.mIsReference)
		    {
		      // find the material it refers to by name, and store its index
		      for( int a = 0; a < pScene.getNumMaterials(); ++a )
		      {
		    	  
	//	        aiString name;
	//	        pScene->mMaterials[a]->Get( AI_MATKEY_NAME, name);
		    	  String name = pScene.mMaterials[a].getString(Material.AI_MATKEY_NAME, 0, 0);
	//	        if( strcmp( name.C_Str(), oldMat.mName.data()) == 0 )
		    	  if(name.equals(oldMat.mName))
		    	  {
		    		  oldMat.sceneIndex = a;
		    		  break;
		    	  }
		      }
	
		      if( oldMat.sceneIndex == -1 )
		      {
		        DefaultLogger.warn(String.format( "Could not resolve global material reference \"%s\"", oldMat.mName));
		        oldMat.sceneIndex = 0;
		      }
	
		      continue;
		    }

			Material mat = new Material();
//			aiString name;
//			name.Set( oldMat.mName);
//			mat->AddProperty( &name, AI_MATKEY_NAME);
			mat.addProperty(oldMat.mName, Material.AI_MATKEY_NAME, 0, 0);

			// Shading model: hardcoded to PHONG, there is no such information in an XFile
			// FIX (aramis): If the specular exponent is 0, use gouraud shading. This is a bugfix
			// for some models in the SDK (e.g. good old tiny.x)
			ShadingMode shadeMode = oldMat.mSpecularExponent == 0.0f 
				? ShadingMode.aiShadingMode_Gouraud : ShadingMode.aiShadingMode_Phong;

			mat.addProperty(shadeMode.ordinal(), Material.AI_MATKEY_SHADING_MODEL, 0,0);
			// material colours
	    // Unclear: there's no ambient colour, but emissive. What to put for ambient?
	    // Probably nothing at all, let the user select a suitable default.
			mat.addProperty( oldMat.mEmissive, Material.AI_MATKEY_COLOR_EMISSIVE, 0,0);
			mat.addProperty( oldMat.mDiffuse, Material.AI_MATKEY_COLOR_DIFFUSE, 0,0);
			mat.addProperty( oldMat.mSpecular, Material.AI_MATKEY_COLOR_SPECULAR, 0,0);
			mat.addProperty( oldMat.mSpecularExponent, Material.AI_MATKEY_SHININESS, 0,0);


			// texture, if there is one
			if (1 == oldMat.mTextures.size())
			{
				XTexEntry otex = AssUtil.back(oldMat.mTextures);
				if (otex.mName.length() > 0)
				{
					// if there is only one texture assume it contains the diffuse color
//					aiString tex( otex.mName);
					if( otex.mIsNormalMap)
						mat.addProperty(otex.mName, /*AI_MATKEY_TEXTURE_NORMALS(0)*/ Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_NORMALS.ordinal(), 0);
					else
						mat.addProperty(otex.mName, /*AI_MATKEY_TEXTURE_DIFFUSE(0)*/ Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE.ordinal(), 0);
				}
			}
			else
			{
				// Otherwise ... try to search for typical strings in the
				// texture's file name like 'bump' or 'diffuse'
				int iHM = 0,iNM = 0,iDM = 0,iSM = 0,iAM = 0,iEM = 0;
				for( int b = 0; b < oldMat.mTextures.size(); b++)
				{
					XTexEntry otex = oldMat.mTextures.get(b);
					String sz = otex.mName;
//					if (!sz.length())continue;
					if(sz.isEmpty()) continue;


					// find the file name
					//const size_t iLen = sz.length();
//					std::string::size_type s = sz.find_last_of("\\/");
//					if (std::string::npos == s)
//						s = 0;
					int s = sz.lastIndexOf("\\/");
					if(s == -1)
						s = 0;

					// cut off the file extension
//					std::string::size_type sExt = sz.find_last_of('.');
//					if (std::string::npos != sExt){
//						sz[sExt] = '\0';
//					}
					
					int sExt = sz.lastIndexOf('.');
					if(sExt != -1)
						sz = sz.substring(0, sExt).toLowerCase();

					// convert to lower case for easier comparision
//					for( int c = 0; c < sz.length(); c++){
//						if( isalpha( sz[c]))
//							sz[c] = tolower( sz[c]);
//					}


					// Place texture filename property under the corresponding name
//					aiString tex( oldMat.mTextures[b].mName);
					String tex = oldMat.mTextures.get(b).mName;

					// bump map
					if (-1 != sz.indexOf("bump", s) || -1 != sz.indexOf("height", s))
					{
						mat.addProperty(tex, /*AI_MATKEY_TEXTURE_HEIGHT(iHM++)*/ Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_HEIGHT, iHM++);
					} else
					if (otex.mIsNormalMap || -1 != sz.indexOf( "normal", s) || -1 != sz.indexOf("nm", s))
					{
						mat.addProperty(tex, /*AI_MATKEY_TEXTURE_NORMALS(iNM++)*/Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_NORMALS, iNM++);
					} else
					if (-1 != sz.indexOf( "spec", s) || -1 != sz.indexOf( "glanz", s))
					{
						mat.addProperty(tex, /*AI_MATKEY_TEXTURE_SPECULAR(iSM++)*/Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_SPECULAR, iSM++);
					} else
					if (-1 != sz.indexOf( "ambi", s) || -1 != sz.indexOf( "env", s))
					{
						mat.addProperty(tex, /*AI_MATKEY_TEXTURE_AMBIENT(iAM++)*/Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_AMBIENT, iAM++);
					} else
					if (-1 != sz.indexOf( "emissive", s) || -1 != sz.indexOf( "self", s))
					{
						mat.addProperty(tex, /*AI_MATKEY_TEXTURE_EMISSIVE(iEM++)*/ Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_EMISSIVE, iEM++);
					} else
					{
						// Assume it is a diffuse texture
						mat.addProperty(tex, /*AI_MATKEY_TEXTURE_DIFFUSE(iDM++)*/Material._AI_MATKEY_TEXTURE_BASE, TextureType.aiTextureType_DIFFUSE, iDM++);
					}
				}
			}

			pScene.mMaterials[numMat] = mat;
			oldMat.sceneIndex = numMat;
			numMat++;
		}
		
		if(numMat != pScene.mMaterials.length){
			pScene.mMaterials = Arrays.copyOf(pScene.mMaterials, numMat);
		}
	}
}
