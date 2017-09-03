package assimp.common;

import java.util.Arrays;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/** 
 * Data structure for a material<p>
*
*  Material data is stored using a key-value structure. A single key-value
*  pair is called a 'material property'. C++ users should use the provided
*  member functions of aiMaterial to process material properties, C users
*  have to stick with the aiMaterialGetXXX family of unbound functions.
*  The library defines a set of standard keys (AI_MATKEY_XXX).
*/
public class Material implements Copyable<Material>{
	
	/** Name for default materials (2nd is used if meshes have UV coords) */
	public static final String AI_DEFAULT_MATERIAL_NAME = "DefaultMaterial";
	
	// ---------------------------------------------------------------------------
	public static final String AI_MATKEY_NAME  = "?mat.name",
	             AI_MATKEY_TWOSIDED ="$mat.twosided",
	             AI_MATKEY_SHADING_MODEL ="$mat.shadingm",
	             AI_MATKEY_ENABLE_WIREFRAME ="$mat.wireframe",
	             AI_MATKEY_BLEND_FUNC ="$mat.blend",
	             AI_MATKEY_OPACITY ="$mat.opacity",
	             AI_MATKEY_BUMPSCALING ="$mat.bumpscaling",
	             AI_MATKEY_SHININESS ="$mat.shininess",
	             AI_MATKEY_REFLECTIVITY ="$mat.reflectivity",
	             AI_MATKEY_SHININESS_STRENGTH ="$mat.shinpercent",
	             AI_MATKEY_REFRACTI ="$mat.refracti",
	             AI_MATKEY_COLOR_DIFFUSE ="$clr.diffuse",
	             AI_MATKEY_COLOR_AMBIENT ="$clr.ambient",
	             AI_MATKEY_COLOR_SPECULAR ="$clr.specular",
	             AI_MATKEY_COLOR_EMISSIVE ="$clr.emissive",
	             AI_MATKEY_COLOR_TRANSPARENT ="$clr.transparent",
	             AI_MATKEY_COLOR_REFLECTIVE ="$clr.reflective",
	             AI_MATKEY_GLOBAL_BACKGROUND_IMAGE ="?bg.global";

	// ---------------------------------------------------------------------------
	// Pure key names for all texture-related properties
	//! @cond MATS_DOC_FULL
	public static final String _AI_MATKEY_TEXTURE_BASE 	    = "$tex.file";
	public static final String _AI_MATKEY_UVWSRC_BASE	    = "$tex.uvwsrc";
	public static final String _AI_MATKEY_TEXOP_BASE	    = "$tex.op";
	public static final String _AI_MATKEY_MAPPING_BASE		= "$tex.mapping";
	public static final String _AI_MATKEY_TEXBLEND_BASE		= "$tex.blend";
	public static final String _AI_MATKEY_MAPPINGMODE_U_BASE= "$tex.mapmodeu";
	public static final String _AI_MATKEY_MAPPINGMODE_V_BASE= "$tex.mapmodev";
	public static final String _AI_MATKEY_TEXMAP_AXIS_BASE	= "$tex.mapaxis";
	public static final String _AI_MATKEY_UVTRANSFORM_BASE	= "$tex.uvtrafo";
	public static final String _AI_MATKEY_TEXFLAGS_BASE		= "$tex.flags";

	/** List of all material properties loaded. */
	public MaterialProperty[] mProperties;
	
	/** Number of properties in the data base */
    public int mNumProperties;

    public int getNumProperties(){ return mNumProperties/*mProperties != null ? mProperties.length : 0*/; }
    
    @Override
    public Material copy() {
    	Material mat = new Material();
    	mat.mNumProperties = mNumProperties;
    	mat.mProperties = AssUtil.copyOf(mProperties);
    	return mat;
    }
 // -------------------------------------------------------------------
    /* Retrieve an array of Type values with a specific key 
     *  from the material
     *
     * @param pKey Key to search for. One of the AI_MATKEY_XXX constants.
     * @param type .. set by AI_MATKEY_XXX
     * @param idx .. set by AI_MATKEY_XXX
     * @param pOut Pointer to a buffer to receive the result. 
     * @param pMax Specifies the size of the given buffer, in Type's.
     * Receives the number of values (not bytes!) read. 
     * NULL is a valid value for this parameter.
     */
//    template <typename Type>
//    aiReturn Get(String pKey,int type,
//		int idx, Type* pOut, int* pMax) const;
//
//	aiReturn Get(String pKey,int type,
//		int idx, int* pOut, int* pMax) const;
//
//	aiReturn Get(String pKey,int type,
//		int idx, float* pOut, int* pMax) const;

    // -------------------------------------------------------------------
    /* Retrieve a Type value with a specific key 
     *  from the material
	 *
	 * @param pKey Key to search for. One of the AI_MATKEY_XXX constants.
    * @param type Specifies the type of the texture to be retrieved (
    *    e.g. diffuse, specular, height map ...)
    * @param idx Index of the texture to be retrieved.
	 * @param pOut Reference to receive the output value
	 */
//	template <typename Type>
//	aiReturn Get(String pKey,int type,
//		int idx,Type& pOut) const;
//
//
	public int getInt(String pKey,int type,int idx){
		int[] a = new int[1];
		aiGetMaterialIntegerArray(this,pKey,type,idx,a,1);
		return a[0];
	}

	public float getFloat(String pKey,int type,int idx){
		float[] a = new float[1];
		aiGetMaterialFloatArray(this, pKey, type, idx, a, 1);
		return a[0];
	}

	public String getString(String pKey,int type,int idx){
		return aiGetMaterialString(this, pKey, type, idx);
	}

	public void get(String pKey,int type, int idx, Vector4f pOut){
		aiGetMaterialColor(this, pKey, type, idx, pOut);
	}

	public void get(String pKey,int type, int idx, Vector3f pOut){
		float[] a = new float[3];
		aiGetMaterialFloatArray(this, pKey, type, idx, a, 3);
		pOut.load(a, 0);
	}

	public void get(String pKey,int type, int idx, UVTransform pOut){
		aiGetMaterialUVTransform(this, pKey, type, idx, pOut);
	}

	// -------------------------------------------------------------------
	/** Get the number of textures for a particular texture type.
	 *  @param type Texture type to check for
	 *  @return Number of textures for this type.
	 *  @note A texture can be easily queried using #GetTexture() */
	public int getTextureCount(TextureType type){
		return aiGetMaterialTextureCount(this, type);
	}

	// -------------------------------------------------------------------
	/** Helper function to get all parameters pertaining to a 
	 *  particular texture slot from a material.<p>
	*
	*  This function is provided just for convenience, you could also
	*  read the single material properties manually.
	*  @param type Specifies the type of the texture to be retrieved (
	*    e.g. diffuse, specular, height map ...)
	*  @param index Index of the texture to be retrieved. The function fails
	*    if there is no texture of that type with this index. 
	*    #GetTextureCount() can be used to determine the number of textures
	*    per texture type.
	*  @param path Receives the path to the texture.
	*	 NULL is a valid value.
   *  @param mapping The texture mapping.
   *		NULL is allowed as value.
	*  @param uvindex Receives the UV index of the texture. 
	*    NULL is a valid value.
	*  @param blend Receives the blend factor for the texture
	*	 NULL is a valid value.
	*  @param op Receives the texture operation to be performed between
	*	 this texture and the previous texture. NULL is allowed as value.
	*  @param mapmode Receives the mapping modes to be used for the texture.
	*    The parameter may be NULL but if it is a valid pointer it MUST
	*    point to an array of 3 aiTextureMapMode's (one for each
	*    axis: UVW order (=XYZ)). 
	*/
	// -------------------------------------------------------------------
	public void getTexture(TextureType type,
		int  index,
		String[] path,
		TextureMapping[] mapping	/*= NULL*/,
		int[] uvindex		/*= NULL*/,
		float[] blend				   /*= NULL*/,
		TextureOp[] op				/*= NULL*/,
		TextureMapMode[] mapmode	/*= NULL*/){
		aiGetMaterialTexture(this, type, index, path, mapping, uvindex, blend, op, mapmode, null);
	}


	// Setters


	// ------------------------------------------------------------------------------
	/** Add a property with a given key and type info to the material
	 *  structure 
	 *
	 *  @param pInput Pointer to input data
	 *  @param refCopy Weather just copy the data reference.
	 *  @param pKey Key/Usage of the property (AI_MATKEY_XXX)
	 *  @param type Set by the AI_MATKEY_XXX macro
	 *  @param index Set by the AI_MATKEY_XXX macro
	 *  @param pType Type information hint */
	public void addBinaryProperty (byte[] pInput, boolean refCopy, String pKey, int type,int index, PropertyTypeInfo pType){
		// first search the list whether there is already an entry with this key
		int iOutIndex = -1;
		for (int i = 0; i < getNumProperties();++i)	{
			MaterialProperty prop = mProperties[i];

			if (prop == null ||
				(prop != null/* just for safety */ 
//				&& !strcmp( prop.mKey.data, pKey )
				&& prop.mKey.equals(pKey)
				&& prop.mSemantic == type 
				&& prop.mIndex == index)){

//				delete mProperties[i];
				iOutIndex = i;
			}
		}

		// Allocate a new material property
		MaterialProperty pcNew = new MaterialProperty();

		// .. and fill it
		pcNew.mType = pType;
		pcNew.mSemantic = type;
		pcNew.mIndex = index;

		if(!refCopy){
//			pcNew.mData = new byte[pSizeInBytes];
//			System.arraycopy(pInput, 0, pcNew.mData, 0, pSizeInBytes);
			pcNew.mData = Arrays.copyOf(pInput, pInput.length);
		}else{
			pcNew.mData = pInput;
		}

//		pcNew.mKey.length = ::strlen(pKey);
//		ai_assert ( MAXLEN > pcNew.mKey.length);
//		strcpy( pcNew.mKey.data, pKey );
		pcNew.mKey = pKey;

		if (-1 != iOutIndex)	{
			mProperties[iOutIndex] = pcNew;
			return;
		}

		// resize the array ... double the storage allocated
//		if (mNumProperties == mNumAllocated)	{
//			unsigned int iOld = mNumAllocated;
//			mNumAllocated *= 2;
//
//			aiMaterialProperty** ppTemp;
//			try {
//			ppTemp = new aiMaterialProperty*[mNumAllocated];
//			} catch (std::bad_alloc&) {
//				return AI_OUTOFMEMORY;
//			}
//
//			// just copy all items over; then replace the old array
//			memcpy (ppTemp,mProperties,iOld * sizeof(void*));
//
//			delete[] mProperties;
//			mProperties = ppTemp;
//		}
		mProperties = Arrays.copyOf(mProperties, mProperties.length * 2);
		// push back ...
		mProperties[mNumProperties++] = pcNew;
	}

	// ------------------------------------------------------------------------------
	/** Add a string property with a given key and type info to the 
	 *  material structure 
	 *
	 *  @param pInput Input string
	 *  @param pKey Key/Usage of the property (AI_MATKEY_XXX)
	 *  @param type Set by the AI_MATKEY_XXX macro
	 *  @param index Set by the AI_MATKEY_XXX macro */
	@SuppressWarnings("deprecation")
	public void addProperty (String pInput, String pKey,int type  /*= 0*/,int index /*= 0*/){
		// TODO
		int length = pInput.length();
		byte[] input = new byte[4 + length];
		int offset = AssUtil.getBytes(length, input, 0);
		pInput.getBytes(0, length, input, offset);
		addBinaryProperty(input, true, pKey, type, index, PropertyTypeInfo.aiPTI_String);
	}
	
	/** Add a string property with a given key and type info to the 
	 *  material structure 
	 *
	 *  @param pInput Input string
	 *  @param pKey Key/Usage of the property (AI_MATKEY_XXX)
	 *  @param type Set by the AI_MATKEY_XXX macro
	 *  @param index Set by the AI_MATKEY_XXX macro */
	@SuppressWarnings("deprecation")
	public void addProperty (String pInput, String pKey,Enum<?> type  /*= 0*/,int index /*= 0*/){
		// TODO
		int length = pInput.length();
		byte[] input = new byte[4 + length];
		int offset = AssUtil.getBytes(length, input, 0);
		pInput.getBytes(0, length, input, offset);
		addBinaryProperty(input, true, pKey, type.ordinal(), index, PropertyTypeInfo.aiPTI_String);
	}

	// ------------------------------------------------------------------------------
	/* Add a property with a given key to the material structure 
	 *  @param pInput Pointer to the input data
	 *  @param pNumValues Number of values in the array
	 *  @param pKey Key/Usage of the property (AI_MATKEY_XXX)
	 *  @param type Set by the AI_MATKEY_XXX macro
	 *  @param index Set by the AI_MATKEY_XXX macro  */
//	template<class TYPE>
//	aiReturn AddProperty (const TYPE* pInput,
//		int pNumValues,
//		String pKey,
//		int type  = 0,
//		int index = 0);

	public void addProperty (Vector3f[] pInput, String pKey, int type /*= 0*/, int index/* = 0*/){
		byte[] a = new byte[pInput.length * AssUtil.SIZE_OF_VEC3];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}
	
	public void addProperty (Vector3f pInput, String pKey, int type /*= 0*/, int index/* = 0*/){
		byte[] a = new byte[AssUtil.SIZE_OF_VEC3];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}
	
	public void addProperty (Vector4f[] pInput, String pKey, int type /*= 0*/, int index/* = 0*/){
		byte[] a = new byte[pInput.length * AssUtil.SIZE_OF_VEC4];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}
	
	public void addProperty (Vector4f pInput, String pKey, int type /*= 0*/, int index/* = 0*/){
		byte[] a = new byte[AssUtil.SIZE_OF_VEC4];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}
	
	public void addProperty (float pInput, String pKey, int type/*  = 0*/, int index/* = 0*/){
		byte[] a = new byte[AssUtil.SIZE_OF_FLOAT];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}
	
	public void addProperty (float[] pInput, String pKey, int type/*  = 0*/, int index/* = 0*/){
		byte[] a = new byte[pInput.length * AssUtil.SIZE_OF_FLOAT];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}

	public void addProperty (int pInput, String pKey, int type/*  = 0*/, int index/* = 0*/){
		byte[] a = new byte[AssUtil.SIZE_OF_INT];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Integer);
	}
	
	public void addProperty (int[] pInput, String pKey, int type/*  = 0*/, int index/* = 0*/){
		byte[] a = new byte[pInput.length * AssUtil.SIZE_OF_INT];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Integer);
	}

//	aiReturn AddProperty (const aiUVTransform* pInput,
//		int pNumValues,
//		String pKey,
//		int type  = 0,
//		int index = 0);
	
	public void addProperty (UVTransform[] pInput, String pKey, int type /*= 0*/, int index/* = 0*/){
		byte[] a = new byte[pInput.length * UVTransform.SIZE];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}
	
	public void addProperty (UVTransform pInput, String pKey, int type /*= 0*/, int index/* = 0*/){
		byte[] a = new byte[UVTransform.SIZE];
		AssUtil.getBytes(pInput, a, 0);
		addBinaryProperty(a, true, pKey, type, index, PropertyTypeInfo.aiPTI_Float);
	}

	// ------------------------------------------------------------------------------
	/** Remove a given key from the list.
	 *
	 *  The function fails if the key isn't found
	 *  @param pKey Key to be deleted */
	public void removeProperty (String pKey, int type /*= 0*/, int index/* = 0*/){
		for (int i = 0; i < mNumProperties;++i) {
			MaterialProperty prop = mProperties[i];

			if (prop!=null 
//				&& !strcmp( prop.mKey.data, pKey ) 
				&& prop.mKey.equals(pKey)
				&&prop.mSemantic == type && prop.mIndex == index)
			{
				// Delete this entry
//				delete mProperties[i];

				// collapse the array behind --.
				--mNumProperties;
				for (int a = i; a < mNumProperties;++a)	{
					mProperties[a] = mProperties[a+1];
				}
			}
		}
	}

	// ------------------------------------------------------------------------------
	/** Removes all properties from the material.<p>
	 *
	 *  The data array remains allocated so adding new properties is quite fast.  */
	public void clear(){
		Arrays.fill(mProperties, null);
		mNumProperties = 0;
	}

	// ------------------------------------------------------------------------------
	/** Copy the property list of a material
	 *  <p><b>NOTE: </b> This method need test.
	 *  @param pcSrc Source material
	 *  @param pcDest Destination material
	 */
	public static void copyPropertyList(Material pcSrc, Material pcDest){
		int iOldNum = pcDest.mNumProperties;
		int arrayLength = AssUtil.length(pcDest.mProperties) + AssUtil.length(pcSrc.mProperties);
		pcDest.mNumProperties += pcSrc.mNumProperties;

		MaterialProperty[] pcOld = pcDest.mProperties;
		pcDest.mProperties = new MaterialProperty[arrayLength];

		if (iOldNum != 0 && pcOld != null)	{
//			for (int i = 0; i < iOldNum;++i) {
//				pcDest.mProperties[i] = pcOld[i];
//			}
			
			System.arraycopy(pcOld, 0, pcDest.mProperties, 0, iOldNum);

//			delete[] pcOld;
		}
		for (int i = iOldNum; i< pcDest.mNumProperties;++i)	{
			MaterialProperty propSrc = pcSrc.mProperties[i];

			// search whether we have already a property with this name . if yes, overwrite it
			MaterialProperty prop;
			for (int q = 0; q < iOldNum;++q) {
				prop = pcDest.mProperties[q];
				if (prop != null/* just for safety */ 
//					&& prop.mKey == propSrc.mKey
					&& prop.mKey.equals(propSrc.mKey)
					&& prop.mSemantic == propSrc.mSemantic
					&& prop.mIndex == propSrc.mIndex)	{
//					delete prop;

					// collapse the whole array ...
//					memmove(&pcDest.mProperties[q],&pcDest.mProperties[q+1],i-q);
					System.arraycopy(pcDest.mProperties, q+1, pcDest.mProperties, q, i-q);
					
					i--;
					pcDest.mNumProperties--;
				}
			}

			// Allocate the output property and copy the source property
			prop = pcDest.mProperties[i] = new MaterialProperty();
			prop.mKey = propSrc.mKey;
			prop.mType = propSrc.mType;
			prop.mSemantic = propSrc.mSemantic;
			prop.mIndex = propSrc.mIndex;

//			prop.mData = new char[propSrc.mDataLength];
//			memcpy(prop.mData,propSrc.mData,prop.mDataLength);
			prop.mData = Arrays.copyOf(propSrc.mData, propSrc.mData.length);
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	/** Get a specific property from a material */
	static MaterialProperty aiGetMaterialProperty(Material pMat, String pKey, int type, int index)
	{
//		ai_assert (pMat != NULL);
//		ai_assert (pKey != NULL);
//		ai_assert (pPropOut != NULL);

		/*  Just search for a property with exactly this name ..
		 *  could be improved by hashing, but it's possibly 
		 *  no worth the effort (we're bound to C structures,
		 *  thus std::map or derivates are not applicable. */
		for (int i = 0; i < pMat.mNumProperties;++i) {
			MaterialProperty prop = pMat.mProperties[i];

			if (prop != null/* just for safety ... */
				&& prop.mKey.equals(pKey ) 
				&& (AssimpConfig.UINT_MAX == type  || prop.mSemantic == type) /* UINT_MAX is a wildcard, but this is undocumented :-) */ 
				&& (AssimpConfig.UINT_MAX == index || prop.mIndex == index))
			{
//				*pPropOut = pMat.mProperties[i];
				return prop;
			}
		}
//		*pPropOut = NULL;
		return null;
	}
	
	/** 
	 * Retrieve a single float property with a specific key from the material. If the value couldn't find that associated with the
	 * given key, the <i>defaltValue</i> will returned. 
	 * 
	 * <p>
	 *
	 *
	 * Pass one of the AI_MATKEY_XXX constants for the last three parameters (the
	 * example reads the #AI_MATKEY_SHININESS_STRENGTH property of the first diffuse texture)
	 * <pre>
	 * float specStrength = aiGetMaterialFloat(mat, AI_MATKEY_SHININESS_STRENGTH);
	 * </pre>
	 *
	 * @param pMat Pointer to the input material. May not be null
	 * @param pKey Key to search for. One of the AI_MATKEY_XXX constants.
	 * @param pOut Receives the output float.
	 * @param type (see the code sample above)
	 * @param index (see the code sample above)
	 * @param defaultValue the default return value.
	 * @return Specifies whether the key has been found. If not, the <i>defaultValue</i> will return.*/
	// ---------------------------------------------------------------------------
	static float aiGetMaterialFloat(Material pMat,String pKey, int type, int index, float defaultValue)
	{
		float[] f = new float[1];
		int count = aiGetMaterialFloatArray(pMat,pKey,type,index,f,1);
		return count > 0 ? f[0] : defaultValue;
	}

	// ------------------------------------------------------------------------------------------------
	/** Get an array of floating-point values from the material. Return the length of the array. */
	static int aiGetMaterialFloatArray(Material pMat, String pKey, int type, int index, float[] pOut,int pMax)
	{
//		ai_assert (pOut != NULL);
//		ai_assert (pMat != NULL);

		MaterialProperty prop = aiGetMaterialProperty(pMat,pKey,type,index/*, (const aiMaterialProperty**) &prop*/);
		if (prop == null) {
			throw new NullPointerException("aiGetMaterialProperty return null");
		}

		// data is given in floats, simply copy it
		int iWrite = 0;
		if( PropertyTypeInfo.aiPTI_Float == prop.mType || PropertyTypeInfo.aiPTI_Buffer == prop.mType)	{
			iWrite = prop.getDataLength() / /*sizeof(float)*/4;
			iWrite = Math.min(pMax,iWrite); ;
			for (int a = 0; a < iWrite;++a)	{
//				pOut[a] = static_cast<float> ( reinterpret_cast<float*>(prop.mData)[a] );
				pOut[a] = AssUtil.getFloat(prop.mData, a *4);
			}
			
			pMax = iWrite;
		}
		// data is given in ints, convert to float
		else if( PropertyTypeInfo.aiPTI_Integer == prop.mType)	{
			iWrite = prop.getDataLength() / /*sizeof(int32_t)*/ 4;
			iWrite = Math.min(pMax,iWrite); ;
			for (int a = 0; a < iWrite;++a)	{
//				pOut[a] = static_cast<float> ( reinterpret_cast<int32_t*>(prop.mData)[a] );
				pOut[a] = AssUtil.getInt(prop.mData, a * 4);
			}
			pMax = iWrite;
		}
		// a string ... read floats separated by spaces
		else {
			iWrite = pMax;
			// strings are zero-terminated with a 32 bit length prefix, so this is safe
//			String cur =  prop.mData+4;
			int cur = 4;
//			ai_assert(prop.mDataLength>=5 && !prop.mData[prop.mDataLength-1]);
//			NO need check zero-terminated character for Java programming.
//			if(!(prop.getDataLength() >= 5 && prop.mData[prop.getDataLength() - 1] == 0))
//				throw new AssertionError();
			
			int[] _cur = {cur};
			for (int a = 0; ;++a) {
//				cur = fast_atoreal_move<float>(cur,pOut[a]); TODO 
//				cur = AssUtil.extractFloat(prop.mData, cur, pOut, a);
				pOut[a] = (float) AssUtil.fast_atoreal_move(prop.mData, cur, _cur, false);
				cur = _cur[0];
				if(a==iWrite-1) {
					break;
				}
//				if(!IsSpace(*cur)) {
//					DefaultLogger::get().error("Material property" + std::string(pKey) + 
//						" is a string; failed to parse a float array out of it.");
//					return AI_FAILURE;
//				}
				
				if(!Character.isSpaceChar((char) (prop.mData[cur] & 0xFF))){
					throw new RuntimeException("Material property" + pKey + 
						" is a string; failed to parse a float array out of it.");
				}else{
					// TODO The C++ source doesn't contain this line.
					cur++;
				}
			}

			pMax = iWrite;
		}
		return pMax;

	}
	
	/**
	 * Retrieve an integer property with a specific key from a material
	 *
	 * See the sample for aiGetMaterialFloat for more information.*/
	// ---------------------------------------------------------------------------
	static int aiGetMaterialInteger(Material pMat, String pKey, int type, int index, int defaultValue)
	{
		int[] a = new int[1];
		int count = aiGetMaterialIntegerArray(pMat,pKey,type,index,a,1);
		return count > 0 ? a[0] : defaultValue;
	}
	
	/**
	 * Retrieve an integer property with a specific key from a material
	 *
	 * See the sample for aiGetMaterialFloat for more information.*/
	// ---------------------------------------------------------------------------
	static int aiGetMaterialInteger(Material pMat, String pKey, int type, int index)
	{
		return aiGetMaterialInteger(pMat, pKey, type, index, -1);
	}

	// ------------------------------------------------------------------------------------------------
	// Get an array if integers from the material
	static int aiGetMaterialIntegerArray(Material pMat, String pKey, int type, int index, int[] pOut, int pMax)
	{
		MaterialProperty prop = aiGetMaterialProperty(pMat,pKey,type,index/*,(const aiMaterialProperty**) &prop*/);
		if (prop == null) {
			throw new NullPointerException("aiGetMaterialProperty return null");
		}

		// data is given in ints, simply copy it
		int iWrite = 0;
		if( PropertyTypeInfo.aiPTI_Integer == prop.mType || PropertyTypeInfo.aiPTI_Buffer == prop.mType)	{
			iWrite = prop.getDataLength() / /*sizeof(int32_t)*/ 4;
			iWrite = Math.min(pMax,iWrite);
			for (int a = 0; a < iWrite;++a) {
//				pOut[a] = static_cast<int>(reinterpret_cast<int32_t*>(prop.mData)[a]);
				pOut[a] = AssUtil.getInt(prop.mData, a * 4);
			}
			pMax = iWrite;
		}
		// data is given in floats convert to int 
		else if( PropertyTypeInfo.aiPTI_Float == prop.mType)	{
			iWrite = prop.getDataLength() / /*sizeof(float)*/ 4;
			iWrite = Math.min(pMax,iWrite);
			for (int a = 0; a < iWrite;++a) {
//				pOut[a] = static_cast<int>(reinterpret_cast<float*>(prop.mData)[a]);
				pOut[a] = (int) AssUtil.getFloat(prop.mData, a * 4);
			}
			pMax = iWrite;
		}
		// it is a string ... no way to read something out of this
		else	{
			iWrite = pMax;
			// strings are zero-terminated with a 32 bit length prefix, so this is safe
//			String cur =  prop.mData+4;
			int cur = 4;
//			ai_assert(prop.mDataLength>=5 && !prop.mData[prop.mDataLength-1]);
			int[] _cur = {cur};
			for (int a = 0; ;++a) {	
//				pOut[a] = strtol10(cur,&cur);
				pOut[a] = (int)AssUtil.fast_atoreal_move(prop.mData, cur, _cur, false);
				cur = _cur[0];
				if(a==iWrite-1) {
					break;
				}
//				if(!IsSpace(*cur)) {
//					DefaultLogger::get().error("Material property" + std::string(pKey) + 
//						" is a string; failed to parse an integer array out of it.");
//					return AI_FAILURE;
//				}
				
				if(!Character.isSpaceChar((char) (prop.mData[cur] & 0xFF))){
					throw new RuntimeException("Material property" + pKey + 
						" is a string; failed to parse a float array out of it.");
				}else{
					// TODO The C++ source doesn't contain this line.
					cur++;
				}
			}

			pMax = iWrite;
		}
		return pMax;
	}

	// ------------------------------------------------------------------------------------------------
	/** Get a color (3 or 4 floats) from the material */
	static void aiGetMaterialColor(Material pMat, String pKey, int type, int index, float[] pOut)
	{
		int iMax = 4;
		iMax = aiGetMaterialFloatArray(pMat,pKey,type,index,pOut,4);

		// if no alpha channel is defined: set it to 1.0
		if (3 == iMax) {
			pOut[3]= 1.0f;
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	/** Get a color (3 or 4 floats) from the material */
	static void aiGetMaterialColor(Material pMat, String pKey, int type, int index, Vector4f pOut)
	{
		float[] out = new float[4];
		aiGetMaterialColor(pMat,pKey,type,index,out);
        pOut.load(out, 0);
	}

	// ------------------------------------------------------------------------------------------------
	/** Get a aiUVTransform (4 floats) from the material */
	static void aiGetMaterialUVTransform(Material pMat, String pKey, int type, int index, UVTransform pOut)
	{
		float[] out = new float[4];
		aiGetMaterialFloatArray(pMat,pKey,type,index,out,4);
		pOut.load(out, 0);
	}

	// ------------------------------------------------------------------------------------------------
	/** Get a string from the material */
	static String aiGetMaterialString(Material pMat, String pKey, int type, int index)
	{
		final MaterialProperty prop = aiGetMaterialProperty(pMat,pKey,type,index);
		if (prop == null) {
			throw new NullPointerException("aiGetMaterialProperty return null");
		}

		if( PropertyTypeInfo.aiPTI_String == prop.mType) {
//			ai_assert(prop.mDataLength>=5);

			// The string is stored as 32 but length prefix followed by zero-terminated UTF8 data
//			pOut.length = static_cast<int>(*reinterpret_cast<uint32_t*>(prop.mData));
			int length = AssUtil.getInt(prop.mData, 0);

			// Not need contain the zero-terminated for Java programming. TODO
//			ai_assert(pOut.length+1+4==prop.mDataLength && !prop.mData[prop.mDataLength-1]);
			if(length + 4 != prop.getDataLength())
				throw new IllegalArgumentException("The length of string doesn't match the length of the data.");
//			memcpy(pOut.data,prop.mData+4,pOut.length+1);
			return new String(prop.mData, 4, length);
		}
		else {
			// TODO - implement lexical cast as well
			DefaultLogger.error("Material property" + pKey + " was found, but is no string" );	
			return null;
		}
	}

	// ------------------------------------------------------------------------------------------------
	// Get the number of textures on a particular texture stack
	static int aiGetMaterialTextureCount(Material pMat, TextureType type)
	{
		/* Textures are always stored with ascending indices (ValidateDS provides a check, so we don't need to do it again) */
		int max = 0;
		for (int i = 0; i < pMat.getNumProperties();++i) {
			MaterialProperty prop = pMat.mProperties[i];

			if (prop !=null/* just a sanity check ... */ 
//				&& 0 == strcmp( prop.mKey.data, _AI_MATKEY_TEXTURE_BASE )
				&& _AI_MATKEY_TEXTURE_BASE.equals(prop.mKey)
				&& prop.mSemantic == type.ordinal()) {
		
				max = Math.max(max,prop.mIndex+1);
			}
		}
		return max;
	}

	// ------------------------------------------------------------------------------------------------
	static void aiGetMaterialTexture(Material mat,
	    TextureType type,
	    int  index,
	    String[] path,
		TextureMapping[] _mapping	/*= NULL*/,
	    int[] uvindex		/*= NULL*/,
	    float[] blend				/*= NULL*/,
	    TextureOp[] op				/*= NULL*/,
		TextureMapMode[] mapmode	/*= NULL*/,
		int[] flags         /*= NULL*/
		)
	{
		// Get the path to the texture
//		if (AI_SUCCESS != aiGetMaterialString(mat,AI_MATKEY_TEXTURE(type,index),path))	{
//			return AI_FAILURE;
//		}
		path[0] = aiGetMaterialString(mat, _AI_MATKEY_TEXTURE_BASE, type.ordinal(), index);
		// Determine mapping type 
//		TextureMapping mapping = TextureMapping.aiTextureMapping_UV;
		int[] out = {TextureMapping.aiTextureMapping_UV.ordinal()};
		out[0] = aiGetMaterialInteger(mat,_AI_MATKEY_TEXTURE_BASE, type.ordinal(), index);
		if (_mapping != null)
			_mapping[0] = TextureMapping.values()[out[0]];

		// Get UV index 
		if (TextureMapping.aiTextureMapping_UV.ordinal() == out[0] && uvindex != null)	{
			uvindex[0] = aiGetMaterialInteger(mat,_AI_MATKEY_UVWSRC_BASE, type.ordinal(),index);
		}
		// Get blend factor 
		if (blend != null)	{
			blend[0] = aiGetMaterialFloat(mat,_AI_MATKEY_TEXBLEND_BASE,type.ordinal(),index, 0);
		}
		// Get texture operation 
		if (op != null){
			op[0] = TextureOp.values()[aiGetMaterialInteger(mat,_AI_MATKEY_TEXOP_BASE,type.ordinal(),index)];
		}
		// Get texture mapping modes
		if (mapmode != null)	{
			TextureMapMode[] modes = TextureMapMode.values();
			mapmode[0] = modes[aiGetMaterialInteger(mat,_AI_MATKEY_MAPPINGMODE_U_BASE,type.ordinal(),index)];
			mapmode[1] = modes[aiGetMaterialInteger(mat,_AI_MATKEY_MAPPINGMODE_U_BASE,type.ordinal(),index)];		
		}
		// Get texture flags
		if (flags!=null){
			flags[0] = aiGetMaterialInteger(mat,_AI_MATKEY_MAPPINGMODE_U_BASE,type.ordinal(),index);
		}
	}
}
