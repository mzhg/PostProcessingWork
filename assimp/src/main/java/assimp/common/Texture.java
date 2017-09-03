package assimp.common;

import java.nio.IntBuffer;

/** Helper structure to describe an embedded texture<p>
 * 
 * Normally textures are contained in external files but some file formats embed
 * them directly in the model file. There are two types of embedded textures: 
 * 1. Uncompressed textures. The color data is given in an uncompressed format. 
 * 2. Compressed textures stored in a file format like png or jpg. The raw file 
 * bytes are given so the application must utilize an image decoder (e.g. DevIL) to
 * get access to the actual color data.
 */
public class Texture implements Copyable<Texture>{

	/** Width of the texture, in pixels<p>
	 *
	 * If mHeight is zero the texture is compressed in a format
	 * like JPEG. In this case mWidth specifies the size of the
	 * memory area pcData is pointing to, in bytes.
	 */
	public int mWidth;

	/** Height of the texture, in pixels<p>
	 *
	 * If this value is zero, pcData points to an compressed texture
	 * in any format (e.g. JPEG).
	 */
	public int mHeight;

	/** A hint from the loader to make it easier for applications
	 *  to determine the type of embedded compressed textures.<p>
	 *
	 * If mHeight != 0 this member is undefined. Otherwise it
	 * is set set to '\\0\\0\\0\\0' if the loader has no additional
	 * information about the texture file format used OR the
	 * file extension of the format without a trailing dot. If there 
	 * are multiple file extensions for a format, the shortest 
	 * extension is chosen (JPEG maps to 'jpg', not to 'jpeg').
	 * E.g. 'dds\\0', 'pcx\\0', 'jpg\\0'.  All characters are lower-case.
	 * The fourth character will always be '\\0'.
	 */
	public String achFormatHint = "";

	/** Data of the texture.<p>
	 *
	 * Points to an array of mWidth * mHeight aiTexel's.
	 * The format of the texture data is always RGBA8888 to
	 * make the implementation for user of the library as easy
	 * as possible. If mHeight = 0 this is a pointer to a memory
	 * buffer of size mWidth containing the compressed texture
	 * data. Good luck, have fun!
	 */
	public IntBuffer pcData;
	
	//! For compressed textures (mHeight == 0): compare the
	//! format hint against a given string.
	//! @param s Input string. 3 characters are maximally processed.
	//!        Example values: "jpg", "png"
	//! @return true if the given string matches the format hint
	public boolean checkFormat(String s){
		return achFormatHint.equals(s);
	}

	@Override
	public Texture copy() {
		Texture texture = new Texture();
		texture.mHeight = mHeight;
		texture.mWidth  = mWidth;
		texture.achFormatHint = achFormatHint;
		texture.pcData = AssUtil.copyOf(pcData);
		return texture;
	}
}
