package assimp.common;

/** Defines some mixed flags for a particular texture.<p>
*
*  Usually you'll instruct your cg artists how textures have to look like ...
*  and how they will be processed in your application. However, if you use
*  Assimp for completely generic loading purposes you might also need to 
*  process these flags in order to display as many 'unknown' 3D models as 
*  possible correctly.<p>
*
*  This corresponds to the #AI_MATKEY_TEXFLAGS property.
*/
public enum TextureFlags {

	/** The texture's color values have to be inverted (componentwise 1-n)
	 */
	aiTextureFlags_Invert /*= 0x1*/,

	/** Explicit request to the application to process the alpha channel
	 *  of the texture.<p>
	 *
	 *  Mutually exclusive with #aiTextureFlags_IgnoreAlpha. These
	 *  flags are set if the library can say for sure that the alpha
	 *  channel is used/is not used. If the model format does not
	 *  define this, it is left to the application to decide whether
	 *  the texture alpha channel - if any - is evaluated or not.
	 */
	aiTextureFlags_UseAlpha /*= 0x2*/,

	/** Explicit request to the application to ignore the alpha channel
	 *  of the texture.<p>
	 *
	 *  Mutually exclusive with #aiTextureFlags_UseAlpha. 
	 */
	aiTextureFlags_IgnoreAlpha /*= 0x4*/,
}
