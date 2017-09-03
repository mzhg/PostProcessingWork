package assimp.common;


/** Validates the whole ASSIMP scene data structure for correctness.
 *  ImportErrorException is thrown of the scene is corrupt.*/
public class ValidateDSProcess extends BaseProcess{

	private Scene mScene;
	
	@Override
	public boolean isActive(int pFlags) {
		return (pFlags & PostProcessSteps.aiProcess_ValidateDataStructure) != 0;
	}

	@Override
	public void execute(Scene pScene) {
		this.mScene = pScene;
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.debug("ValidateDataStructureProcess begin");
		
		// validate the node graph of the scene
		validate(pScene.mRootNode);
		
		// validate all meshes
		if (pScene.getNumMeshes() > 0) {
			doValidation(pScene.mMeshes,"mMeshes","mNumMeshes");
		}
		else if ((mScene.mFlags & Scene.AI_SCENE_FLAGS_INCOMPLETE) == 0)	{
			reportError("aiScene::mNumMeshes is 0. At least one mesh must be there");
		}
		else if (pScene.mMeshes != null)	{
			reportError("aiScene::mMeshes is non-null although there are no meshes");
		}
		
		// validate all animations
		if (pScene.getNumAnimations() > 0) {
			doValidation(pScene.mAnimations,"mAnimations","mNumAnimations");
		}
		else if (pScene.mAnimations != null)	{
			reportError("aiScene::mAnimations is non-null although there are no animations");
		}

		// validate all cameras
		if (pScene.getNumCameras() > 0) {
			doValidationWithNameCheck(pScene.mCameras, "mCameras","mNumCameras");
		}
		else if (pScene.mCameras != null)	{
			reportError("aiScene::mCameras is non-null although there are no cameras");
		}

		// validate all lights
		if (pScene.getNumLights() > 0) {
			doValidationWithNameCheck(pScene.mLights,"mLights","mNumLights");
		}
		else if (pScene.mLights != null)	{
			reportError("aiScene::mLights is non-null although there are no lights");
		}

		// validate all textures
		if (pScene.getNumTextures() > 0) {
			doValidation(pScene.mTextures, "mTextures","mNumTextures");
		}
		else if (pScene.mTextures != null)	{
			reportError("aiScene::mTextures is non-null although there are no textures");
		}
		
		// validate all materials
		if (pScene.getNumMaterials() > 0) {
			doValidation(pScene.mMaterials, "mMaterials","mNumMaterials");
		}
//	#if 0
//		// NOTE: ScenePreprocessor generates a default material if none is there
//		else if (!(mScene.mFlags & AI_SCENE_FLAGS_INCOMPLETE))	{
//			ReportError("aiScene::mNumMaterials is 0. At least one material must be there");
//		}
//	#endif
		else if (pScene.mMaterials != null)	{
			reportError("aiScene::mMaterials is non-null although there are no materials");
		}

//		if (!has)ReportError("The aiScene data structure is empty");
		DefaultLogger.debug("ValidateDataStructureProcess end");
		
	}
	
	private final void _validate(Object obj){
		if(obj instanceof Animation){
			validate((Animation)obj);
		}else if(obj instanceof Mesh){
			validate((Mesh)obj);
		}else if(obj instanceof Material){
			validate((Material)obj);
		}else if(obj instanceof Texture){
			validate((Texture)obj);
		}
	}
	
	private final void doValidation(Object[] parray, String firstName, String secondName) {
		if (parray == null){
			reportError("aiScene::%s is null",firstName);
		}
		for (int i = 0; i < parray.length;++i)
		{
			if (parray[i] == null){
				reportError("aiScene::%s[%i] is null (aiScene::%s is %i)", firstName,i,secondName, parray.length);
			}else{
				_validate(parray[i]);
			}
		}
	}
	
	private final void doValidationWithNameCheck(NamedObject[] array, String firstName, String secondName){
		// validate all entries
		doValidationEx(array,firstName,secondName);
		
		int size = array.length;
		for (int i = 0; i < size;++i)
		{
			int res = hasNameMatch(array[i].getName(),mScene.mRootNode);
			if (res == 0)	{
				reportError("aiScene::%s[%i] has no corresponding node in the scene graph (%s)",
					firstName,i,array[i].getName());
			}
			else if (1 != res)	{
				reportError("aiScene::%s[%i]: there are more than one nodes with %s as name",
					firstName,i,array[i].getName());
			}
		}
	}
	
	private final void doValidationEx(NamedObject[] parray, String firstName, String secondName){
		if (parray == null)	{
			reportError("aiScene::%s is null", firstName);
		}
		int size = parray.length;
		for (int i = 0; i < size;++i)
		{
			if (parray[i] == null)
			{
				reportError("aiScene::%s[%i] is null (aiScene::%s is %i)",
					firstName,i,secondName,size);
			}else{
				_validate(parray[i]);
			}

			// check whether there are duplicate names
			for (int a = i+1; a < size;++a)
			{
//				if (parray[i].mName == parray[a].mName)
				if(parray[i].getName().equals(parray[a].getName()))
				{
					reportError("aiScene::%s[%i] has the same name as aiScene::%s[%i]",firstName, i,secondName, a);
				}
			}
		}
	}

	// -------------------------------------------------------------------
	/** Report a validation error. This will throw an exception,
	 *  control won't return.
	 * @param msg Format string for sprintf().*/
	protected void reportError(String msg, Object ...args){
		String errMsg = msg != null ? String.format(msg, args) : "";
		throw new DeadlyImportError("Validation failed: " + errMsg);
	}


	// -------------------------------------------------------------------
	/** Report a validation warning. This won't throw an exception,
	 *  control will return to the callera.
	 * @param msg Format string for sprintf().*/
	protected void reportWarning(String msg, Object ...args){
		if(DefaultLogger.LOG_OUT){
			String errMsg = msg != null ? String.format(msg, args) : "";
			DefaultLogger.warn("Validation warning: " + errMsg);
		}
	}
	
	static int hasNameMatch(String in, Node node)
	{
		int result = (node.mName.equals(in) ? 1 : 0 );
		int numChildren = node.getNumChildren();
		for (int i = 0; i < numChildren;++i)	{
			result += hasNameMatch(in,node.mChildren[i]);
		}
		return result;
	}


	// -------------------------------------------------------------------
	/** Validates a mesh
	 * @param pMesh Input mesh*/
	protected void validate(Mesh pMesh){
		// validate the material index of the mesh
		if (mScene.getNumMaterials() > 0 && pMesh.mMaterialIndex >= mScene.getNumMaterials())
		{
			reportError("aiMesh::mMaterialIndex is invalid (value: %i maximum: %i)",
				pMesh.mMaterialIndex,mScene.getNumMaterials() -1);
		}

		validate(pMesh.mName);

		int nunFaces = pMesh.getNumFaces();
		for (int i = 0; i < nunFaces; ++i)
		{
			Face face = pMesh.mFaces[i];

			if (pMesh.mPrimitiveTypes != 0)
			{
				switch (face.getNumIndices())
				{
				case 0:
					reportError("aiMesh::mFaces[%i].mNumIndices is 0",i);
				case 1:
					if (0 == (pMesh.mPrimitiveTypes & Mesh.aiPrimitiveType_POINT))
					{
						reportError("aiMesh::mFaces[%i] is a POINT but aiMesh::mPrimtiveTypes does not report the POINT flag",i);
					}
					break;
				case 2:
					if (0 == (pMesh.mPrimitiveTypes & Mesh.aiPrimitiveType_LINE))
					{
						reportError("aiMesh::mFaces[%i] is a LINE but aiMesh::mPrimtiveTypes does not report the LINE flag",i);
					}
					break;
				case 3:
					if (0 == (pMesh.mPrimitiveTypes & Mesh.aiPrimitiveType_TRIANGLE))
					{
						reportError("aiMesh::mFaces[%i] is a TRIANGLE but aiMesh::mPrimtiveTypes does not report the TRIANGLE flag",i);
					}
					break;
				default:
					if (0 == (pMesh.mPrimitiveTypes & Mesh.aiPrimitiveType_POLYGON))
					{
						reportError("aiMesh::mFaces[%i] is a POLYGON but aiMesh::mPrimtiveTypes does not report the POLYGON flag",i);
					}
					break;
				};
			}

//			if (face.mIndices)
//				ReportError("aiMesh::mFaces[%i].mIndices is null",i);
		}

		// positions must always be there ...
		if (pMesh.mNumVertices == 0 || (pMesh.mVertices == null && mScene.mFlags == 0))	{
			reportError("The mesh contains no vertices");
		}

		if (pMesh.mNumVertices /*> Mesh.AI_MAX_VERTICES*/ < 0) {
			reportError("Mesh has too many vertices: %u, but the limit is %u",pMesh.mNumVertices,Mesh.AI_MAX_VERTICES);
		}
		if (pMesh.getNumFaces() /*> Mesh.AI_MAX_FACES*/ < 0) {
			reportError("Mesh has too many faces: %u, but the limit is %u",pMesh.getNumFaces(),Mesh.AI_MAX_FACES);
		}

		// if tangents are there there must also be bitangent vectors ...
		if ((pMesh.mTangents != null) != (pMesh.mBitangents != null))	{
			reportError("If there are tangents, bitangent vectors must be present as well");
		}
		
		// faces, too
		if (pMesh.getNumFaces() == 0 || (/*!pMesh.mFaces && */mScene.mFlags == 0))	{
			reportError("Mesh contains no faces");
		}

		// now check whether the face indexing layout is correct:
		// unique vertices, pseudo-indexed.
//		BooleanList abRefList = new BooleanArrayList();
//		abRefList.size(pMesh.mNumVertices);
		boolean[] abRefList = new boolean[pMesh.mNumVertices];
		for (int i = 0; i < pMesh.getNumFaces();++i)
		{
			Face face = pMesh.mFaces[i];
			if (face.getNumIndices()/* > AI_MAX_FACE_INDICES*/ < 0) {
				reportError("Face %u has too many faces: %u, but the limit is %d",i,face.getNumIndices(),Mesh.AI_MAX_FACE_INDICES);
			}

			for (int a = 0; a < face.getNumIndices();++a)
			{
				if (face.get(a) >= pMesh.mNumVertices)	{
					reportError("aiMesh::mFaces[%i]::mIndices[%i] is out of range",i,a);
				}
				// the MSB flag is temporarily used by the extra verbose
				// mode to tell us that the JoinVerticesProcess might have 
				// been executed already.
				if ( (this.mScene.mFlags & Scene.AI_SCENE_FLAGS_NON_VERBOSE_FORMAT ) == 0 && abRefList[face.get(a)])
				{
					reportError("aiMesh::mVertices[%i] is referenced twice - second time by aiMesh::mFaces[%i]::mIndices[%i]",face.get(a),i,a);
				}
				abRefList[face.get(a)] = true;
			}
		}

		// check whether there are vertices that aren't referenced by a face
		boolean b = false;
		for (int i = 0; i < pMesh.mNumVertices;++i)	{
			if (!abRefList[i])b = true;
		}
		abRefList = null;
		if (b)reportWarning("There are unreferenced vertices");

		// texture channel 2 may not be set if channel 1 is zero ...
		{
			int i = 0;
			for (;i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++i)
			{
				if (!pMesh.hasTextureCoords(i))break;
			}
			for (;i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++i)
				if (pMesh.hasTextureCoords(i))
				{
					reportError("Texture coordinate channel %i exists although the previous channel was null.",i);
				}
		}
		// the same for the vertex colors
		{
			int i = 0;
			for (;i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS;++i)
			{
				if (!pMesh.hasVertexColors(i))break;
			}
			for (;i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS;++i)
				if (pMesh.hasVertexColors(i))
				{
					reportError("Vertex color channel %i is exists although the previous channel was null.",i);
				}
		}


		// now validate all bones
		if (pMesh.getNumBones() > 0)
		{
			if (pMesh.mBones == null)
			{
				reportError("aiMesh::mBones is null (aiMesh::mNumBones is %i)", pMesh.getNumBones());
			}
//			boost::scoped_array<float> afSum(null);
			float[] afSum = null;
			if (pMesh.mNumVertices > 0)
			{
//				afSum.reset(new float[pMesh.mNumVertices]);
				afSum = new float[pMesh.mNumVertices];
//				for (int i = 0; i < pMesh.mNumVertices;++i)
//					afSum[i] = 0.0f;
			}

			// check whether there are duplicate bone names
			for (int i = 0; i < pMesh.getNumBones();++i)
			{
				Bone bone = pMesh.mBones[i];
				if (bone.getNumWeights() /*> Mesh.AI_MAX_BONE_WEIGHTS*/ < 0) {
					reportError("Bone %u has too many weights: %u, but the limit is %u",i,bone.getNumWeights(),Mesh.AI_MAX_BONE_WEIGHTS);
				}

				if (pMesh.mBones[i] == null)
				{
					reportError("aiMesh::mBones[%i] is null (aiMesh::mNumBones is %i)",
						i,pMesh.getNumBones());
				}
				validate(pMesh,pMesh.mBones[i],afSum);

				for (int a = i+1; a < pMesh.getNumBones();++a)
				{
					if (pMesh.mBones[i].mName == pMesh.mBones[a].mName)
					{
						reportError("aiMesh::mBones[%i] has the same name as aiMesh::mBones[%i]",i,a);
					}
				}
			}
			// check whether all bone weights for a vertex sum to 1.0 ...
			for (int i = 0; i < pMesh.mNumVertices;++i)
			{
				if (afSum[i] != 0 && (afSum[i] <= 0.94 || afSum[i] >= 1.05))	{
					reportWarning("aiMesh::mVertices[%i]: bone weight sum != 1.0 (sum is %f)",i,afSum[i]);
				}
			}
		}
		else if (pMesh.mBones != null)
		{
			reportError("aiMesh::mBones is non-null although there are no bones");
		}
	}

	// -------------------------------------------------------------------
	/** Validates a bone
	 * @param pMesh Input mesh
	 * @param pBone Input bone*/
	protected void validate(Mesh pMesh, Bone pBone,float[] afSum){
		validate(pBone.mName);

	   	if (pBone.getNumWeights() == 0)	{
			reportError("aiBone::mNumWeights is zero");
		}

		// check whether all vertices affected by this bone are valid
		for (int i = 0; i < pBone.getNumWeights();++i)
		{
			if (pBone.mWeights[i].mVertexId >= pMesh.mNumVertices)	{
				reportError("aiBone::mWeights[%i].mVertexId is out of range",i);
			}
			else if (pBone.mWeights[i].mWeight == 0 || pBone.mWeights[i].mWeight > 1.0f)	{
				reportWarning("aiBone::mWeights[%i].mWeight has an invalid value",i);
			}
			afSum[pBone.mWeights[i].mVertexId] += pBone.mWeights[i].mWeight;
		}
	}

	// -------------------------------------------------------------------
	/** Validates an animation
	 * @param pAnimation Input animation*/
	protected void validate(Animation pAnimation){
		validate(pAnimation.mName);

		// validate all materials
		if (pAnimation.getNumChannels() > 0)	
		{
			if (pAnimation.mChannels == null)	{
				reportError("aiAnimation::mChannels is null (aiAnimation::mNumChannels is %i)",pAnimation.getNumChannels());
			}
			
			for (int i = 0; i < pAnimation.getNumChannels();++i)
			{
				if (pAnimation.mChannels[i] == null)
				{
					reportError("aiAnimation::mChannels[%i] is null (aiAnimation::mNumChannels is %i)",
						i, pAnimation.getNumChannels());
				}
				validate(pAnimation, pAnimation.mChannels[i]);
			}
		}
		else reportError("aiAnimation::mNumChannels is 0. At least one node animation channel must be there.");
	}

	// -------------------------------------------------------------------
	/** Validates a material
	 * @param pMaterial Input material*/
	protected void validate(Material pMaterial){
		// check whether there are material keys that are obviously not legal
		for (int i = 0; i < pMaterial.mNumProperties;++i)
		{
			MaterialProperty prop = pMaterial.mProperties[i];
			if (prop == null)	{
				reportError("aiMaterial::mProperties[%i] is null (aiMaterial::mNumProperties is %i)",i,pMaterial.mNumProperties);
			}
			if (prop.mData == null)	{
				reportError("aiMaterial::mProperties[%i].mDataLength or aiMaterial::mProperties[%i].mData is 0",i,i);
			}
			// check all predefined types
			if (PropertyTypeInfo.aiPTI_String == prop.mType)	{
				// FIX: strings are now stored in a less expensive way, but we can't use the
				// validation routine for 'normal' aiStrings
				int len;
				if (prop.getDataLength() < 5 || prop.getDataLength() < 4 + (len=AssUtil.getInt(prop.mData, 0) /* *reinterpret_cast<uint32_t*>(prop.mData)*/) + 1)	{
					reportError("aiMaterial::mProperties[%i].mDataLength is too small to contain a string (%i, needed: %i)",
						i,prop.getDataLength(),/*sizeof(aiString)*/ 1028); // TODO
				}
				// NO need this for Java Programming.
//				if(prop.mData[prop.mDataLength-1]) { 
//					ReportError("Missing null-terminator in string material property");
//				}
			//	Validate((const aiString*)prop.mData);
			}
			else if (PropertyTypeInfo.aiPTI_Float == prop.mType)	{
				if (prop.getDataLength() < /*sizeof(float)*/ 4)	{
					reportError("aiMaterial::mProperties[%i].mDataLength is too small to contain a float (%i, needed: %i)",
						i,prop.getDataLength(),/*sizeof(float)*/ 4);
				}
			}
			else if (PropertyTypeInfo.aiPTI_Integer == prop.mType)	{
				if (prop.getDataLength() < /*sizeof(int)*/4)	{
					reportError("aiMaterial::mProperties[%i].mDataLength is too small to contain an integer (%i, needed: %i)",
						i,prop.getDataLength(),/*sizeof(int)*/4);
				}
			}
			// TODO: check whether there is a key with an unknown name ...
		}

		// make some more specific tests 
		float fTemp;
		int iShading = Material.aiGetMaterialInteger(pMaterial, "$mat.shadingm",0,0, -1);
		if (iShading != -1/*AI_SUCCESS == aiGetMaterialInteger( pMaterial,AI_MATKEY_SHADING_MODEL,&iShading)*/)	{
			switch (ShadingMode.values()[iShading] /*(aiShadingMode)iShading*/)
			{
			case aiShadingMode_Blinn:
			case aiShadingMode_CookTorrance:
			case aiShadingMode_Phong:

				fTemp = Material.aiGetMaterialFloat(pMaterial, "$mat.shininess",0,0, -1f);
				if (fTemp == -1f /*AI_SUCCESS != aiGetMaterialFloat(pMaterial,AI_MATKEY_SHININESS,&fTemp)*/)	{
					reportWarning("A specular shading model is specified but there is no AI_MATKEY_SHININESS key");
				}
				
				fTemp = Material.aiGetMaterialFloat(pMaterial, "$mat.shinpercent",0,0, -1f);
				if (/*AI_SUCCESS == aiGetMaterialFloat(pMaterial,AI_MATKEY_SHININESS_STRENGTH,&fTemp) && !*/fTemp == 0.0f)	{
					reportWarning("A specular shading model is specified but the value of the AI_MATKEY_SHININESS_STRENGTH key is 0.0");
				}
				break;
			default: ;
			};
		}

		fTemp = Material.aiGetMaterialFloat(pMaterial, "$mat.opacity",0,0, -1f);
		if (fTemp != -1f/*AI_SUCCESS == aiGetMaterialFloat( pMaterial,AI_MATKEY_OPACITY,&fTemp)*/ && (fTemp == 0f || fTemp > 1.01f))	{
			reportWarning("Invalid opacity value (must be 0 < opacity < 1.0)");
		}

		// Check whether there are invalid texture keys
		// TODO: that's a relict of the past, where texture type and index were baked
		// into the material string ... we could do that in one single pass.
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_DIFFUSE);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_SPECULAR);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_AMBIENT);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_EMISSIVE);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_OPACITY);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_SHININESS);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_HEIGHT);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_NORMALS);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_DISPLACEMENT);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_LIGHTMAP);
		searchForInvalidTextures(pMaterial,TextureType.aiTextureType_REFLECTION);
	}

	// -------------------------------------------------------------------
	/** Search the material data structure for invalid or corrupt
	 *  texture keys.
	 * @param pMaterial Input material
	 * @param type Type of the texture*/
	protected void searchForInvalidTextures(Material pMaterial,TextureType type){
		final String szType = ProcessHelper.textureTypeToString(type);

		// ****************************************************************************
		// Search all keys of the material ...
		// textures must be specified with ascending indices 
		// (e.g. diffuse #2 may not be specified if diffuse #1 is not there ...)
		// ****************************************************************************

		int iNumIndices = 0;
		int iIndex = -1;
		for (int i = 0; i < pMaterial.mNumProperties;++i)
		{
			MaterialProperty prop = pMaterial.mProperties[i];
			if ("$tex.file".equals(prop.mKey)/*!::strcmp(prop.mKey.data,"$tex.file")*/ && prop.mSemantic == type.ordinal())	{
				iIndex = Math.max(iIndex, (int) prop.mIndex);
				++iNumIndices;

				if (PropertyTypeInfo.aiPTI_String != prop.mType)
					reportError("Material property %s is expected to be a string",prop.mKey);
			}
		}
		if (iIndex +1 != iNumIndices)	{
			reportError("%s #%i is set, but there are only %i %s textures",
				szType,iIndex,iNumIndices,szType);
		}
		if (iNumIndices == 0)return;
		
//		std::vector<aiTextureMapping> mappings(iNumIndices);
		int[] mappings = new int[iNumIndices];

		// Now check whether all UV indices are valid ...
		boolean bNoSpecified = true;
		for (int i = 0; i < pMaterial.mNumProperties;++i)
		{
			MaterialProperty prop = pMaterial.mProperties[i];
			if (prop.mSemantic != type.ordinal())continue;

			if (prop.mIndex >= iNumIndices)
			{
				reportError("Found texture property with index %i, although there are only %i textures of type %s",
					prop.mIndex, iNumIndices, szType);
			}
				
			if ("$tex.mapping".equals(prop.mKey) /*!::strcmp(prop.mKey.data,"$tex.mapping")*/)	{
				if (PropertyTypeInfo.aiPTI_Integer != prop.mType || prop.getDataLength() < 4/*sizeof(aiTextureMapping)*/)
				{
					reportError("Material property %s%i is expected to be an integer (size is %i)",prop.mKey,prop.mIndex,prop.getDataLength());
				}
//				mappings[prop.mIndex] = *((aiTextureMapping*)prop.mData);
				mappings[prop.mIndex] = AssUtil.getInt(prop.mData, 0);
			}
			else if ("$tex.uvtrafo".equals(prop.mKey)/*!::strcmp(prop.mKey.data,"$tex.uvtrafo")*/)	{
				if (PropertyTypeInfo.aiPTI_Float != prop.mType || prop.getDataLength() < UVTransform.SIZE)
				{
					reportError("Material property %s%i is expected to be 5 floats large (size is %i)",
						prop.mKey,prop.mIndex, prop.getDataLength());
				}
//				mappings[prop.mIndex] = *((aiTextureMapping*)prop.mData);
				mappings[prop.mIndex] = AssUtil.getInt(prop.mData, 0);
			}
			else if ("$tex.uvwsrc".equals(prop.mKey)/*!::strcmp(prop.mKey.data,"$tex.uvwsrc")*/) {
				if (PropertyTypeInfo.aiPTI_Integer != prop.mType || /*sizeof(int)*/4 > prop.getDataLength())
				{
					reportError("Material property %s%i is expected to be an integer (size is %i)",
						prop.mKey,prop.mIndex,prop.getDataLength());
				}
				bNoSpecified = false;

				// Ignore UV indices for texture channels that are not there ...

				// Get the value
//				iIndex = *((unsigned int*)prop.mData);
				iIndex = AssUtil.getInt(prop.mData, 0);

				// Check whether there is a mesh using this material
				// which has not enough UV channels ...
				for (int a = 0; a < mScene.getNumMeshes();++a)
				{
					Mesh mesh = mScene.mMeshes[a];
					if(mesh.mMaterialIndex == i)
					{
						int iChannels = 0;
						while (mesh.hasTextureCoords(iChannels))++iChannels;
						if (iIndex >= iChannels)
						{
							reportWarning("Invalid UV index: %i (key %s). Mesh %i has only %i UV channels",
								iIndex,prop.mKey,a,iChannels);
						}
					}
				}
			}
		}
		if (bNoSpecified)
		{
			// Assume that all textures are using the first UV channel
			for (int a = 0; a < mScene.getNumMeshes();++a)
			{
				Mesh mesh = mScene.mMeshes[a];
				if(mesh.mMaterialIndex == iIndex && mappings[0] == TextureMapping.aiTextureMapping_UV.ordinal())
				{
					if (mesh.mTextureCoords[0] == null)
					{
						// This is a special case ... it could be that the
						// original mesh format intended the use of a special
						// mapping here.
						reportWarning("UV-mapped texture, but there are no UV coords");
					}
				}
			}
		}
	}

	// -------------------------------------------------------------------
	/** Validates a texture
	 * @param pTexture Input texture*/
	protected void validate(Texture pTexture){
		// the data section may NEVER be null
		if (pTexture.pcData == null)	{
			reportError("aiTexture::pcData is null");
		}
		if (pTexture.mHeight != 0)
		{
			if (pTexture.mWidth == 0)
				reportError("aiTexture::mWidth is zero (aiTexture::mHeight is %i, uncompressed texture)",pTexture.mHeight);
		}
		else 
		{
			if (pTexture.mWidth == 0) {
				reportError("aiTexture::mWidth is zero (compressed texture)");
			}
			// NO need for Java programming.
//			if ('\0' != pTexture.achFormatHint[3]) {
//				reportWarning("aiTexture::achFormatHint must be zero-terminated");
//			}
			else if ('.'  == pTexture.achFormatHint.charAt(0))	{
				reportWarning("aiTexture::achFormatHint should contain a file extension without a leading dot (format hint: %s).",pTexture.achFormatHint);
			}
		}

		char[] sz = pTexture.achFormatHint.toCharArray();
	 	if ((sz[0] >= 'A' && sz[0] <= 'Z') ||
			(sz[1] >= 'A' && sz[1] <= 'Z') ||
			(sz[2] >= 'A' && sz[2] <= 'Z') ||
			(sz[3] >= 'A' && sz[3] <= 'Z'))	{
			reportError("aiTexture::achFormatHint contains non-lowercase letters");
		}
	}
	
	// -------------------------------------------------------------------
	/** Validates a light source
	 * @param pLight Input light
	 */
	protected void validate(Light pLight){
		if (pLight.mType == LightSourceType.aiLightSource_UNDEFINED)
			reportWarning("aiLight::mType is aiLightSource_UNDEFINED");

		if (pLight.mAttenuationConstant == 0 &&
			pLight.mAttenuationLinear==0   && 
			pLight.mAttenuationQuadratic == 0)	{
			reportWarning("aiLight::mAttenuationXXX - all are zero");
		}

		if (pLight.mAngleInnerCone > pLight.mAngleOuterCone)
			reportError("aiLight::mAngleInnerCone is larger than aiLight::mAngleOuterCone");

		if (pLight.mColorDiffuse.isZero() && pLight.mColorAmbient.isZero()
			&& pLight.mColorSpecular.isZero())
		{
			reportWarning("aiLight::mColorXXX - all are black and won't have any influence");
		}
	}
	
	// -------------------------------------------------------------------
	/** Validates a camera
	 * @param pCamera Input camera*/
	protected void validate(Camera pCamera){
		if (pCamera.mClipPlaneFar <= pCamera.mClipPlaneNear)
			reportError("aiCamera::mClipPlaneFar must be >= aiCamera::mClipPlaneNear");

		// FIX: there are many 3ds files with invalid FOVs. No reason to
		// reject them at all ... a warning is appropriate.
		if (pCamera.mHorizontalFOV == 0 || pCamera.mHorizontalFOV >= (float)Math.PI)
			reportWarning("%f is not a valid value for aiCamera::mHorizontalFOV",pCamera.mHorizontalFOV);
	}

	// -------------------------------------------------------------------
	/** Validates a bone animation channel
	 * @param pAnimation Animation channel.
	 * @param pNodeAnim Input bone animation */
	protected void validate(Animation pAnimation, NodeAnim pNodeAnim){
		validate(pNodeAnim.mNodeName);

		final int numPositionKeys = pNodeAnim.getNumPositionKeys();
		if (numPositionKeys == 0 && pNodeAnim.mScalingKeys == null && pNodeAnim.getNumRotationKeys() == 0)
			reportError("Empty node animation channel");

		// otherwise check whether one of the keys exceeds the total duration of the animation
		if (numPositionKeys > 0)
		{
			if (pNodeAnim.mPositionKeys == null)
			{
				this.reportError("aiNodeAnim::mPositionKeys is null"/*,pNodeAnim.mNumPositionKeys*/);
			}
			double dLast = -10e10;
			for (int i = 0; i < numPositionKeys;++i)
			{
				// ScenePreprocessor will compute the duration if still the default value
				// (Aramis) Add small epsilon, comparison tended to fail if max_time == duration,
				//  seems to be due the compilers register usage/width.
				if (pAnimation.mDuration > 0. && pNodeAnim.mPositionKeys[i].mTime > pAnimation.mDuration+0.001)
				{
					reportError("aiNodeAnim::mPositionKeys[%i].mTime (%.5f) is larger than aiAnimation::mDuration (which is %.5f)",i,
						(float)pNodeAnim.mPositionKeys[i].mTime,
						(float)pAnimation.mDuration);
				}
				if (i > 0 && pNodeAnim.mPositionKeys[i].mTime <= dLast)
				{
					reportWarning("aiNodeAnim::mPositionKeys[%i].mTime (%.5f) is smaller than aiAnimation::mPositionKeys[%i] (which is %.5f)",i,
						(float)pNodeAnim.mPositionKeys[i].mTime,
						i-1, (float)dLast);
				}
				dLast = pNodeAnim.mPositionKeys[i].mTime;
			}
		}
		// rotation keys
		if (pNodeAnim.getNumRotationKeys() > 0)
		{
			if (pNodeAnim.mRotationKeys == null)
			{
				reportError("aiNodeAnim::mRotationKeys is null"/*,pNodeAnim.mNumRotationKeys*/);
			}
			double dLast = -10e10;
			for (int i = 0; i < pNodeAnim.getNumRotationKeys();++i)
			{
				if (pAnimation.mDuration > 0. && pNodeAnim.mRotationKeys[i].mTime > pAnimation.mDuration+0.001)
				{
					reportError("aiNodeAnim::mRotationKeys[%i].mTime (%.5f) is larger than aiAnimation::mDuration (which is %.5f)",i,
						(float)pNodeAnim.mRotationKeys[i].mTime,
						(float)pAnimation.mDuration);
				}
				if (i > 0 && pNodeAnim.mRotationKeys[i].mTime <= dLast)
				{
					reportWarning("aiNodeAnim::mRotationKeys[%i].mTime (%.5f) is smaller than aiAnimation::mRotationKeys[%i] (which is %.5f)",i,
						(float)pNodeAnim.mRotationKeys[i].mTime,
						i-1, (float)dLast);
				}
				dLast = pNodeAnim.mRotationKeys[i].mTime;
			}
		}
		// scaling keys
		if (pNodeAnim.getNumScalingKeys() > 0)
		{
			if (pNodeAnim.mScalingKeys == null)	{
				reportError("aiNodeAnim::mScalingKeys is null"/*,pNodeAnim.mNumScalingKeys*/);
			}
			double dLast = -10e10;
			for (int i = 0; i < pNodeAnim.getNumScalingKeys();++i)
			{
				if (pAnimation.mDuration > 0. && pNodeAnim.mScalingKeys[i].mTime > pAnimation.mDuration+0.001)
				{
					reportError("aiNodeAnim::mScalingKeys[%i].mTime (%.5f) is larger than aiAnimation::mDuration (which is %.5f)",i,
						(float)pNodeAnim.mScalingKeys[i].mTime,
						(float)pAnimation.mDuration);
				}
				if (i > 0 && pNodeAnim.mScalingKeys[i].mTime <= dLast)
				{
					reportWarning("aiNodeAnim::mScalingKeys[%i].mTime (%.5f) is smaller than aiAnimation::mScalingKeys[%i] (which is %.5f)",i,
						(float)pNodeAnim.mScalingKeys[i].mTime,
						i-1, (float)dLast);
				}
				dLast = pNodeAnim.mScalingKeys[i].mTime;
			}
		}

		if (pNodeAnim.getNumScalingKeys() == 0 && pNodeAnim.getNumRotationKeys() == 0 && pNodeAnim.getNumPositionKeys() == 0)
		{
			reportError("A node animation channel must have at least one subtrack");
		}
	}

	// -------------------------------------------------------------------
	/** Validates a node and all of its subnodes
	 * @param Node Input node*/
	protected void validate(Node pNode){
		if (pNode == null) reportError("A node of the scenegraph is null");
		if (pNode != mScene.mRootNode && pNode.mParent==null)
			this.reportError("A node has no valid parent (aiNode::mParent is null)");

		this.validate(pNode.mName);

		// validate all meshes
		if (pNode.getNumMeshes() > 0)
		{
			if (pNode.mMeshes == null)
			{
				reportError("aiNode::mMeshes is null"/*,pNode.mNumMeshes*/);
			}
//			std::vector<bool> abHadMesh;
//			abHadMesh.resize(mScene.mNumMeshes,false);
			boolean[] abHadMesh = new boolean[mScene.getNumMeshes()];
			for (int i = 0; i < pNode.getNumMeshes();++i)
			{
				if (pNode.mMeshes[i] >= mScene.getNumMeshes())
				{
					reportError("aiNode::mMeshes[%i] is out of range (maximum is %i)",
						pNode.mMeshes[i],mScene.getNumMeshes()-1);
				}
				if (abHadMesh[pNode.mMeshes[i]])
				{
					reportError("aiNode::mMeshes[%i] is already referenced by this node (value: %i)",
						i,pNode.mMeshes[i]);
				}
				abHadMesh[pNode.mMeshes[i]] = true;
			}
		}
		if (pNode.getNumChildren() > 0)
		{
			if (pNode.mChildren == null)	{
				reportError("aiNode::mChildren is null"/*,pNode.mNumChildren*/);
			}
			for (int i = 0; i < pNode.getNumChildren();++i)	{
				validate(pNode.mChildren[i]);
			}
		}
	}

	// -------------------------------------------------------------------
	/** Validates a string
	 * @param pString Input string*/
	protected void validate(String pString){
		if (pString.length() > AssimpConfig.MAXLEN)
		{
			reportError("aiString::length is too large (%i, maximum is %i)",pString.length(),AssimpConfig.MAXLEN);
		}
		/*const char* sz = pString.data;
		while (true)
		{
			if ('\0' == *sz)
			{
				if (pString.length != (unsigned int)(sz-pString.data))
					ReportError("aiString::data is invalid: the terminal zero is at a wrong offset");
				break;
			}
			else if (sz >= &pString.data[MAXLEN])
				ReportError("aiString::data is invalid. There is no terminal character");
			++sz;
		}*/
	}

}
