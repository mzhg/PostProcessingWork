package assimp.importer.collada;

import assimp.common.AssimpConfig;
import assimp.common.TextureOp;
import assimp.common.UVTransform;

/** Represents a texture sampler in collada */
final class Sampler {

	/** Name of image reference
	 */
	String mName;

	/** Wrap U?
	 */
	boolean mWrapU = true;

	/** Wrap V?
	 */
	boolean mWrapV = true;

	/** Mirror U?
	 */
	boolean mMirrorU;

	/** Mirror V?
	 */
	boolean mMirrorV;

	/** Blend mode
	 */
	TextureOp mOp = TextureOp.aiTextureOp_Multiply;

	/** UV transformation
	 */
	UVTransform mTransform;

	/** Name of source UV channel
	 */
	String mUVChannel;

	/** Resolved UV channel index or UINT_MAX if not known
	 */
	int mUVId = AssimpConfig.UINT_MAX;

	// OKINO/MAX3D extensions from here
	// -------------------------------------------------------

	/** Weighting factor
	 */
	float mWeighting = 1.0f;

	/** Mixing factor from OKINO
	 */
	float mMixWithPrevious =1.0f;
}
