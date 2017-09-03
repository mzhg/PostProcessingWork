package assimp.importer.collada;

import java.nio.ByteBuffer;

/** An image, meaning texture */
final class COLImage {

	String mFileName;
	
	/** If image file name is zero, embedded image data
	 */
	ByteBuffer mImageData;

	/** If image file name is zero, file format of
	 *  embedded image data.
	 */
	String mEmbeddedFormat;
}
