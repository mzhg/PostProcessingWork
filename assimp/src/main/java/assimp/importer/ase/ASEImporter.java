package assimp.importer.ase;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.BaseImporter;
import assimp.common.Bone;
import assimp.common.Camera;
import assimp.common.DeadlyImportError;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.FileUtils;
import assimp.common.Importer;
import assimp.common.ImporterDesc;
import assimp.common.IntFloatPair;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.QuatKey;
import assimp.common.SGSpatialSort;
import assimp.common.Scene;
import assimp.common.ShadingMode;
import assimp.common.SkeletonMeshBuilder;
import assimp.common.TextureType;
import assimp.common.VectorKey;
import assimp.common.VertexWeight;
import assimp.importer.d3ds.D3DSFace;
import assimp.importer.d3ds.D3DSHelper;
import assimp.importer.d3ds.D3DSMaterial;
import assimp.importer.d3ds.D3DSTexture;

/** 
 * Importer class for the 3DS ASE ASCII format.
*/
public class ASEImporter extends BaseImporter{

	private static final ImporterDesc desc = new ImporterDesc(
		"ASE Importer",
		"",
		"",
		"Similar to 3DS but text-encoded",
		ImporterDesc.aiImporterFlags_SupportTextFlavour,
		0,
		0,
		0,
		0,
		"ase ask" 
	);
	/** Parser instance */
	ASEParser mParser;

	/** Buffer to hold the loaded file */
	ByteBuffer mBuffer;

	/** Scene to be filled */
	Scene pcScene;

	/** Config options: Recompute the normals in every case - WA
	    for 3DS Max broken ASE normal export */
	boolean configRecomputeNormals;
	boolean noSkeletonMesh;
	
	@Override
	protected boolean canRead(String pFile, InputStream pIOHandler,boolean checkSig) {
		// check file extension 
		String extension = getExtension(pFile);
		
		if( extension.equals("ase") || extension.equals("ask"))
			return true;

		if ((extension.length()  == 0|| checkSig) && pIOHandler != null) {
			String tokens[] = {"*3dsmax_asciiexport"};
			try {
				return searchFileHeaderForToken(pIOHandler,pFile,tokens,1, 200, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected ImporterDesc getInfo() {
		return desc;
	}
	
	@Override
	public void setupProperties(Importer pImp) {
		configRecomputeNormals = (pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_ASE_RECONSTRUCT_NORMALS,1) != 0 ? true : false);
		noSkeletonMesh = pImp.getPropertyInteger(AssimpConfig.AI_CONFIG_IMPORT_NO_SKELETON_MESHES,0) != 0;
	}

	@Override
	protected void internReadFile(File pFile, Scene pScene) {
		mBuffer = FileUtils.loadText(pFile, true, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		pcScene = pScene;
		
		// ------------------------------------------------------------------
		// Guess the file format by looking at the extension
		// ASC is considered to be the older format 110,
		// ASE is the actual version 200 (that is currently written by max)
		// ------------------------------------------------------------------
		int defaultFormat;
		String filename = pFile.getName();
		int s = filename.length()-1;
		switch (filename.charAt(s))	{

		case 'C':
		case 'c':
			defaultFormat = ASEParser.AI_ASE_OLD_FILE_FORMAT;
			break;
		default:
			defaultFormat = ASEParser.AI_ASE_NEW_FILE_FORMAT;
		};

		// Construct an ASE parser and parse the file
		ASEParser parser = new ASEParser(mBuffer,defaultFormat);
		mParser = parser;
		mParser.parse();

		//------------------------------------------------------------------
		// Check whether we god at least one mesh. If we did - generate
		// materials and copy meshes. 
		// ------------------------------------------------------------------
		if ( !mParser.m_vMeshes.isEmpty())	{

			// If absolutely no material has been loaded from the file
			// we need to generate a default material
			generateDefaultMaterial();

			// process all meshes
			boolean tookNormals = false;
//			std::vector<aiMesh*> avOutMeshes;
//			avOutMeshes.reserve(mParser->m_vMeshes.size()*2);
			List<Mesh> avOutMeshes = new ArrayList<Mesh>(mParser.m_vMeshes.size()*2);
			for (ASEMesh i :  mParser.m_vMeshes)	{
				if (i.bSkip) {
					continue;
				}
				buildUniqueRepresentation(i);

				// Need to generate proper vertex normals if necessary
				if(generateNormals(i)) {
					tookNormals = true;
				}

				// Convert all meshes to aiMesh objects
				convertMeshes(i,avOutMeshes);
			}
			if (tookNormals && DefaultLogger.LOG_OUT)	{
				DefaultLogger.debug("ASE: Taking normals from the file. Use "+
					"the AI_CONFIG_IMPORT_ASE_RECONSTRUCT_NORMALS setting if you "+
					"experience problems");
			}

			// Now build the output mesh list. Remove dummies
//			pScene->mNumMeshes = avOutMeshes.size();
			/*Mesh** pp = */pScene.mMeshes = new Mesh[avOutMeshes.size()];
			int pp = 0;
			for (Mesh i :  avOutMeshes) {
				if (i.getNumFaces() == 0) {
					continue;
				}
//				*pp++ = *i;
				pScene.mMeshes[pp++] = i;
			}
//			pScene->mNumMeshes = (pp - pScene->mMeshes);
			if(pp < avOutMeshes.size())
				pScene.mMeshes = Arrays.copyOf(pScene.mMeshes, pp);

			// Build final material indices (remove submaterials and setup
			// the final list)
			buildMaterialIndices();
		}

		// ------------------------------------------------------------------
		// Copy all scene graph nodes - lights, cameras, dummies and meshes
		// into one huge list.
		//------------------------------------------------------------------
//		std::<BaseNode*> nodes;
//		nodes.reserve(mParser->m_vMeshes.size() +mParser->m_vLights.size()
//			+ mParser->m_vCameras.size() + mParser->m_vDummies.size());
		List<BaseNode> nodes = new ArrayList<BaseNode>(mParser.m_vMeshes.size() +mParser.m_vLights.size()
				+ mParser.m_vCameras.size() + mParser.m_vDummies.size());
		// Lights
//		for (std::vector<ASE::Light>::iterator it = mParser->m_vLights.begin(), 
//			 end = mParser->m_vLights.end();it != end; ++it)nodes.push_back(&(*it));
		nodes.addAll(mParser.m_vLights);
		// Cameras
//		for (std::vector<ASE::Camera>::iterator it = mParser->m_vCameras.begin(), 
//			 end = mParser->m_vCameras.end();it != end; ++it)nodes.push_back(&(*it));
		nodes.addAll(mParser.m_vCameras);
		// Meshes
//		for (std::vector<ASE::Mesh>::iterator it = mParser->m_vMeshes.begin(),
//			end = mParser->m_vMeshes.end();it != end; ++it)nodes.push_back(&(*it));
		nodes.addAll(mParser.m_vMeshes);
		// Dummies
//		for (std::vector<ASE::Dummy>::iterator it = mParser->m_vDummies.begin(),
//			end = mParser->m_vDummies.end();it != end; ++it)nodes.push_back(&(*it));
		nodes.addAll(mParser.m_vDummies);

		// build the final node graph
		buildNodes(nodes);

		// build output animations
		buildAnimations(nodes);

		// build output cameras
		buildCameras();

		// build output lights
		buildLights();

		// ------------------------------------------------------------------
		// If we have no meshes use the SkeletonMeshBuilder helper class
		// to build a mesh for the animation skeleton
		// FIXME: very strange results
		// ------------------------------------------------------------------
		if (pScene.getNumMeshes() == 0)	{
			pScene.mFlags |= Scene.AI_SCENE_FLAGS_INCOMPLETE;
			if (!noSkeletonMesh) {
				new SkeletonMeshBuilder (pScene, null, false);
			}
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	void generateDefaultMaterial()
	{
//		ai_assert(NULL != mParser);

		boolean bHas = false;
//		for (std::vector<ASE::Mesh>::iterator i =  mParser->m_vMeshes.begin();i != mParser->m_vMeshes.end();++i) {
		for(ASEMesh i : mParser.m_vMeshes){
			if (i.bSkip)continue;
			if (ASEFace.DEFAULT_MATINDEX == i.iMaterialIndex)	{
				i.iMaterialIndex = mParser.m_vMaterials.size();
				bHas = true;
			}
		}
		if (bHas || mParser.m_vMaterials.isEmpty())	{
			// add a simple material without submaterials to the parser's list
			ASEMaterial mat;
			mParser.m_vMaterials.add ( mat=new ASEMaterial() );
//			ASEMaterial mat = mParser->m_vMaterials.back();

			mat.mDiffuse  .set(0.6f,0.6f,0.6f);
			mat.mSpecular .set(1.0f,1.0f,1.0f);
			mat.mAmbient  .set(0.05f,0.05f,0.05f);
			mat.mShading  = D3DSHelper.Gouraud;
			mat.mName     = Material.AI_DEFAULT_MATERIAL_NAME;
		}
	}
	
	// -------------------------------------------------------------------
	/** Generate normal vectors basing on smoothing groups
	 * (in some cases the normal are already contained in the file)
	 * \param mesh Mesh to work on
	 * \return false if the normals have been recomputed
	 */
	boolean generateNormals(ASEMesh mesh){
		if (mesh.mNormals == null && !configRecomputeNormals)
		{
			// Check whether there are only uninitialized normals. If there are
			// some, skip all normals from the file and compute them on our own
//			for (std::vector<aiVector3D>::const_iterator qq =  mesh.mNormals.begin();qq != mesh.mNormals.end();++qq) {
//				if ((*qq).x || (*qq).y || (*qq).z)
//				{
//					return true;
//				}
//			}
			
			boolean flag = false;
			while(mesh.mNormals.remaining() > 0)
				if(mesh.mNormals.get() != 0){
					flag =  true;
					break;
				}
			mesh.mNormals.position(0);
			if(flag)
				return true;
		}
		// The array is reused.
		computeNormalsWithSmoothingsGroups(mesh);
		return false;
	}
	
	void computeNormalsWithSmoothingsGroups(ASEMesh sMesh)
	{
		// First generate face normals
//		sMesh.mNormals.resize(sMesh.mPositions.size(),aiVector3D());
//		Vector3f[] normals = new Vector3f[sMesh.mPositions.size()];
//		AssUtil.initArray(normals);
		FloatBuffer normals = MemoryUtil.createFloatBuffer(sMesh.mPositions.remaining(), AssimpConfig.MESH_USE_NATIVE_MEMORY);
		Vector3f pDelta1 = new Vector3f();
		Vector3f pDelta2 = new Vector3f();
		Vector3f pV1 = new Vector3f();
		Vector3f pV2 = new Vector3f();
		Vector3f pV3 = new Vector3f();
		for(int a = 0; a < sMesh.mFaces.size(); a++)
		{
			ASEFace face = sMesh.mFaces.get(a);

//			Vector3f pV1 = sMesh.mPositions.get(face.mIndices[0]);
//			Vector3f pV2 = sMesh.mPositions.get(face.mIndices[1]);
//			Vector3f pV3 = sMesh.mPositions.get(face.mIndices[2]);
			int index = face.mIndices[0] * 3;
			pV1.x = sMesh.mPositions.get(index++);
			pV1.y = sMesh.mPositions.get(index++);
			pV1.z = sMesh.mPositions.get(index++);
			
			index = face.mIndices[1] * 3;
			pV2.x = sMesh.mPositions.get(index++);
			pV2.y = sMesh.mPositions.get(index++);
			pV2.z = sMesh.mPositions.get(index++);
			
			index = face.mIndices[2] * 3;
			pV3.x = sMesh.mPositions.get(index++);
			pV3.y = sMesh.mPositions.get(index++);
			pV3.z = sMesh.mPositions.get(index++);

//			aiVector3D pDelta1 = *pV2 - *pV1;
//			aiVector3D pDelta2 = *pV3 - *pV1;
			Vector3f.sub(pV2, pV1, pDelta1);
			Vector3f.sub(pV3, pV1, pDelta2);
//			aiVector3D vNor = pDelta1 ^ pDelta2;
			Vector3f vNor = Vector3f.cross(pDelta1, pDelta2, pV3);

			for (int c = 0; c < 3;++c){
//				normals[face.mIndices[c]] = vNor;
				index = face.mIndices[c] * 3;
				normals.put(index++, vNor.x);
				normals.put(index++, vNor.y);
				normals.put(index++, vNor.z);
			}
		}

		// calculate the position bounds so we have a reliable epsilon to check position differences against 
		
		Vector3f minVec = pDelta1;
		Vector3f maxVec = pDelta2;
		minVec.set( 1e10f, 1e10f, 1e10f);
		maxVec.set( -1e10f, -1e10f, -1e10f);
		int size = sMesh.getNumPostions();
		for(int a = 0; a < size; a++)
		{
			int index = 3 * a;
			Vector3f position = pV1;
			position.x = sMesh.mPositions.get(index++);
			position.y = sMesh.mPositions.get(index++);
			position.z = sMesh.mPositions.get(index++);
			minVec.x = Math.min( minVec.x, position.x);
			minVec.y = Math.min( minVec.y, position.y);
			minVec.z = Math.min( minVec.z, position.z);
			maxVec.x = Math.max( maxVec.x, position.x);
			maxVec.y = Math.max( maxVec.y, position.y);
			maxVec.z = Math.max( maxVec.z, position.z);
		}
		final float posEpsilon = Vector3f.distance(maxVec, minVec) * 1e-5f;
//		std::vector<aiVector3D> avNormals;
//		avNormals.resize(sMesh.mNormals.size());
		
		// now generate the spatial sort tree
		SGSpatialSort sSort = new SGSpatialSort();
//		for( typename std::vector<T>::iterator i =  sMesh.mFaces.begin();
//			i != sMesh.mFaces.end();++i)
//		{
//			for (int c = 0; c < 3;++c)
//				sSort.Add(sMesh.mPositions[(*i).mIndices[c]],(*i).mIndices[c],(*i).iSmoothGroup);
//		}
		for(D3DSFace i : sMesh.mFaces){
			for(int c = 0; c < 3; c++){
				int index = i.mIndices[c];
				Vector3f vec3 = new Vector3f();
				vec3.x = sMesh.mPositions.get(index++);
				vec3.y = sMesh.mPositions.get(index++);
				vec3.z = sMesh.mPositions.get(index++);
				sSort.add(vec3, i.mIndices[c], i.iSmoothGroup);
			}
		}
		
		sSort.prepare();

//		std::vector<bool> vertexDone(sMesh.mPositions.size(),false);
		boolean[] vertexDone = new boolean[sMesh.getNumPostions()];
		final Vector3f vNormals = new Vector3f();
		IntList poResult = new IntArrayList();
//		for( typename std::vector<T>::iterator i =  sMesh.mFaces.begin();
//			i != sMesh.mFaces.end();++i)
		for(D3DSFace i : sMesh.mFaces)
		{
//			std::vector<int> poResult;
			for (int c = 0; c < 3;++c)
			{
				int idx = i.mIndices[c];
				if (vertexDone[idx])continue;

				int index = 3 * idx;
				pV1.x = sMesh.mPositions.get(index++);
				pV1.y = sMesh.mPositions.get(index++);
				pV1.z = sMesh.mPositions.get(index++);
				sSort.findPositions(pV1,i.iSmoothGroup,posEpsilon,poResult, false);

//				aiVector3D vNormals;
//				for (std::vector<int>::const_iterator
//					a =  poResult.begin();
//					a != poResult.end();++a)
				vNormals.set(0, 0, 0);
				for(int k = 0; k < poResult.size(); k ++)
				{
					int a = poResult.getInt(k);
//					vNormals += sMesh.mNormals[a];
//					Vector3f nor = normals[a];
					index = 3 * a;
					float norx = normals.get(index++);
					float nory = normals.get(index++);
					float norz = normals.get(index++);
					
					vNormals.x += norx;
					vNormals.y += nory;
					vNormals.z += norz;
				}
				vNormals.normalise();

				// write back into all affected normals
//				for (std::vector<int>::const_iterator
//					a =  poResult.begin();
//					a != poResult.end();++a)
				for(int k = 0; k < poResult.size(); k++)
				{
					idx = poResult.getInt(k);
//					normals[idx].set(vNormals);
					index = 3 * idx;
					normals.put(index++, vNormals.x);
					normals.put(index++, vNormals.y);
					normals.put(index++, vNormals.z);
					vertexDone[idx] = true;
				}
			}
		}
		sMesh.mNormals = normals;
//		sMesh.mNormals.clear();
//		for(Vector3f v : normals)
//			sMesh.mNormals.add(v);
	}

	// -------------------------------------------------------------------
	/** Create valid vertex/normal/UV/color/face lists.
	 *  All elements are unique, faces have only one set of indices
	 *  after this step occurs.
	 * \param mesh Mesh to work on
	 */
	void buildUniqueRepresentation(ASEMesh mesh){
		FloatBuffer mPositions = null;
		FloatBuffer[] amTexCoords = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];
		FloatBuffer  mVertexColors = null;
		FloatBuffer mNormals = null;
//		List<BoneVertex> mBoneVertices;

		int iSize = mesh.mFaces.size() * 3;
		mPositions = MemoryUtil.createFloatBuffer(iSize * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);

		// optional texture coordinates
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++i)	{
			if (mesh.amTexCoords[i] != null)	{
				amTexCoords[i] = MemoryUtil.createFloatBuffer(iSize * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			}
		}
		// optional vertex colors
		if (mesh.mVertexColors != null)	{
			mVertexColors = MemoryUtil.createFloatBuffer(iSize * 4, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		}

		// optional vertex normals (vertex normals can simply be copied)
		if (mesh.mNormals != null)	{
			mNormals= MemoryUtil.createFloatBuffer(iSize * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		}
		// bone vertices. There is no need to change the bone list
		if (!mesh.mBoneVertices.isEmpty())	{
//			mBoneVertices.resize();
		}

		// iterate through all faces in the mesh
		int iCurrent = 0, fi = 0;
//		for (std::vector<ASE::Face>::iterator i =  mesh.mFaces.begin();i != mesh.mFaces.end();++i,++fi)	{
		Vector3f vec = new Vector3f();
		for(int i = 0; i < mesh.mFaces.size(); ++i,++fi){
			ASEFace face = mesh.mFaces.get(i);
			for (int n = 0; n < 3;++n,++iCurrent)
			{
//				mPositions[iCurrent] = mesh.mPositions[(*i).mIndices[n]];
				MemoryUtil.arraycopy(mesh.mPositions, face.mIndices[n] * 3, mPositions, 3 * iCurrent, 3);

				// add texture coordinates
				for (int c = 0; c < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++c)	{
					if (mesh.amTexCoords[c] == null)break;
//					amTexCoords[c][iCurrent] = mesh.amTexCoords[c][(*i).amUVIndices[c][n]];
					MemoryUtil.arraycopy(mesh.amTexCoords[c], face.amUVIndices[c][n] * 3, amTexCoords[c], iCurrent * 3, 3);
				}
				// add vertex colors
				if (mesh.mVertexColors != null)	{
//					mVertexColors[iCurrent] = mesh.mVertexColors[(*i).mColorIndices[n]];
					MemoryUtil.arraycopy(mesh.mVertexColors, face.mColorIndices[n] * 4, mVertexColors, iCurrent * 4, 4);
				}
				// add normal vectors
				if (mesh.mNormals != null)	{
//					mNormals[iCurrent] = mesh.mNormals[fi*3+n];
//					mNormals[iCurrent].Normalize();
					int index = fi * 3 + n;
					vec.x = mesh.mNormals.get(index++);
					vec.y = mesh.mNormals.get(index++);
					vec.z = mesh.mNormals.get(index++);
					vec.normalise();
					index = iCurrent * 3;
					mNormals.put(index++, vec.x);
					mNormals.put(index++, vec.y);
					mNormals.put(index++, vec.z);
				}

				// handle bone vertices
				if (face.mIndices[n] < mesh.mBoneVertices.size())	{
					// (sometimes this will cause bone verts to be duplicated
					//  however, I' quite sure Schrompf' JoinVerticesStep
					//  will fix that again ...)
//					mBoneVertices[iCurrent] =  mesh.mBoneVertices[(*i).mIndices[n]];
				}
				face.mIndices[n] = iCurrent;
			}
		}

		// replace the old arrays
		mesh.mNormals = mNormals;
		mesh.mPositions = mPositions;
		mesh.mVertexColors = mVertexColors;

		for (int c = 0; c < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++c)
			mesh.amTexCoords[c] = amTexCoords[c];
	}
	
	// Copy a texture from the ASE structs to the output material
	void copyASETexture(Material mat, D3DSTexture texture, TextureType type)
	{
		// Setup the texture name
		String tex = texture.mMapName;
//		tex.Set( texture.mMapName);
		mat.addProperty(tex, Material._AI_MATKEY_TEXTURE_BASE, type.ordinal(),0);

		// Setup the texture blend factor
		if (!Float.isNaN(texture.mTextureBlend))
			mat.addProperty(texture.mTextureBlend, Material._AI_MATKEY_TEXBLEND_BASE, type.ordinal(),0);

		// Setup texture UV transformations
		float[] input = {texture.mOffsetU, texture.mOffsetV, texture.mScaleU, texture.mScaleV, texture.mRotation};
		mat.addProperty(input,Material._AI_MATKEY_UVTRANSFORM_BASE, type.ordinal(),0);
	}


	/** Create one-material-per-mesh meshes ;-)
	 * \param mesh Mesh to work with
	 *  \param Receives the list of all created meshes
	 */
	@SuppressWarnings("unchecked")
	void convertMeshes(ASEMesh mesh, List<Mesh> avOutMeshes){
		// validate the material index of the mesh
		if (mesh.iMaterialIndex >= mParser.m_vMaterials.size())	{
			mesh.iMaterialIndex = mParser.m_vMaterials.size()-1;
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("Material index is out of range");
		}

		// If the material the mesh is assigned to is consisting of submeshes, split it
		if (!mParser.m_vMaterials.get(mesh.iMaterialIndex).avSubMaterials.isEmpty())	{
			List<ASEMaterial> vSubMaterials = mParser.m_vMaterials.get(mesh.iMaterialIndex).avSubMaterials;

//			std::vector<int>* aiSplit = new std::vector<int>[vSubMaterials.size()];
			IntArrayList[] aiSplit = new IntArrayList[vSubMaterials.size()];
			AssUtil.initArray(aiSplit);

			// build a list of all faces per submaterial
			for (int i = 0; i < mesh.mFaces.size();++i)	{
				// check range
				if (mesh.mFaces.get(i).iMaterial >= vSubMaterials.size()) {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("Submaterial index is out of range");

					// use the last material instead
					aiSplit[vSubMaterials.size()-1].add(i);
				}
				else aiSplit[mesh.mFaces.get(i).iMaterial].add(i);
			}

			// now generate submeshes
			for (int p = 0; p < vSubMaterials.size();++p)	{
				if (!aiSplit[p].isEmpty())	{

					Mesh p_pcOut = new Mesh();
					p_pcOut.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;

					// let the sub material index
					p_pcOut.mMaterialIndex = p;

					// we will need this material
					mParser.m_vMaterials.get(mesh.iMaterialIndex).avSubMaterials.get(p).bNeed = true;

					// store the real index here ... color channel 3
//					p_pcOut.mColors[3] = (aiColor4D*)(uintptr_t)mesh.iMaterialIndex;
					p_pcOut.msg1 = mesh.iMaterialIndex;

					// store a pointer to the mesh in color channel 2
//					p_pcOut.mColors[2] = (aiColor4D*) &mesh;
					p_pcOut.tag = mesh;
					avOutMeshes.add(p_pcOut);

					// convert vertices
					p_pcOut.mNumVertices = aiSplit[p].size()*3;
//					p_pcOut.mNumFaces = aiSplit[p].size();
					int numFaces = aiSplit[p].size();

					// receive output vertex weights
//					std::vector<std::pair<int, float> > *avOutputBones = NULL;
//					if (!mesh.mBones.empty())	{
//						avOutputBones = new std::vector<std::pair<int, float> >[mesh.mBones.size()];
//					}
					List<IntFloatPair>[] avOutputBones = null;
					if(!mesh.mBones.isEmpty()){
						avOutputBones = new List[mesh.mBones.size()];
						for(int i = 0; i < avOutputBones.length; i++)
							avOutputBones[i] = new ArrayList<IntFloatPair>();
					}
					
					// allocate enough storage for faces
					p_pcOut.mFaces = new Face[numFaces];

					int iBase = 0,iIndex;
					if (p_pcOut.mNumVertices > 0)	{
//						p_pcOut.mVertices = new aiVector3D[p_pcOut.mNumVertices];
//						p_pcOut.mNormals  = new aiVector3D[p_pcOut.mNumVertices];
						p_pcOut.mVertices = MemoryUtil.createFloatBuffer(p_pcOut.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
						p_pcOut.mNormals = MemoryUtil.createFloatBuffer(p_pcOut.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
						for (int q = 0; q < aiSplit[p].size();++q)	{

							iIndex = aiSplit[p].getInt(q);

//							p_pcOut.mFaces[q].mIndices = new int[3];
//							p_pcOut.mFaces[q].mNumIndices = 3;
							p_pcOut.mFaces[q] = Face.createInstance(3);

							for (int t = 0; t < 3;++t, ++iBase)	{
								final int iIndex2 = mesh.mFaces.get(iIndex).mIndices[t];

//								p_pcOut.mVertices[iBase] = mesh.mPositions [iIndex2];
//								p_pcOut.mNormals [iBase] = mesh.mNormals   [iIndex2];
								MemoryUtil.arraycopy(mesh.mPositions, iIndex2 * 3, p_pcOut.mVertices, iBase * 3, 3);
								MemoryUtil.arraycopy(mesh.mNormals, iIndex2 * 3, p_pcOut.mNormals, iBase * 3, 3);

								// convert bones, if existing
								if (!mesh.mBones.isEmpty()) {
									// check whether there is a vertex weight for this vertex index
									if (iIndex2 < mesh.mBoneVertices.size())	{

//										for (std::vector<std::pair<int,float> >::const_iterator
//											blubb =  mesh.mBoneVertices[iIndex2].mBoneWeights.begin();
//											blubb != mesh.mBoneVertices[iIndex2].mBoneWeights.end();++blubb)	{
										for(IntFloatPair blubb : mesh.mBoneVertices.get(iIndex2).mBoneWeights){
											// NOTE: illegal cases have already been filtered out
//											avOutputBones[(*blubb).first].push_back(std::pair<int, float>(
//												iBase,(*blubb).second));
											avOutputBones[blubb.first].add(new IntFloatPair(iBase, blubb.second));
										}
									}
								}
								p_pcOut.mFaces[q].set(t, iBase);
							}
						}
					}
					// convert texture coordinates (up to AI_MAX_NUMBER_OF_TEXTURECOORDS sets supported)
					for (int c = 0; c < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++c) {
						if (mesh.amTexCoords[c] != null)
						{
//							p_pcOut.mTextureCoords[c] = new aiVector3D[p_pcOut.mNumVertices];
							p_pcOut.mTextureCoords[c] = MemoryUtil.createFloatBuffer(p_pcOut.mNumVertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
							iBase = 0;
							for (int q = 0; q < aiSplit[p].size();++q)	{
								iIndex = aiSplit[p].getInt(q);
								for (int t = 0; t < 3;++t)	{
//									p_pcOut.mTextureCoords[c][iBase++] = mesh.amTexCoords[c][mesh.mFaces[iIndex].mIndices[t]];
									MemoryUtil.arraycopy(mesh.amTexCoords[c], mesh.mFaces.get(iIndex).mIndices[t] * 3, p_pcOut.mTextureCoords[c], 3 * iBase++, 3);
								}
							}
							// Setup the number of valid vertex components
							p_pcOut.mNumUVComponents[c] = mesh.mNumUVComponents[c];
						}
					}

					// Convert vertex colors (only one set supported)
					if (mesh.mVertexColors != null){
//						p_pcOut.mColors[0] = new aiColor4D[p_pcOut.mNumVertices];
						p_pcOut.mColors[0] = MemoryUtil.createFloatBuffer(p_pcOut.mNumVertices * 4, AssimpConfig.MESH_USE_NATIVE_MEMORY);
						iBase = 0;
						for (int q = 0; q < aiSplit[p].size();++q)	{
							iIndex = aiSplit[p].getInt(q);
							for (int t = 0; t < 3;++t)	{
//								p_pcOut.mColors[0][iBase++] = mesh.mVertexColors[mesh.mFaces[iIndex].mIndices[t]];
								MemoryUtil.arraycopy(mesh.mVertexColors, mesh.mFaces.get(iIndex).mIndices[t] * 4, p_pcOut.mColors[0], 4 * iBase++, 4);
							}
						}
					}
					// Copy bones
					if (!mesh.mBones.isEmpty())	{
//						p_pcOut.mNumBones = 0;
						int numBones = 0;
						for (int mrspock = 0; mrspock < mesh.mBones.size();++mrspock)
							if (!avOutputBones[mrspock].isEmpty())numBones++;

						p_pcOut.mBones = new Bone [numBones ];
//						aiBone** pcBone = p_pcOut.mBones;
						int pcBone = 0;
						for (int mrspock = 0; mrspock < mesh.mBones.size();++mrspock)
						{
							if (!avOutputBones[mrspock].isEmpty())	{
								// we will need this bone. add it to the output mesh and
								// add all per-vertex weights
								Bone pc = p_pcOut.mBones[pcBone] = new Bone();
								pc.mName = (mesh.mBones.get(mrspock).mName);

//								pc.mNumWeights = avOutputBones[mrspock].size();
								pc.mWeights = new VertexWeight[avOutputBones[mrspock].size()];

								for (int captainkirk = 0; captainkirk < pc.mWeights.length;++captainkirk)
								{
									IntFloatPair ref = avOutputBones[mrspock].get(captainkirk);
//									pc->mWeights[captainkirk].mVertexId = ref.first;
//									pc->mWeights[captainkirk].mWeight = ref.second;
									pc.mWeights[captainkirk] = new VertexWeight(ref.first, ref.second);
								}
								++pcBone;
							}
						}
						// delete allocated storage
//						delete[] avOutputBones;
						avOutputBones = null;
					}
				}
			}
			// delete storage
//			delete[] aiSplit;
			aiSplit = null;
		}
		else
		{
			// Otherwise we can simply copy the data to one output mesh
			// This codepath needs less memory and uses fast memcpy()s
			// to do the actual copying. So I think it is worth the 
			// effort here.

			Mesh p_pcOut = new Mesh();
			p_pcOut.mPrimitiveTypes = Mesh.aiPrimitiveType_TRIANGLE;

			// set an empty sub material index
			p_pcOut.mMaterialIndex = ASEFace.DEFAULT_MATINDEX;
			mParser.m_vMaterials.get(mesh.iMaterialIndex).bNeed = true;

			// store the real index here ... in color channel 3
//			p_pcOut.mColors[3] = (aiColor4D*)(uintptr_t)mesh.iMaterialIndex;
			p_pcOut.msg1 = mesh.iMaterialIndex;

			// store a pointer to the mesh in color channel 2
//			p_pcOut.mColors[2] = (aiColor4D*) &mesh;
			p_pcOut.tag = mesh;
			avOutMeshes.add(p_pcOut);

			// If the mesh hasn't faces or vertices, there are two cases
			// possible: 1. the model is invalid. 2. This is a dummy
			// helper object which we are going to remove later ...
			if (mesh.mFaces.isEmpty() || mesh.mPositions == null)	{
				return;
			}

			// convert vertices
			p_pcOut.mNumVertices = mesh.mPositions.remaining()/3;
//			p_pcOut.mNumFaces = mesh.mFaces.size();
			int numFaces = mesh.mFaces.size();

			// allocate enough storage for faces
			p_pcOut.mFaces = new Face[numFaces];

			// copy vertices
//			p_pcOut.mVertices = new aiVector3D[mesh.mPositions.size()];
//			memcpy(p_pcOut.mVertices,&mesh.mPositions[0],
//				mesh.mPositions.size() * sizeof(aiVector3D));
			p_pcOut.mVertices = MemoryUtil.refCopy(mesh.mPositions, AssimpConfig.MESH_USE_NATIVE_MEMORY);   // reference copy

			// copy normals
//			p_pcOut.mNormals = new aiVector3D[mesh.mNormals.size()];
//			memcpy(p_pcOut.mNormals,&mesh.mNormals[0],
//				mesh.mNormals.size() * sizeof(aiVector3D));
			p_pcOut.mNormals = MemoryUtil.refCopy(mesh.mNormals, AssimpConfig.MESH_USE_NATIVE_MEMORY);

			// copy texture coordinates
			for (int c = 0; c < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++c)	{
				if (mesh.amTexCoords[c] != null)	{
//					p_pcOut.mTextureCoords[c] = new aiVector3D[mesh.amTexCoords[c].size()];
//					memcpy(p_pcOut.mTextureCoords[c],&mesh.amTexCoords[c][0],
//						mesh.amTexCoords[c].size() * sizeof(aiVector3D));
					p_pcOut.mTextureCoords[c] = MemoryUtil.refCopy(mesh.amTexCoords[c], AssimpConfig.MESH_USE_NATIVE_MEMORY);

					// setup the number of valid vertex components
					p_pcOut.mNumUVComponents[c] = mesh.mNumUVComponents[c];
				}
			}

			// copy vertex colors
			if (mesh.mVertexColors != null)	{
//				p_pcOut.mColors[0] = new aiColor4D[mesh.mVertexColors.size()];
//				memcpy(p_pcOut.mColors[0],&mesh.mVertexColors[0],
//					mesh.mVertexColors.size() * sizeof(aiColor4D));
				p_pcOut.mColors[0] = MemoryUtil.refCopy(mesh.mVertexColors, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			}

			// copy faces
			for (int iFace = 0; iFace < p_pcOut.getNumFaces();++iFace)	{
//				p_pcOut.mFaces[iFace].mNumIndices = 3;
//				p_pcOut.mFaces[iFace].mIndices = new int[3];
				Face face = p_pcOut.mFaces[iFace] = Face.createInstance(3);

				// copy indices 
//				p_pcOut.mFaces[iFace].mIndices[0] = mesh.mFaces[iFace].mIndices[0];
//				p_pcOut.mFaces[iFace].mIndices[1] = mesh.mFaces[iFace].mIndices[1];
//				p_pcOut.mFaces[iFace].mIndices[2] = mesh.mFaces[iFace].mIndices[2];
				int[] src = mesh.mFaces.get(iFace).mIndices;
				face.set(0, src[0]);
				face.set(1, src[1]);
				face.set(2, src[2]);
			}

			// copy vertex bones
			if (!mesh.mBones.isEmpty() && !mesh.mBoneVertices.isEmpty())	{
//				std::vector<std::vector<aiVertexWeight> > avBonesOut( mesh.mBones.size() );
				List<VertexWeight>[] avBonesOut = new List[mesh.mBones.size()];
				for(int i = 0; i < avBonesOut.length; i++)
					avBonesOut[i] = new ArrayList<VertexWeight>();

				// find all vertex weights for this bone
				int quak = 0;
//				for (std::vector<BoneVertex>::const_iterator harrypotter =  mesh.mBoneVertices.begin();
//					harrypotter != mesh.mBoneVertices.end();++harrypotter,++quak)	{
				for(int k = 0; k < mesh.mBoneVertices.size(); k++, ++quak){
					BoneVertex harrypotter = mesh.mBoneVertices.get(k);
					
//					for (std::vector<std::pair<int,float> >::const_iterator
//						ronaldweasley  = (*harrypotter).mBoneWeights.begin();
//						ronaldweasley != (*harrypotter).mBoneWeights.end();++ronaldweasley)
					for (IntFloatPair ronaldweasley : harrypotter.mBoneWeights)
					{
						VertexWeight weight = new VertexWeight();
						weight.mVertexId = quak;
						weight.mWeight = (ronaldweasley).second;
						avBonesOut[(ronaldweasley).first].add(weight);
					}
				}

				// now build a final bone list
//				p_pcOut.mNumBones = 0;
				int numBones = 0;
				for (int jfkennedy = 0; jfkennedy < mesh.mBones.size();++jfkennedy)
					if (!avBonesOut[jfkennedy].isEmpty())/*p_pcOut.mNumBones++*/numBones++;

				p_pcOut.mBones = new Bone[numBones];
//				aiBone** pcBone = p_pcOut.mBones;
				int pcBone = 0;
				for (int jfkennedy = 0; jfkennedy < mesh.mBones.size();++jfkennedy)	{
					if (!avBonesOut[jfkennedy].isEmpty())	{
						Bone pc = p_pcOut.mBones[pcBone] = new Bone();
						pc.mName =(mesh.mBones.get(jfkennedy).mName);
//						pc.mNumWeights = avBonesOut[jfkennedy].size();
						pc.mWeights = new VertexWeight[avBonesOut[jfkennedy].size()];
//						::memcpy(pc->mWeights,&avBonesOut[jfkennedy][0],
//							sizeof(aiVertexWeight) * pc->mNumWeights);
						avBonesOut[jfkennedy].toArray(pc.mWeights);
						++pcBone;
					}
				}
			}
		}
	}


	// -------------------------------------------------------------------
	/** Convert a material to a aiMaterial object
	 * \param mat Input material
	 */
	void convertMaterial(ASEMaterial mat){
		// LARGE TODO: Much code her is copied from 3DS ... join them maybe?

		// Allocate the output material
		mat.pcInstance = new Material();

		// At first add the base ambient color of the
		// scene to	the material
		mat.mAmbient.x += mParser.m_clrAmbient.x;
		mat.mAmbient.y += mParser.m_clrAmbient.y;
		mat.mAmbient.z += mParser.m_clrAmbient.z;

		mat.pcInstance.addProperty(mat.mName, Material.AI_MATKEY_NAME, 0, 0);

		// material colors
		mat.pcInstance.addProperty( mat.mAmbient, Material.AI_MATKEY_COLOR_AMBIENT,0,0);
		mat.pcInstance.addProperty( mat.mDiffuse, Material.AI_MATKEY_COLOR_DIFFUSE,0,0);
		mat.pcInstance.addProperty( mat.mSpecular,Material.AI_MATKEY_COLOR_SPECULAR,0,0);
		mat.pcInstance.addProperty( mat.mEmissive,Material.AI_MATKEY_COLOR_EMISSIVE,0,0);

		// shininess
		if (0.0f != mat.mSpecularExponent && 0.0f != mat.mShininessStrength)
		{
			mat.pcInstance.addProperty(mat.mSpecularExponent, Material.AI_MATKEY_SHININESS, 0, 0);
			mat.pcInstance.addProperty(mat.mShininessStrength, Material.AI_MATKEY_SHININESS_STRENGTH, 0, 0);
		}
		// If there is no shininess, we can disable phong lighting
		else if (D3DSHelper.Metal == mat.mShading ||
			D3DSHelper.Phong == mat.mShading ||
			D3DSHelper.Blinn == mat.mShading)
		{
			mat.mShading = D3DSHelper.Gouraud;
		}

		// opacity
		mat.pcInstance.addProperty(mat.mTransparency,Material.AI_MATKEY_OPACITY, 0,0);

		// Two sided rendering?
		if (mat.mTwoSided)
		{
			int i = 1;
			mat.pcInstance.addProperty(i,Material.AI_MATKEY_TWOSIDED, 0,0);
		}

		// shading mode
		ShadingMode eShading = ShadingMode.aiShadingMode_NoShading;
		switch (mat.mShading)
		{
			case D3DSHelper.Flat:
				eShading = ShadingMode.aiShadingMode_Flat; break;
			case D3DSHelper.Phong :
				eShading = ShadingMode.aiShadingMode_Phong; break;
			case D3DSHelper.Blinn :
				eShading = ShadingMode.aiShadingMode_Blinn; break;

				// I don't know what "Wire" shading should be,
				// assume it is simple lambertian diffuse (L dot N) shading
			case D3DSHelper.Wire:
				{
					// set the wireframe flag
					mat.pcInstance.addProperty(1,Material.AI_MATKEY_ENABLE_WIREFRAME, 0, 0);
				}
			case D3DSHelper.Gouraud:
				eShading = ShadingMode.aiShadingMode_Gouraud; break;
			case D3DSHelper.Metal :
				eShading = ShadingMode.aiShadingMode_CookTorrance; break;
		}
		mat.pcInstance.addProperty(eShading.ordinal() + 1,Material.AI_MATKEY_SHADING_MODEL, 0, 0);

		// DIFFUSE texture
		if( mat.sTexDiffuse.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexDiffuse, TextureType.aiTextureType_DIFFUSE);

		// SPECULAR texture
		if( mat.sTexSpecular.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexSpecular, TextureType.aiTextureType_SPECULAR);

		// AMBIENT texture
		if( mat.sTexAmbient.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexAmbient, TextureType.aiTextureType_AMBIENT);

		// OPACITY texture
		if( mat.sTexOpacity.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexOpacity, TextureType.aiTextureType_OPACITY);

		// EMISSIVE texture
		if( mat.sTexEmissive.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexEmissive, TextureType.aiTextureType_EMISSIVE);

		// BUMP texture
		if( mat.sTexBump.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexBump, TextureType.aiTextureType_HEIGHT);

		// SHININESS texture
		if( mat.sTexShininess.mMapName.length() > 0)
			copyASETexture(mat.pcInstance,mat.sTexShininess, TextureType.aiTextureType_SHININESS);

		// store the name of the material itself, too
		if( mat.mName.length() > 0)	{
//			aiString tex;tex.Set( mat.mName);
			mat.pcInstance.addProperty(mat.mName, Material.AI_MATKEY_NAME, 0, 0);
		}
		return;
	}


	// -------------------------------------------------------------------
	/** Setup the final material indices for each mesh
	 */
	void buildMaterialIndices(){
		int numMaterials = 0;
		// iterate through all materials and check whether we need them
		for (int iMat = 0; iMat < mParser.m_vMaterials.size();++iMat)
		{
			ASEMaterial mat = mParser.m_vMaterials.get(iMat);
			if (mat.bNeed)	{
				// Convert it to the aiMaterial layout
				convertMaterial(mat);
				++numMaterials;
			}
			for (int iSubMat = 0; iSubMat < mat.avSubMaterials.size();++iSubMat)
			{
				ASEMaterial submat = mat.avSubMaterials.get(iSubMat);
				if (submat.bNeed)	{
					// Convert it to the aiMaterial layout
					convertMaterial(submat);
					++numMaterials;
				}
			}
		}

		// allocate the output material array
		pcScene.mMaterials = new Material[numMaterials];
		D3DSMaterial[] pcIntMaterials = new D3DSMaterial[numMaterials];

		int iNum = 0;
		for (int iMat = 0; iMat < mParser.m_vMaterials.size();++iMat) {
			ASEMaterial mat = mParser.m_vMaterials.get(iMat);
			if (mat.bNeed)
			{
//				ai_assert(NULL != mat.pcInstance);
				pcScene.mMaterials[iNum] = mat.pcInstance;

				// Store the internal material, too
				pcIntMaterials[iNum] = mat;

				// Iterate through all meshes and search for one which is using
				// this top-level material index
				for (int iMesh = 0; iMesh < pcScene.getNumMeshes();++iMesh)
				{
					Mesh mesh = pcScene.mMeshes[iMesh];
					if (ASEFace.DEFAULT_MATINDEX == mesh.mMaterialIndex &&
						iMat == /*(uintptr_t)mesh->mColors[3]*/mesh.msg1)
					{
						mesh.mMaterialIndex = iNum;
//						mesh->mColors[3] = NULL;
						mesh.msg1 = 0;
					}
				}
				iNum++;
			}
			for (int iSubMat = 0; iSubMat < mat.avSubMaterials.size();++iSubMat)	{
				ASEMaterial submat = mat.avSubMaterials.get(iSubMat);
				if (submat.bNeed)	{
//					ai_assert(NULL != submat.pcInstance);
					pcScene.mMaterials[iNum] = submat.pcInstance;

					// Store the internal material, too
					pcIntMaterials[iNum] = submat;

					// Iterate through all meshes and search for one which is using
					// this sub-level material index
					for (int iMesh = 0; iMesh < pcScene.getNumMeshes();++iMesh)	{
						Mesh mesh = pcScene.mMeshes[iMesh];

						if (iSubMat == mesh.mMaterialIndex && iMat == mesh.msg1/*(uintptr_t)mesh->mColors[3]*/)	{
							mesh.mMaterialIndex = iNum;
//							mesh->mColors[3]    = NULL;
							mesh.msg1           = 0;
						}
					}
					iNum++;
				}
			}
		}

		// Dekete our temporary array
//		delete[] pcIntMaterials;
		pcIntMaterials = null;
	}


	// -------------------------------------------------------------------
	/** Build the node graph
	 */
	void buildNodes(List<BaseNode> nodes){
		// allocate the one and only root node
		Node root = pcScene.mRootNode = new Node();
		root.mName = ("<ASERoot>");

		// Setup the coordinate system transformation
//		pcScene->mRootNode->mNumChildren = 1;
		pcScene.mRootNode.mChildren = new Node[1];
		Node ch = pcScene.mRootNode.mChildren[0] = new Node();
		ch.mParent = root;

		// Change the transformation matrix of all nodes
//		for (std::vector<BaseNode*>::iterator it = nodes.begin(), end = nodes.end();it != end; ++it)	{
		for (BaseNode it : nodes){
//			aiMatrix4x4& m = (*it)->mTransform;
//			m.Transpose(); // row-order vs column-order
			it.mTransform.transpose();
		}

		// add all nodes
		addNodes(nodes,ch,null);

		// now iterate through al nodes and find those that have not yet
		// been added to the nodegraph (= their parent could not be recognized)
//		std::vector<const BaseNode*> aiList;
		List<BaseNode> aiList = new ArrayList<BaseNode>();
//		for (std::vector<BaseNode*>::iterator it = nodes.begin(), end = nodes.end();it != end; ++it)	{
		for (BaseNode it : nodes){
			if (it.mProcessed) {
				continue;
			}

			// check whether our parent is known
			boolean bKnowParent = false;
			
			// search the list another time, starting *here* and try to find out whether
			// there is a node that references *us* as a parent
//			for (std::vector<BaseNode*>::const_iterator it2 = nodes.begin();it2 != end; ++it2) {
			for (BaseNode it2 : nodes){
				if (it2 == it) {
					continue;
				}

//				if ((*it2)->mName == (*it)->mParent)	{
			    if (it2.mName.equals(it.mParent)) {
					bKnowParent = true;
					break;
				}
			}
			if (!bKnowParent)	{
				aiList.add(it);
			}
		}

		// Are there ane orphaned nodes?
		if (!aiList.isEmpty())	{
//			std::vector<aiNode*> apcNodes;
//			apcNodes.reserve(aiList.size() + pcScene->mRootNode->mNumChildren);
			List<Node> apcNodes = new ArrayList<Node>(aiList.size() + pcScene.mRootNode.getNumChildren());

			for (int i = 0; i < pcScene.mRootNode.getNumChildren();++i)
				apcNodes.add(pcScene.mRootNode.mChildren[i]);

//			delete[] pcScene->mRootNode->mChildren;
			pcScene.mRootNode.mChildren = null;
//			for (std::vector<const BaseNode*>::/*const_*/iterator i =  aiList.begin();i != aiList.end();++i){
			for (BaseNode i : aiList){
				BaseNode src = i;

				// The parent is not known, so we can assume that we must add 
				// this node to the root node of the whole scene
				Node pcNode = new Node();
				pcNode.mParent = pcScene.mRootNode;
				pcNode.mName = (src.mName);
				addMeshes(src,pcNode);
				addNodes(nodes,pcNode,pcNode.mName);
				apcNodes.add(pcNode);
			}

			// Regenerate our output array
//			pcScene->mRootNode->mChildren = new aiNode*[apcNodes.size()];
//			for (int i = 0; i < apcNodes.size();++i)
//				pcScene->mRootNode->mChildren[i] = apcNodes[i];
//
//			pcScene->mRootNode->mNumChildren = apcNodes.size();
			pcScene.mRootNode.mChildren = apcNodes.toArray(new Node[apcNodes.size()]);
		}

		// Reset the third color set to NULL - we used this field to store a temporary pointer
		for (int i = 0; i < pcScene.getNumMeshes();++i)
			pcScene.mMeshes[i].tag = null;

		// The root node should not have at least one child or the file is valid
		if (pcScene.mRootNode.mChildren == null) {
			throw new DeadlyImportError("ASE: No nodes loaded. The file is either empty or corrupt");
		}
		
		// Now rotate the whole scene 90 degrees around the x axis to convert to internal coordinate system
//		pcScene->mRootNode->mTransformation = aiMatrix4x4(1.f,0.f,0.f,0.f,
//			0.f,0.f,1.f,0.f,0.f,-1.f,0.f,0.f,0.f,0.f,0.f,1.f);
		Matrix4f mat = pcScene.mRootNode.mTransformation;
		mat.setColumn(0, 1.f,0.f,0.f,0.f);
		mat.setColumn(1, 0.f,0.f,1.f,0.f);
		mat.setColumn(2, 0.f,-1.f,0.f,0.f);
		mat.setColumn(3, 0.f,0.f,0.f,1.f);
	}


	// -------------------------------------------------------------------
	/** Build output cameras
	 */
	void buildCameras(){
		if (!mParser.m_vCameras.isEmpty())	{
//			pcScene.mNumCameras = mParser->m_vCameras.size();
			pcScene.mCameras = new Camera[mParser.m_vCameras.size()];

			for (int i = 0; i < pcScene.mCameras.length;++i)	{
				Camera out = pcScene.mCameras[i] = new Camera();
				ASECamera in = mParser.m_vCameras.get(i);

				// copy members
				out.mClipPlaneFar  = in.mFar;
				out.mClipPlaneNear = (in.mNear != 0.0f ? in.mNear : 0.1f); 
				out.mHorizontalFOV = in.mFOV;

				out.mName = (in.mName);
			}
		}
	}
	
	/** Build output lights
	 */
	void buildLights(){
		if (!mParser.m_vLights.isEmpty())	{
//			pcScene.mNumLights = mParser->m_vLights.size();
			pcScene.mLights    = new Light[mParser.m_vLights.size()];

			for (int i = 0; i < pcScene.mLights.length;++i)	{
				Light out = pcScene.mLights[i] = new Light();
				ASELight in = mParser.m_vLights.get(i);

				// The direction is encoded in the transformation matrix of the node. 
				// In 3DS MAX the light source points into negative Z direction if 
				// the node transformation is the identity. 
				out.mDirection.set(0.f,0.f,-1.f);

				out.mName = (in.mName);
				switch (in.mLightType)
				{
				case ASELight.TARGET:
					out.mType = LightSourceType.aiLightSource_SPOT;
					out.mAngleInnerCone = (float)Math.toRadians(in.mAngle);
					out.mAngleOuterCone = (in.mFalloff != 0? (float)Math.toRadians(in.mFalloff) : out.mAngleInnerCone);
					break;

				case ASELight.DIRECTIONAL:
					out.mType = LightSourceType.aiLightSource_DIRECTIONAL;
					break;

				default:
				//case ASELight.OMNI:
					out.mType = LightSourceType.aiLightSource_POINT;
					break;
				};
//				out->mColorDiffuse = out->mColorSpecular = in.mColor * in.mIntensity;
				out.mColorDiffuse.x = out.mColorSpecular.x = in.mColor.x * in.mIntensity;
				out.mColorDiffuse.y = out.mColorSpecular.y = in.mColor.y * in.mIntensity;
				out.mColorDiffuse.z = out.mColorSpecular.z = in.mColor.z * in.mIntensity;
			}
		}
	}


	// -------------------------------------------------------------------
	/** Build output animations
	 */
	void buildAnimations(List<BaseNode> nodes){
		// check whether we have at least one mesh which has animations
//		std::vector<ASE::BaseNode*>::const_iterator i =  nodes.begin();
	    int iNum = 0;
	    int i = 0;
		for (;i != nodes.size();++i)	{

			BaseNode node = nodes.get(i);
			if(DefaultLogger.LOG_OUT){
				// TODO: Implement Bezier & TCB support
				if (node.mAnim.mPositionType != ASEAnimation.TRACK)	{
					DefaultLogger.warn("ASE: Position controller uses Bezier/TCB keys. This is not supported.");
				}
				if (node.mAnim.mRotationType != ASEAnimation.TRACK)	{
					DefaultLogger.warn("ASE: Rotation controller uses Bezier/TCB keys. This is not supported.");
				}
				if (node.mAnim.mScalingType != ASEAnimation.TRACK)	{
					DefaultLogger.warn("ASE: Position controller uses Bezier/TCB keys. This is not supported.");
				}
			}

			// We compare against 1 here - firstly one key is not
			// really an animation and secondly MAX writes dummies
			// that represent the node transformation.
			if (node.mAnim.akeyPositions.size()>1 || node.mAnim.akeyRotations.size()>1 || node.mAnim.akeyScaling.size()>1){
				++iNum;
			}
			if (node.mTargetAnim.akeyPositions.size() > 1 && !Float.isNaN( node.mTargetPosition.x )) {
				++iNum;
			}
		}
		if (iNum > 0)	{
			// Generate a new animation channel and setup everything for it
//			pcScene.mNumAnimations = 1;
			pcScene.mAnimations     = new Animation[1];
			Animation pcAnim        = pcScene.mAnimations[0] = new Animation();
//			pcAnim->mNumChannels    = iNum;
			pcAnim.mChannels        = new NodeAnim[iNum];
			pcAnim.mTicksPerSecond  = mParser.iFrameSpeed * mParser.iTicksPerFrame;

			iNum = 0;
			
			// Now iterate through all meshes and collect all data we can find
			for (i = 0;i != nodes.size();++i)	{

				BaseNode me = nodes.get(i);
				if ( me.mTargetAnim.akeyPositions.size() > 1 && !Float.isNaN( me.mTargetPosition.x ))	{
					// Generate an extra channel for the camera/light target.
					// BuildNodes() does also generate an extra node, named
					// <baseName>.Target.
					NodeAnim nd = pcAnim.mChannels[iNum++] = new NodeAnim();
					nd.mNodeName = (me.mName + ".Target");

					// If there is no input position channel we will need
					// to supply the default position from the node's
					// local transformation matrix.
					/*TargetAnimationHelper helper;
					if (me->mAnim.akeyPositions.empty())
					{
						aiMatrix4x4& mat = (*i)->mTransform;
						helper.SetFixedMainAnimationChannel(aiVector3D(
							mat.a4, mat.b4, mat.c4));
					}
					else helper.SetMainAnimationChannel (&me->mAnim.akeyPositions);
					helper.SetTargetAnimationChannel (&me->mTargetAnim.akeyPositions);
					
					helper.Process(&me->mTargetAnim.akeyPositions);*/

					// Allocate the key array and fill it
//					nd->mNumPositionKeys =  me->mTargetAnim.akeyPositions.size();
					nd.mPositionKeys = new VectorKey[me.mTargetAnim.akeyPositions.size()];

//					::memcpy(nd->mPositionKeys,&me->mTargetAnim.akeyPositions[0],
//						nd->mNumPositionKeys * sizeof(aiVectorKey));
					me.mTargetAnim.akeyPositions.toArray(nd.mPositionKeys);
				}

				if (me.mAnim.akeyPositions.size() > 1 || me.mAnim.akeyRotations.size() > 1 || me.mAnim.akeyScaling.size() > 1)	{
					// Begin a new node animation channel for this node
					NodeAnim nd = pcAnim.mChannels[iNum++] = new NodeAnim();
					nd.mNodeName = (me.mName);

					// copy position keys
					if (me.mAnim.akeyPositions.size() > 1 )
					{
						// Allocate the key array and fill it
//						nd->mNumPositionKeys =  me->mAnim.akeyPositions.size();
//						nd->mPositionKeys = new aiVectorKey[nd->mNumPositionKeys];
//
//						::memcpy(nd->mPositionKeys,&me->mAnim.akeyPositions[0],
//							nd->mNumPositionKeys * sizeof(aiVectorKey));
						nd.mPositionKeys = me.mAnim.akeyPositions.toArray(new VectorKey[me.mAnim.akeyPositions.size()]);
					}
					// copy rotation keys
					if (me.mAnim.akeyRotations.size() > 1 )	{
						// Allocate the key array and fill it
//						nd.mNumRotationKeys =  me->mAnim.akeyRotations.size();
						nd.mRotationKeys = new QuatKey[me.mAnim.akeyRotations.size()];

						// --------------------------------------------------------------------
						// Rotation keys are offsets to the previous keys.
						// We have the quaternion representations of all 
						// of them, so we just need to concatenate all
						// (unit-length) quaternions to get the absolute
						// rotations.
						// Rotation keys are ABSOLUTE for older files
						// --------------------------------------------------------------------
						Quaternion cur = new Quaternion();
						for (int a = 0; a < nd.mRotationKeys.length;++a)	{
							QuatKey q = me.mAnim.akeyRotations.get(a);

							if (mParser.iFileFormat > 110)	{
//								cur = (a > 0 ? cur*q.mValue : q.mValue);
								if(a > 0){
									Quaternion.mul(cur, q.mValue, cur);
								}else{
									cur.set(q.mValue);
								}
//								q.mValue = cur.normalize();
								cur.normalise();
								q.mValue.set(cur);
							}
							nd.mRotationKeys[a] = new QuatKey(q.mTime, q.mValue); 

							// need this to get to Assimp quaternion conventions
							nd.mRotationKeys[a].mValue.w *= -1.f;
						}
					}
					// copy scaling keys
					if (me.mAnim.akeyScaling.size() > 1 )	{
						// Allocate the key array and fill it
//						nd.mNumScalingKeys = me.mAnim.akeyScaling.size();
//						nd.mScalingKeys = new VectorKey[nd.mNumScalingKeys];
//
//						::memcpy(nd->mScalingKeys,&me->mAnim.akeyScaling[0],
//							nd->mNumScalingKeys * sizeof(aiVectorKey));
						
						nd.mScalingKeys = me.mAnim.akeyScaling.toArray(new VectorKey[me.mAnim.akeyScaling.size()]);
					}
				}
			}
		}
	}


	// -------------------------------------------------------------------
	/** Add sub nodes to a node
	 *  \param pcParent parent node to be filled
	 *  \param szName Name of the parent node
	 *  \param matrix Current transform
	 */
	void addNodes(List<BaseNode> nodes, Node pcParent, String szName){
		addNodes(nodes, pcParent, szName, null);
	}		

	void addNodes(List<BaseNode> nodes, Node pcParent, String szName,Matrix4f matrix){
		Matrix4f mParentAdjust = null;
		final int len = szName != null ? szName.length() : 0;
//		ai_assert(4 <= AI_MAX_NUMBER_OF_COLOR_SETS);

		// Receives child nodes for the pcParent node
//		std::vector<aiNode*> apcNodes;
		List<Node> apcNodes = new ArrayList<Node>();
		
		// Now iterate through all nodes in the scene and search for one
		// which has *us* as parent.
//		for (std::vector<BaseNode*>::const_iterator it = nodes.begin(), end = nodes.end(); it != end; ++it) {
		for(BaseNode snode : nodes){
//			const BaseNode* snode = *it;
			if (szName != null)	{
				if (len != snode.mParent.length() || !szName.equals(snode.mParent)/*::strcmp(szName,snode->mParent.c_str())*/)
					continue;
			}
			else if (snode.mParent.length() > 0)
				continue;

			snode.mProcessed = true;

			// Allocate a new node and add it to the output data structure
			Node node;
			apcNodes.add(node = new Node());

//			node->mName.Set((snode->mName.length() ? snode->mName.c_str() : "Unnamed_Node"));
			if(snode.mName.length() > 0)
				node.mName = snode.mName;
			else
				node.mName = "Unnamed_Node";
			node.mParent = pcParent;

			// Setup the transformation matrix of the node
//			aiMatrix4x4 mParentAdjust  = mat;
//			mParentAdjust.Inverse();
//			node->mTransformation = mParentAdjust*snode->mTransform;
			if(matrix != null){
				mParentAdjust = Matrix4f.invert(matrix, mParentAdjust);
				Matrix4f.mul(snode.mTransform, mParentAdjust, node.mTransformation);
			}else{
				node.mTransformation.load(snode.mTransform);
			}

			// Add sub nodes - prevent stack overflow due to recursive parenting
			if (!node.mName.equals(node.mParent.mName)) {
				addNodes(nodes,node,node.mName,snode.mTransform);
			}

			// Further processing depends on the type of the node
			if (snode.mType == BaseNode.MESH)	{
				// If the type of this node is "Mesh" we need to search
				// the list of output meshes in the data structure for
				// all those that belonged to this node once. This is
				// slightly inconvinient here and a better solution should
				// be used when this code is refactored next.
				addMeshes(snode,node);
			}
			else if (!Float.isNaN( snode.mTargetPosition.x ))	{
				// If this is a target camera or light we generate a small
				// child node which marks the position of the camera
				// target (the direction information is contained in *this*
				// node's animation track but the exact target position
				// would be lost otherwise)
				if (node.mChildren == null)	{
					node.mChildren = new Node[1];
				}

				Node nd = new Node();

				nd.mName = ( snode.mName + ".Target" );

				nd.mTransformation.m30 = snode.mTargetPosition.x - snode.mTransform.m30;
				nd.mTransformation.m31 = snode.mTargetPosition.y - snode.mTransform.m31;
				nd.mTransformation.m32 = snode.mTargetPosition.z - snode.mTransform.m32;

				nd.mParent = node;

				// The .Target node is always the first child node 
				for (int m = 0; m < node.getNumChildren() - 1;++m)
					node.mChildren[m+1] = node.mChildren[m]; 
			
				node.mChildren[0] = nd;
//				node->mNumChildren++;

				// What we did is so great, it is at least worth a debug message
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.debug("ASE: Generating separate target node ("+snode.mName+")");
			}
		}

		// Allocate enough space for the child nodes
		// We allocate one slot more  in case this is a target camera/light
		int numChildren = apcNodes.size();
		if (numChildren > 0)	{
			pcParent.mChildren = new Node[apcNodes.size()+1 /* PLUS ONE !!! */];

			// now build all nodes for our nice new children
//			for (int p = 0; p < apcNodes.size();++p)
//				pcParent->mChildren[p] = apcNodes[p];
			apcNodes.toArray(pcParent.mChildren);
		}
		return;
	}

	void addMeshes(BaseNode snode,Node node){
		Matrix4f m = new Matrix4f();
		Vector3f v = new Vector3f();
		
		int node_numMeshes = node.getNumMeshes();
		for (int i = 0; i < pcScene.getNumMeshes();++i)	{
			// Get the name of the mesh (the mesh instance has been temporarily stored in the third vertex color)
			Mesh pcMesh  = pcScene.mMeshes[i];
			ASEMesh mesh = (ASEMesh)pcMesh.tag;  //pcMesh->mColors[2];

			if (mesh == snode) {
				++node_numMeshes;
			}
		}

		if(node_numMeshes > 0)	{
			node.mMeshes = new int[node_numMeshes];
			for (int i = 0, p = 0; i < pcScene.getNumMeshes();++i)	{
				Mesh pcMesh  = pcScene.mMeshes[i];
				ASEMesh mesh = (ASEMesh)pcMesh.tag;  //pcMesh->mColors[2];
				if (mesh == snode)	{
					node.mMeshes[p++] = i;

					// Transform all vertices of the mesh back into their local space -> 
					// at the moment they are pretransformed
//					Matrix4f m  = mesh.mTransform;
//					m.Inverse();
					Matrix4f.invert(mesh.mTransform, m);

//					aiVector3D* pvCurPtr = pcMesh->mVertices;
//					const aiVector3D* pvEndPtr = pvCurPtr + pcMesh->mNumVertices;
//					while (pvCurPtr != pvEndPtr)	{
//						*pvCurPtr = m * (*pvCurPtr);
//						pvCurPtr++;
//					}
					int pvCurPtr = 0;
					int pvEndPtr = pcMesh.mNumVertices;
					while(pvCurPtr != pvEndPtr){
						int pos = 3 * pvCurPtr;
						v.load(pcMesh.mVertices);
						Matrix4f.transformVector(m, v, v);
						pcMesh.mVertices.put(pos ++, v.x);
						pcMesh.mVertices.put(pos ++, v.y);
						pcMesh.mVertices.put(pos ++, v.z);
					}
					pcMesh.mVertices.flip();
					

					// Do the same for the normal vectors, if we have them.
					// As always, inverse transpose.
					if (pcMesh.mNormals != null)	{
//						aiMatrix3x3 m3 = aiMatrix3x3( mesh->mTransform );
//						m3.Transpose();
						Matrix4f.transpose(mesh.mTransform, m);

//						pvCurPtr = pcMesh->mNormals;
//						pvEndPtr = pvCurPtr + pcMesh->mNumVertices;
//						while (pvCurPtr != pvEndPtr)	{
//							*pvCurPtr = m3 * (*pvCurPtr);
//							pvCurPtr++;
//						}
						
						pvCurPtr = 0;
						pvEndPtr = pcMesh.mNumVertices;
						while(pvCurPtr != pvEndPtr){
							int pos = 3 * pvCurPtr;
							v.load(pcMesh.mNormals);
							Matrix4f.transformNormal(m, v, v);
							pcMesh.mNormals.put(pos ++, v.x);
							pcMesh.mNormals.put(pos ++, v.y);
							pcMesh.mNormals.put(pos ++, v.z);
						}
						pcMesh.mNormals.flip();
					}
				}
			}
		}
	}

}
