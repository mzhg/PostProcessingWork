package assimp.common;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** Static helper class providing various utilities to merge two
 *    scenes. It is intended as internal utility and NOT for use by 
 *    applications.<p>
 * 
 * The class is currently being used by various postprocessing steps
 * and loaders (ie. LWS).
 */
public class SceneCombiner {

	/** 
	 *  Generate unique names for all named scene items
	 */
	public static final int AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES = 0x1;

	/** 
	 *  Generate unique names for materials, too. 
	 *  This is not absolutely required to pass the validation.
	 */
	public static final int AI_INT_MERGE_SCENE_GEN_UNIQUE_MATNAMES = 0x2;

	/** 
	 * Use deep copies of duplicate scenes
	 */
	public static final int AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY = 0x4;

	/** 
	 * If attachment nodes are not found in the given master scene,
	 * search the other imported scenes for them in an any order.
	 */
	public static final int AI_INT_MERGE_SCENE_RESOLVE_CROSS_ATTACHMENTS = 0x8;

	/** 
	 * Can be combined with AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES.
	 * Unique names are generated, but only if this is absolutely
	 * required to avoid name conflicts.
	 */
	public static final int AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY = 0x10;
	
	// Add a prefix to a string
	private static String prefixString(String string, String prefix){
		// If the string is already prefixed, we won't prefix it a second time
		if (string.length() >= 1 && string.charAt(0) == '$')
			return string;
		
		if (prefix.length() +string.length()>=AssimpConfig.MAXLEN-1) {
			throw new IllegalArgumentException("Can't add an unique prefix because the string is too long");
		}
		
		return prefix + string;
	}
	
	// -------------------------------------------------------------------
	/** Merges two or more scenes.
	 *
	 *  @param src Non-empty list of scenes to be merged. The function
	 *    deletes the input scenes afterwards. There may be duplicate scenes.
	 *  @param flags Combination of the AI_INT_MERGE_SCENE flags defined above
	 *  @param dest  Receives a pointer to the destination scene. If the
	 *    pointer doesn't point to NULL when the function is called, the
	 *    existing scene is cleared and refilled.
	 */
	public static Scene mergeScenes(List<Scene> src,int flags, Scene dest){
		// if _dest points to NULL allocate a new scene. Otherwise clear the old and reuse it
		if (src.isEmpty())
		{
			if (dest != null)
			{
//				(*_dest)->~aiScene();
				copySceneFlat(src.get(0), dest);
			}
			else dest = src.get(0);
			return dest;
		}
//		if (*_dest)(*_dest)->~aiScene();
//		else *_dest = new aiScene();
		if(dest == null)
			dest = new Scene();

		// Create a dummy scene to serve as master for the others
		Scene master = new Scene();
		master.mRootNode = new Node();
		master.mRootNode.mName = ("<MergeRoot>");

		List<AttachmentInfo> srcList = new ArrayList<AttachmentInfo>(src.size());
		for (int i = 0; i < srcList.size();++i)	{
			srcList.add(new AttachmentInfo(src.get(i),master.mRootNode));
		}

		// 'master' will be deleted afterwards
		mergeScenes (master, srcList, flags, dest);
		return dest;
	}


	// -------------------------------------------------------------------
	/** Merges two or more scenes and attaches all sceenes to a specific
	 *  position in the node graph of the masteer scene.
	 *
	 *  @param master Master scene. It will be deleted afterwards. All 
	 *    other scenes will be inserted in its node graph.
	 *  @param srcList Non-empty list of scenes to be merged along with their
	 *    corresponding attachment points in the master scene. The function
	 *    deletes the input scenes afterwards. There may be duplicate scenes.
	 *  @param flags Combination of the AI_INT_MERGE_SCENE flags defined above
	 *  @param dest Receives a pointer to the destination scene. If the
	 *    pointer doesn't point to NULL when the function is called, the
	 *    existing scene is cleared and refilled.
	 */
	@SuppressWarnings("deprecation")
	public static Scene mergeScenes(Scene master, List<AttachmentInfo> srcList, int flags, Scene dest){
		// if _dest points to null allocate a new scene. Otherwise clear the old and reuse it
		if (srcList.isEmpty())	{
			if (dest != null)	{
				copySceneFlat(master, dest);
			}
			else dest = master;
			return dest;
		}
		if (dest != null) {
//			(*_dest)->~aiScene();
//			new (*_dest) aiScene();
		}
		else dest = new Scene();

		List<SceneHelper> src = new ArrayList<>(srcList.size()+1);
//		src[0].scene = master;
		src.add(new SceneHelper(master));
		for (int i = 0; i < srcList.size();++i)	{
//			src[i+1] = SceneHelper( srcList[i].scene );
			src.add(new SceneHelper(srcList.get(i).scene));
		}

		// this helper array specifies which scenes are duplicates of others
//		std::vector<unsigned int> duplicates(src.size(),UINT_MAX);
		int[] duplicates = new int[src.size()];
		Arrays.fill(duplicates, -1);

		// this helper array is used as lookup table several times
//		std::vector<unsigned int> offset(src.size());
		int[] offset = new int[src.size()];

		// Find duplicate scenes
		for (int i = 0; i < src.size();++i) {
			if (duplicates[i] != i && duplicates[i] != -1) {
				continue;
			}
				
			duplicates[i] = i;
			Scene i_scene = src.get(i).scene;
			for (int a = i+1; a < src.size(); ++a)	{
				if (i_scene == src.get(a).scene) {
					duplicates[a] = i;
				}
			}
		}
		
		// Generate unique names for all named stuff?
		if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES) != 0)
		{
//	#if 0
//			// Construct a proper random number generator
//			boost::mt19937 rng(  );
//			boost::uniform_int<> dist(1u,1 << 24u);
//			boost::variate_generator<boost::mt19937&, boost::uniform_int<> > rndGen(rng, dist);   
//	#endif
			for (int i = 1; i < src.size();++i)
			{
				//if (i != duplicates[i]) 
				//{
				//	// duplicate scenes share the same UID
				//	::strcpy( src[i].id, src[duplicates[i]].id );
				//	src[i].idlen = src[duplicates[i]].idlen;

				//	continue;
				//}

//				src[i].idlen = ::sprintf(src[i].id,"$%.6X$_",i);
				SceneHelper src_i = src.get(i);

				if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY) != 0) {
					
					// Compute hashes for all identifiers in this scene and store them
					// in a sorted table (for convenience I'm using std::set). We hash
					// just the node and animation channel names, all identifiers except
					// the material names should be caught by doing this.
					addNodeHashes(src_i.scene.mRootNode,src_i.hashes);

					for (int a = 0; a < src_i.scene.getNumAnimations();++a) {
						Animation anim = src_i.scene.mAnimations[a];
						src_i.hashes.add(anim.mName.hashCode()/*SuperFastHash(anim->mName.data,anim->mName.length)*/);
					}
				}
			}
		}
		
		int cnt;

		// First find out how large the respective output arrays must be
		int numTextures = dest.getNumTextures();
		int numMaterials = dest.getNumMaterials();
		int numMeshes = dest.getNumMeshes();
		int numLights = dest.getNumLights();
		int numCameras = dest.getNumCameras();
		int numAnimations = dest.getNumAnimations();
		
		for (int n = 0; n < src.size();++n )
		{
			SceneHelper cur = src.get(n);

			if (n == duplicates[n] || (flags & AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY) != 0)	{
//				dest->mNumTextures   += (*cur)->mNumTextures;
//				dest->mNumMaterials  += (*cur)->mNumMaterials;
//				dest->mNumMeshes     += (*cur)->mNumMeshes;
				
				numTextures += cur.scene.getNumTextures();
				numMaterials += cur.scene.getNumMaterials();
				numMeshes += cur.scene.getNumMeshes();
			}

//			dest->mNumLights     += (*cur)->mNumLights;
//			dest->mNumCameras    += (*cur)->mNumCameras;
//			dest->mNumAnimations += (*cur)->mNumAnimations;
			numLights += cur.scene.getNumLights();
			numCameras += cur.scene.getNumCameras();
			numAnimations += cur.scene.getNumAnimations();

			// Combine the flags of all scenes
			// We need to process them flag-by-flag here to get correct results
			// dest->mFlags ; //|= (*cur)->mFlags;
			if ((cur.scene.mFlags & Scene.AI_SCENE_FLAGS_NON_VERBOSE_FORMAT)!=0) {
				dest.mFlags |= Scene.AI_SCENE_FLAGS_NON_VERBOSE_FORMAT;
			}
		}

		// generate the output texture list + an offset table for all texture indices
		if (numTextures > 0)
		{
			Texture[] pip = dest.mTextures = new Texture[numMaterials];
			int pip_index = 0;
			cnt = 0;
			for (int n = 0; n < src.size();++n )
			{
				SceneHelper cur = src.get(n);
				for (int i = 0; i < cur.scene.getNumTextures();++i)
				{
					if (n != duplicates[n])
					{
						if (( flags & AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY) != 0)
//							copy(pip,cur.scene.mTextures[i]);
							pip[pip_index] = cur.scene.mTextures[i].copy();

						else continue;
					}
					else pip[pip_index] = cur.scene.mTextures[i];
					++pip_index;
				}

				offset[n] = cnt;
//				cnt = (unsigned int)(pip - dest->mTextures);
				cnt = pip_index;
			}
		}

		// generate the output material list + an offset table for all material indices
		if (numMaterials > 0)
		{ 
			Material[] pip = dest.mMaterials = new Material[numMaterials];
			int pip_index = 0;
			cnt = 0;
			for (int n = 0; n < src.size();++n )	{
				SceneHelper cur = src.get(n);
				for (int i = 0; i < cur.scene.getNumMaterials();++i)
				{
					if (n != duplicates[n])
					{
						if (( flags & AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY) != 0)
//							copy(pip,cur.scene.mMaterials[i]);
							pip[pip_index] = cur.scene.mMaterials[i].copy();

						else continue;
					}
					else pip[pip_index] = cur.scene.mMaterials[i];

					if (cur.scene.getNumTextures() != numTextures)		{
						// We need to update all texture indices of the mesh. So we need to search for
						// a material property called '$tex.file'
						Material _pip = pip[pip_index];
						for (int a = 0; a < _pip.getNumProperties();++a)
						{
							MaterialProperty prop = _pip.mProperties[a];
//							if (!strncmp(prop->mKey.data,"$tex.file",9))
							if("$tex.file".equals(prop.mKey))
							{
								// Check whether this texture is an embedded texture.
								// In this case the property looks like this: *<n>,
								// where n is the index of the texture.
//								String s = *((aiString*)prop->mData);
								if ('*' == (prop.mData[4] & 0xFF))	{
									// Offset the index and write it back ..
									final int idx = AssUtil.strtoul10(prop.mData, 5, null) + offset[n];
//									ASSIMP_itoa10(&s.data[1],sizeof(s.data)-1,idx);
									AssUtil.assimp_itoa10(prop.mData, 5, prop.mData.length - 5, idx);
								}
							}

							// Need to generate new, unique material names?
							else if ("$mat.name".equals(prop.mKey)/*!::strcmp( prop->mKey.data,"$mat.name" )*/ && (flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_MATNAMES) != 0)
							{
//								aiString* pcSrc = (aiString*) prop->mData; 
//								PrefixString(*pcSrc, (*cur).id, (*cur).idlen);
								String pcSrc = new String(prop.mData, 4, prop.mData.length - 4);
								String newStr = prefixString(pcSrc, cur.id);
								if(pcSrc != newStr){
									prop.mData = new byte[newStr.length() + 4];
									AssUtil.getBytes(newStr.length(), prop.mData, 0);
									newStr.getBytes(0, newStr.length(), prop.mData, 4);
								}
							}
						}
					}
//					++pip;
					++pip_index;
				}

				offset[n] = cnt;
//				cnt = (unsigned int)(pip - dest->mMaterials);
				cnt = pip_index;
			}
		}

		// generate the output mesh list + again an offset table for all mesh indices
		if (numMeshes > 0)
		{
			Mesh[] pip = dest.mMeshes = new Mesh[numMeshes];
			cnt = 0;
			int pip_index = 0;
			for (int n = 0; n < src.size();++n )
			{
				SceneHelper cur = src.get(n);
				for (int i = 0; i < cur.scene.getNumMeshes();++i)
				{
					if (n != duplicates[n])	{
						if ( (flags & AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY) != 0)
//							copy(pip, cur.scene.mMeshes[i]);
							pip[pip_index] = cur.scene.mMeshes[i].copy();

						else continue;
					}
					else pip[pip_index] = cur.scene.mMeshes[i];

					// update the material index of the mesh
					pip[pip_index].mMaterialIndex +=  offset[n];
//					++pip;
					++pip_index;
				}

				// reuse the offset array - store now the mesh offset in it
				offset[n] = cnt;
//				cnt = (unsigned int)(pip - dest->mMeshes);
				cnt = pip_index;
			}
		}

//		std::vector <NodeAttachmentInfo> nodes;
//		nodes.reserve(srcList.size());
		List<NodeAttachmentInfo> nodes = new ArrayList<NodeAttachmentInfo>(srcList.size());

		// ----------------------------------------------------------------------------
		// Now generate the output node graph. We need to make those
		// names in the graph that are referenced by anims or lights
		// or cameras unique. So we add a prefix to them ... $<rand>_
		// We could also use a counter, but using a random value allows us to
		// use just one prefix if we are joining multiple scene hierarchies recursively.
		// Chances are quite good we don't collide, so we try that ...
		// ----------------------------------------------------------------------------

		// Allocate space for light sources, cameras and animations
		Light[] ppLights = dest.mLights = (numLights > 0 ? new Light[numLights] : null);
		Camera[] ppCameras = dest.mCameras = (numCameras > 0 ? new Camera[numCameras] : null);
		Animation[] ppAnims = dest.mAnimations = (numAnimations > 0? new Animation[numAnimations] : null);

		for ( int n = src.size()-1; n >= 0 ;--n ) /* !!! important !!! */
		{
			SceneHelper cur = src.get(n);
			Node node;

			// To offset or not to offset, this is the question
			if (n != duplicates[n])
			{
				// Get full scenegraph copy
//				copy(node, cur.scene.mRootNode );
				node = cur.scene.mRootNode.copy();
				offsetNodeMeshIndices(node,offset[duplicates[n]]);

				if ((flags & AI_INT_MERGE_SCENE_DUPLICATES_DEEP_CPY) !=0)	{
					// (note:) they are already 'offseted' by offset[duplicates[n]] 
					offsetNodeMeshIndices(node,offset[n] - offset[duplicates[n]]);
				}
			}
			else // if (n == duplicates[n])
			{
				node = cur.scene.mRootNode;
				offsetNodeMeshIndices(node,offset[n]);
			}
			if (n > 0) // src[0] is the master node
				nodes.add(new NodeAttachmentInfo( node,srcList.get(n-1).attachToNode,n ));

			// add name prefixes?
			if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES)!=0) {

				// or the whole scenegraph
				if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY)!=0) {
					addNodePrefixesChecked(node,cur.id,src,n);
				}
				else addNodePrefixes(node,cur.id);

				// meshes
				for (int i = 0; i < cur.scene.getNumMeshes();++i)	{
					Mesh mesh = cur.scene.mMeshes[i]; 

					// rename all bones
					for (int a = 0; a < mesh.getNumBones();++a)	{
						if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY)!=0) {
							if (!findNameMatch(mesh.mBones[a].mName,src,n))
								continue;
						}
						mesh.mBones[a].mName = prefixString(mesh.mBones[a].mName,cur.id);
					}
				}
			}

			// --------------------------------------------------------------------
			// Copy light sources
			for (int i = 0; i < cur.scene.getNumLights();++i)
			{
				if (n != (int)duplicates[n]) // duplicate scene? 
				{
					ppLights[i] = cur.scene.mLights[i].copy();
				}
				else ppLights[i] = cur.scene.mLights[i];


				// Add name prefixes?
				if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES)!=0) {
					if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY)!=0) {
						if (!findNameMatch(ppLights[i].mName,src,n))
							continue;
					}

					ppLights[i].mName = prefixString(ppLights[i].mName,cur.id);
				}
			}

			// --------------------------------------------------------------------
			// Copy cameras
			for (int i = 0; i < cur.scene.getNumCameras();++i/*,++ppCameras*/)	{
				if (n != (int)duplicates[n]) // duplicate scene? 
				{
					ppCameras[i] = cur.scene.mCameras[i].copy();
				}
				else ppCameras[i] = cur.scene.mCameras[i];

				// Add name prefixes?
				if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES)!=0) {
					if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY) !=0){
						if (!findNameMatch(ppCameras[i].mName,src,n))
							continue;
					}

					ppCameras[i].mName = prefixString(ppCameras[i].mName,cur.id);
				}
			}

			// --------------------------------------------------------------------
			// Copy animations
			for (int i = 0; i < cur.scene.getNumAnimations();++i/*,++ppAnims*/)	{
				if (n != (int)duplicates[n]) // duplicate scene? 
				{
//					ppAnims[i] = copy(cur.scene.mAnimations[i], null);
					ppAnims[i] = cur.scene.mAnimations[i].copy();
				}
				else ppAnims[i] = cur.scene.mAnimations[i];

				// Add name prefixes?
				if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES)!=0) {
					if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY)!=0) {
						if (!findNameMatch(ppAnims[i].mName,src,n))
							continue;
					}

					ppAnims[i].mName = prefixString(ppAnims[i].mName,cur.id);

					// don't forget to update all node animation channels
					for (int a = 0; a < ppAnims[i].getNumChannels();++a) {
						if ((flags & AI_INT_MERGE_SCENE_GEN_UNIQUE_NAMES_IF_NECESSARY)!=0) {
							if (!findNameMatch(ppAnims[i].mChannels[a].mNodeName,src,n))
								continue;
						}

						ppAnims[i].mChannels[a].mNodeName = prefixString(ppAnims[i].mChannels[a].mNodeName,cur.id);
					}
				}
			}
		}

		// Now build the output graph
		attachToGraph ( master, nodes);
		dest.mRootNode = master.mRootNode;

		// Check whether we succeeded at building the output graph
		for (NodeAttachmentInfo it : nodes)
		{
			if (!it.resolved) {
				if ((flags & AI_INT_MERGE_SCENE_RESOLVE_CROSS_ATTACHMENTS)!=0) {
					// search for this attachment point in all other imported scenes, too.
					for (int n = 0; n < src.size();++n ) {
						if (n != it.src_idx) {
							attachToGraph(src.get(n).scene,nodes);
							if (it.resolved)
								break;
						}
					}
				}
				if (!it.resolved) {
//					DefaultLogger::get()->error(std::string("SceneCombiner: Failed to resolve attachment ") 
//						+ (*it).node->mName.data + " " + (*it).attachToNode->mName.data);
					DefaultLogger.error("SceneCombiner: Failed to resolve attachment " + it.node.mName + " " + it.attachToNode.mName);
				}
			}
		}

		// now delete all input scenes. Make sure duplicate scenes aren't
		// deleted more than one time
//		for ( unsigned int n = 0; n < src.size();++n )	{
//			if (n != duplicates[n]) // duplicate scene?
//				continue;
//
//			aiScene* deleteMe = src[n].scene;
//
//			// We need to delete the arrays before the destructor is called -
//			// we are reusing the array members
//			delete[] deleteMe->mMeshes;     deleteMe->mMeshes     = NULL;
//			delete[] deleteMe->mCameras;    deleteMe->mCameras    = NULL;
//			delete[] deleteMe->mLights;     deleteMe->mLights     = NULL;
//			delete[] deleteMe->mMaterials;  deleteMe->mMaterials  = NULL;
//			delete[] deleteMe->mAnimations; deleteMe->mAnimations = NULL;
//
//			deleteMe->mRootNode = NULL;
//
//			// Now we can safely delete the scene
//			delete deleteMe;
//		}

		// Check flags
		if (numMeshes == 0 || numMaterials == 0) {
			dest.mFlags |= Scene.AI_SCENE_FLAGS_INCOMPLETE;
		}

		// We're finished
		return dest;
	}


	// -------------------------------------------------------------------
	/** Merges two or more meshes
	 *
	 *  The meshes should have equal vertex formats. Only components
	 *  that are provided by ALL meshes will be present in the output mesh.
	 *  An exception is made for VColors - they are set to black. The 
	 *  meshes should have the same material indices, too. The output
	 *  material index is always the material index of the first mesh.
	 *
	 *  @param out Destination mesh. Must be empty.
	 *  @param flags Currently no parameters
	 *  @param begin First mesh to be processed
	 *  @param end Points to the mesh after the last mesh to be processed
	 */
	public static Mesh mergeMeshes(Mesh out, int flags, List<Mesh> meshes){
		if(meshes == null || meshes.isEmpty())
			return null;
		
		if(out == null)
			out = new Mesh();
		final boolean natived = AssimpConfig.MESH_USE_NATIVE_MEMORY;
		int begin = 0;
		Mesh _begin = meshes.get(begin);
		// Allocate the output mesh
//		Mesh* out = *_out = new aiMesh();
		out.mMaterialIndex = _begin.mMaterialIndex;

		// Find out how much output storage we'll need
		int numFaces = out.getNumFaces();
		int numBones = out.getNumBones();
//		for (std::vector<aiMesh*>::const_iterator it = begin; it != end;++it)	{
		for(Mesh it : meshes){
//			out->mNumVertices	+= (*it)->mNumVertices;
//			out->mNumFaces		+= (*it)->mNumFaces;
//			out->mNumBones		+= (*it)->mNumBones;
//
//			// combine primitive type flags
//			out->mPrimitiveTypes |= (*it)->mPrimitiveTypes;
			
			out.mNumVertices    += it.mNumVertices;
			numFaces    		+= it.getNumFaces();
			numBones            += it.getNumBones();
		}

		if (out.mNumVertices > 0) {
//			aiVector3D* pv2;
			FloatBuffer pv2;

			// copy vertex positions
			if (_begin.hasPositions())	{

				pv2 = out.mVertices = MemoryUtil.createFloatBuffer(out.mNumVertices * 3, natived);  //new aiVector3D[out->mNumVertices];
				for (Mesh it : meshes)	{
					if (it.mVertices != null)	{
//						::memcpy(pv2,(*it)->mVertices,(*it)->mNumVertices*sizeof(aiVector3D));
						pv2.put(it.mVertices);
						it.mVertices.flip();
					}
					else DefaultLogger.warn("JoinMeshes: Positions expected but input mesh contains no positions");
//					pv2 += (*it)->mNumVertices;
					pv2.position(it.mNumVertices * 3 + pv2.position());
				}
				pv2.flip();
			}
			// copy normals
			if (_begin.hasNormals())	{

				pv2 = out.mNormals = MemoryUtil.createFloatBuffer(out.mNumVertices * 3, natived);  //new aiVector3D[out->mNumVertices];
				for (Mesh it : meshes)	{
					if (it.mNormals != null)	{
//						::memcpy(pv2,(*it)->mNormals,(*it)->mNumVertices*sizeof(aiVector3D));
						pv2.put(it.mNormals);
						it.mNormals.flip();
					}
					else DefaultLogger.warn("JoinMeshes: Normals expected but input mesh contains no normals");
//					pv2 += (*it)->mNumVertices;
					pv2.position(it.mNumVertices * 3 + pv2.position());
				}
				pv2.flip();
			}
			// copy tangents and bitangents
			if (_begin.hasTangentsAndBitangents())	{

				pv2 = out.mTangents =MemoryUtil.createFloatBuffer(out.mNumVertices * 3, natived);  // new aiVector3D[out->mNumVertices];
				FloatBuffer pv2b = out.mBitangents =MemoryUtil.createFloatBuffer(out.mNumVertices * 3, natived);  // new aiVector3D[out->mNumVertices];

				for (Mesh it : meshes)	{
					if (it.mTangents != null)	{
//						::memcpy(pv2, (*it)->mTangents,	 (*it)->mNumVertices*sizeof(aiVector3D));
//						::memcpy(pv2b,(*it)->mBitangents,(*it)->mNumVertices*sizeof(aiVector3D));
						pv2.put(it.mTangents);
						it.mTangents.flip();
						
						pv2b.put(it.mBitangents);
						it.mBitangents.flip();
					}
					else DefaultLogger.warn("JoinMeshes: Tangents expected but input mesh contains no tangents");
//					pv2  += (*it)->mNumVertices;
//					pv2b += (*it)->mNumVertices;
					pv2.position(it.mNumVertices * 3 + pv2.position());
					pv2b.position(it.mNumVertices * 3 + pv2b.position());
				}
				pv2.flip();
				pv2b.flip();
			}
			// copy texture coordinates
			int n = 0;
			while (_begin.hasTextureCoords(n))	{
				out.mNumUVComponents[n] = _begin.mNumUVComponents[n];

				pv2 = out.mTextureCoords[n] = MemoryUtil.createFloatBuffer(out.mNumVertices * 3, natived); // new aiVector3D[out->mNumVertices];
				for (Mesh it : meshes)	{

					if (it.mTextureCoords[n] != null)	{
//						::memcpy(pv2,(*it)->mTextureCoords[n],(*it)->mNumVertices*sizeof(aiVector3D));
						pv2.put(it.mTextureCoords[n]);
						it.mTextureCoords[n].flip();
					}
					else DefaultLogger.warn("JoinMeshes: UVs expected but input mesh contains no UVs");
//					pv2 += it.mNumVertices;
					pv2.position(it.mNumVertices * 3 + pv2.position());
				}
				++n;
				pv2.flip();
			}
			// copy vertex colors
			n = 0;
			while (_begin.hasVertexColors(n))	{
				pv2 = out.mColors[n] = MemoryUtil.createFloatBuffer(out.mNumVertices * 4, natived); //new aiColor4D[out->mNumVertices];
				for (Mesh it : meshes)	{

					if (it.mColors[n] != null)	{
//						::memcpy(pv2,(*it)->mColors[n],(*it)->mNumVertices*sizeof(aiColor4D));
						pv2.put(it.mColors[n]);
						it.mColors[n].flip();
					}
					else DefaultLogger.warn("JoinMeshes: VCs expected but input mesh contains no VCs");
//					pv2 += (*it)->mNumVertices;
					pv2.position(it.mNumVertices * 4 + pv2.position());
				}
				++n;
				pv2.flip();
			}
		}

		if (numFaces > 0) // just for safety
		{
			// copy faces
			out.mFaces = new Face[numFaces];

			int ofs = 0;
			for (Mesh it : meshes)	{
				for (int m = 0; m < it.getNumFaces();++m/*,++pf2*/)	{
					Face face = it.mFaces[m];
					
//					pf2->mNumIndices = face.mNumIndices;
//					pf2->mIndices = face.mIndices;
					out.mFaces[m] = face.copy();

					if (ofs != 0)	{
						// add the offset to the vertex
						for (int q = 0; q < face.getNumIndices(); ++q)
//							face.mIndices[q] += ofs;	
							out.mFaces[m].set(q, face.get(q) + ofs);
					}
//					face.mIndices = NULL;
				}
				ofs += it.mNumVertices;
			}
		}

		// bones - as this is quite lengthy, I moved the code to a separate function
		if (numBones > 0)
			mergeBones(out,meshes.iterator());

		// delete all source meshes
//		for (std::vector<aiMesh*>::const_iterator it = begin; it != end;++it)
//			delete *it;
		
		return out;
	}


	// -------------------------------------------------------------------
	/** Merges two or more bones
	 *
	 *  @param out Mesh to receive the output bone list
	 *  @param flags Currently no parameters
	 *  @param begin First mesh to be processed
	 *  @param end Points to the mesh after the last mesh to be processed
	 */
	public static Mesh mergeBones(Mesh out,Iterator<Mesh> it){
		// find we need to build an unique list of all bones.
		// we work with hashes to make the comparisons MUCH faster,
		// at least if we have many bones.
		List<BoneWithHash> asBones = new ArrayList<BoneWithHash>();
		buildUniqueBoneList(asBones, it);
		
		if(out == null)
			out = new Mesh();
		// now create the output bones
//		out.mNumBones = 0;
		int numBones = 0;
		out.mBones = new Bone[asBones.size()];

		for (BoneWithHash bwh : asBones)	{
			// Allocate a bone and setup it's name
			Bone pc = out.mBones[numBones++] = new Bone();
			pc.mName = bwh.second ; //aiString( *((*it).second ));

//			std::vector< BoneSrcIndex >::const_iterator wend = (*it).pSrcBones.end();
//
//			// Loop through all bones to be joined for this bone
//			for (std::vector< BoneSrcIndex >::const_iterator wmit = (*it).pSrcBones.begin(); wmit != wend; ++wmit)	{
//				pc->mNumWeights += (*wmit).first->mNumWeights;
//
//				// NOTE: different offset matrices for bones with equal names
//				// are - at the moment - not handled correctly. 
//				if (wmit != (*it).pSrcBones.begin() && pc->mOffsetMatrix != (*wmit).first->mOffsetMatrix)	{
//					DefaultLogger::get()->warn("Bones with equal names but different offset matrices can't be joined at the moment");
//					continue;
//				}
//				pc->mOffsetMatrix = (*wmit).first->mOffsetMatrix;
//			}
			
			boolean first = true;
			for(BoneWithHash.BoneSrcIndex wmit : bwh.pSrcBones){
				// NOTE: different offset matrices for bones with equal names
				bwh.mNumWeights += wmit.first.getNumWeights();
				
				// NOTE: different offset matrices for bones with equal names
				// are - at the moment - not handled correctly.
				if(!first && !pc.mOffsetMatrix.equals(wmit.first.mOffsetMatrix)){
					DefaultLogger.warn("Bones with equal names but different offset matrices can't be joined at the moment");
					continue;
				}
				pc.mOffsetMatrix.load(wmit.first.mOffsetMatrix);
			}

			// Allocate the vertex weight array
			pc.mWeights = new VertexWeight[bwh.mNumWeights];

			// And copy the final weights - adjust the vertex IDs by the 
			// face index offset of the coresponding mesh.
			for (BoneWithHash.BoneSrcIndex wmit : bwh.pSrcBones)	{
				Bone pip = wmit.first;
				for (int mp = 0; mp < pip.getNumWeights();++mp/*,++avw*/)	{
					VertexWeight vfi = pip.mWeights[mp];
//					avw->mWeight = vfi.mWeight;
//					avw->mVertexId = vfi.mVertexId + (*wmit).second;
					pc.mWeights[mp].mWeight = vfi.mWeight;
					pc.mWeights[mp].mVertexId = vfi.mVertexId + wmit.second;
				}
			}
		}
		
		return out;
	}

	// -------------------------------------------------------------------
	/** Merges two or more materials
	 *
	 *  The materials should be complementary as much as possible. In case
	 *  of a property present in different materials, the first occurence
	 *  is used.
	 *
	 *  @param dest Destination material. Must be empty.
	 *  @param begin First material to be processed
	 *  @param end Points to the material after the last material to be processed
	 */
	public static Material MergeMaterials(Material dest, List<Material> materials){
		// Allocate the output material
		if(dest == null)
			dest = new Material();
		
		final Material out = dest;

		// Get the maximal number of properties
		int size = 0;
		for (Material it : materials) {
//			size += (*it)->mNumProperties;
			size += it.mNumProperties;
		}

		out.clear();
//		delete[] out->mProperties;

//		out.mNumAllocated = size;
		out.mNumProperties = 0;
		out.mProperties = new MaterialProperty[size];

		for (Material it : materials) {
			for(int i = 0; i < it.mNumProperties; ++i) {
				MaterialProperty sprop = it.mProperties[i];

				// Test if we already have a matching property 
				if(Material.aiGetMaterialProperty(out, sprop.mKey, sprop.mType.ordinal(), sprop.mIndex) == null) {
					// If not, we add it to the new material
					MaterialProperty prop = out.mProperties[out.mNumProperties] = new MaterialProperty();

//					prop->mDataLength = sprop->mDataLength;
//					prop->mData = new char[prop->mDataLength];
//					::memcpy(prop->mData, sprop->mData, prop->mDataLength);
					if(sprop.mData != null)
						prop.mData = Arrays.copyOf(sprop.mData, sprop.mData.length);

					prop.mIndex    = sprop.mIndex;
					prop.mSemantic = sprop.mSemantic;
					prop.mKey      = sprop.mKey;
					prop.mType	   = sprop.mType;

					out.mNumProperties++;
				}
			}
		}
		
		return out;
	}

	// -------------------------------------------------------------------
	/** Builds a list of uniquely named bones in a mesh list
	 *
	 *  @param asBones Receives the output list
	 *  @param it First mesh to be processed
	 *  @param end Last mesh to be processed
	 */
	public static void buildUniqueBoneList(List<BoneWithHash> asBones,Iterator<Mesh> it){
		int iOffset = 0;
		
		while (it.hasNext())	{
			Mesh mesh = it.next();
			for (int l = 0; l < mesh.getNumBones();++l)	{
				Bone p = mesh.mBones[l];
//				 itml = SuperFastHash(p->mName.data,(unsigned int)p->mName.length);  TODO
				int itml = p.mName.hashCode();

//				std::list<BoneWithHash>::iterator it2  = asBones.begin();
//				std::list<BoneWithHash>::iterator end2 = asBones.end();
//
//				for (;it2 != end2;++it2)	{
//					if ((*it2).first == itml)	{
//						(*it2).pSrcBones.push_back(BoneSrcIndex(p,iOffset));
//						break;
//					}
//				}
				Iterator<BoneWithHash> it2 = asBones.iterator();
				while(it2.hasNext()){
					BoneWithHash bwh = it2.next();
					if(bwh.first == itml){
						bwh.pSrcBones.add(new BoneWithHash.BoneSrcIndex(p, iOffset));
						break;
					}
				}
				
				if (!it2.hasNext())	{
					// need to begin a new bone entry
					asBones.add(new BoneWithHash());
					BoneWithHash btz = asBones.get(asBones.size() - 1);

					// setup members
					btz.first = itml;
					btz.second = p.mName;
					btz.pSrcBones.add(new BoneWithHash.BoneSrcIndex(p,iOffset));
				}
			}
			iOffset += mesh.mNumVertices;
		}
	}

	// -------------------------------------------------------------------
	/** Add a name prefix to all nodes in a scene.
	 *
	 *  @param Current node. This function is called recursively.
	 *  @param prefix Prefix to be added to all nodes
	 *  @param len STring length
	 */
	public static void addNodePrefixes(Node node, String prefix){
		node.mName = prefixString(node.mName,prefix);

		// Process all children recursively
		for (int i = 0; i < node.getNumChildren();++i)
			addNodePrefixes(node.mChildren[i],prefix);
	}

	// -------------------------------------------------------------------
	/** Add an offset to all mesh indices in a node graph
	 *
	 *  @param Current node. This function is called recursively.
	 *  @param offset Offset to be added to all mesh indices
	 */
	public static void offsetNodeMeshIndices (Node node, int offset){
		int numMeshes = node.getNumMeshes();
		int numChildren = node.getNumChildren();
		for (int i = 0; i < numMeshes;++i)
			node.mMeshes[i] += offset;

		for (int i = 0; i < numChildren;++i)
			offsetNodeMeshIndices(node.mChildren[i],offset);
	}

	// -------------------------------------------------------------------
	/** Attach a list of node graphs to well-defined nodes in a master
	 *  graph. This is a helper for MergeScenes()
	 *
	 *  @param master Master scene
	 *  @param srcList List of source scenes along with their attachment
	 *    points. If an attachment point is null (or does not exist in
	 *    the master graph), a scene is attached to the root of the master
	 *    graph (as an additional child node)
	 *  @duplicates List of duplicates. If elem[n] == n the scene is not
	 *    a duplicate. Otherwise elem[n] links scene n to its first occurence.
	 */
	public static void attachToGraph (Scene master, List<NodeAttachmentInfo> srcList){
		attachToGraph(master.mRootNode, srcList);
	}

	public static void attachToGraph (Node attach, List<NodeAttachmentInfo> srcList){
		int numChildren = attach.getNumChildren();
		int cnt;
		for (cnt = 0; cnt < numChildren;++cnt)
			attachToGraph(attach.mChildren[cnt],srcList);

		cnt = 0;
		for (NodeAttachmentInfo it : srcList)
		{
			if (it.attachToNode == attach && !it.resolved)
				++cnt;
		}

		if (cnt > 0)	{
			Node[] n = new Node[cnt+attach.getNumChildren()];
			if (attach.getNumChildren() > 0)	{
//				::memcpy(n,attach->mChildren,sizeof(void*)*attach->mNumChildren);
//				delete[] attach->mChildren;
				System.arraycopy(attach.mChildren, 0, n, 0, attach.getNumChildren());
			}
			attach.mChildren = n;

//			n += attach.mNumChildren;
//			attach.mNumChildren += cnt;
			int _n = attach.getNumChildren();

			for (int i = 0; i < srcList.size();++i)	{
				NodeAttachmentInfo att = srcList.get(i);
				if (att.attachToNode == attach && !att.resolved)	{
//					*n = att.node;
//					(**n).mParent = attach;
//					++n;
					
					n[_n] = att.node;
					n[_n].mParent = attach;
					_n++;

					// mark this attachment as resolved
					att.resolved = true;
				}
			}
		}
	}

	// -------------------------------------------------------------------
	/** Get a flat copy of a scene<p>
	 *
	 *  Only the first hierarchy layer is copied. All pointer members of
	 *  aiScene are shared by source and destination scene.  If the
	 *    pointer doesn't point to null when the function is called, the
	 *    existing scene is cleared and refilled.
	 *  
	 *  @param src Source scene - remains unmodified.
	 *  @param dest Receives a pointer to the destination scene
	 */
	public static Scene copySceneFlat(Scene source, Scene dest){
		if(dest == null)
			dest = new Scene();
		
		dest.mAnimations = source.mAnimations;
		dest.mCameras    = source.mCameras;
		dest.mFlags      = source.mFlags;
		dest.mLights     = source.mLights;
		dest.mMaterials  = source.mMaterials;
		dest.mMeshes     = source.mMeshes;
		dest.mRootNode   = source.mRootNode;
		dest.mTextures   = source.mTextures;
		
		return dest;
	}


	// -------------------------------------------------------------------
	// Same as AddNodePrefixes, but with an additional check
	private static void addNodePrefixesChecked(Node node, String prefix, List<SceneHelper> input, int cur){
		final int hash = node.mName.hashCode(); //SuperFastHash(node->mName.data,node->mName.length);

		// Check whether we find a positive match in one of the given sets
		for (int i = 0; i < input.size(); ++i) {
			IntSet hash_set = input.get(i).hashes;
			if (cur != i && hash_set.contains(hash)) {
				node.mName = prefixString(node.mName,prefix);
				break;
			}
		}

		// Process all children recursively
		int numChildren = node.getNumChildren();
		for (int i = 0; i < numChildren;++i)
			addNodePrefixesChecked(node.mChildren[i],prefix,input,cur);
	}

	// -------------------------------------------------------------------
	// Add node identifiers to a hashing set
	private static void addNodeHashes(Node node, IntSet hashes){
		// Add node name to hashing set if it is non-empty - empty nodes are allowed 
		// and they can't have any anims assigned so its absolutely safe to duplicate them.
		if (node.mName.length() > 0) {
//			hashes.add( SuperFastHash(node->mName.data,node->mName.length) );
			hashes.add(node.mName.hashCode());
		}

		// Process all children recursively
		for (int i = 0; i < node.getNumChildren();++i)
			addNodeHashes(node.mChildren[i],hashes);
	}


	// -------------------------------------------------------------------
	// Search for duplicate names
	private static boolean findNameMatch(String name, List<SceneHelper> input, int cur){
		int hash = name.hashCode();  //SuperFastHash(node->mName.data,node->mName.length);

		// Check whether we find a positive match in one of the given sets
		for (int i = 0; i < input.size(); ++i) {

			IntSet hash_set = input.get(i).hashes;
			if (cur != i && hash_set.contains(hash)) {
				return true;
			}
		}
		return false;
	}
}
