package assimp.common;

import java.util.Arrays;

/** 
 * Data structure for a single material property<p>
*
*  As an user, you'll probably never need to deal with this data structure.
*  Just use the provided aiGetMaterialXXX() or aiMaterial::Get() family
*  of functions to query material properties easily. Processing them 
*  manually is faster, but it is not the recommended way. It isn't worth
*  the effort. <p>
*  Material property names follow a simple scheme:
*  <pre>
*    $[name]
*    ?[name]
*       A public property, there must be corresponding AI_MATKEY_XXX define
*       2nd: Public, but ignored by the #aiProcess_RemoveRedundantMaterials 
*       post-processing step.
*    ~[name]
*       A temporary property for internal use. 
*  </pre>
*/
public class MaterialProperty implements Copyable<MaterialProperty>{

	/** Specifies the name of the property (key)
     *  Keys are generally case insensitive. 
     */
    public String mKey;

	/** Textures: Specifies their exact usage semantic.
	 * For non-texture properties, this member is always 0 
	 * (or, better-said, #aiTextureType_NONE).
	 */
    public int mSemantic;

	/** Textures: Specifies the index of the texture.
	 *  For non-texture properties, this member is always 0.
	 */
    public int mIndex;

    /** Type information for the property.<p>
     *
     * Defines the data layout inside the data buffer. This is used
	 * by the library internally to perform debug checks and to 
	 * utilize proper type conversions. 
	 * (It's probably a hacky solution, but it works.)
     */
    public PropertyTypeInfo mType;

    /**	Binary buffer to hold the property's value.
     * The size of the buffer is always mDataLength.<p>
     * <b>NOTE: </b> Use the byte array hold the arbitrary data in Java programming is not a good idea.
     */
    public byte[] mData;
    
    /**	Return ths size of the buffer mData is pointing to, in bytes.
	 *  This value may not be 0.
     */
    public int getDataLength(){ return mData != null ? mData.length : 0;}
    
    int _size() {return getDataLength() + 12 + AssUtil.length(mKey) * 2 + 4;}

	@Override
	public MaterialProperty copy() {
		MaterialProperty property = new MaterialProperty();
		if(mData != null){
			property.mData = Arrays.copyOf(mData, mData.length);
		}
		
		property.mIndex = mIndex;
		property.mKey = mKey;
		property.mSemantic = mSemantic;
		property.mType = mType;
		
		return property;
	}
}
