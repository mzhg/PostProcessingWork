/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team
All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the 
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/
package assimp.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.StringTokenizer;

public abstract class BaseImporter {

	/** Error description in case there was one. */
	protected String mErrorText;

	/** Currently set progress handler */
	protected ProgressHandler progress;
	
	// -------------------------------------------------------------------
	/** Returns whether the class can handle the format of the given file.<p>
	 *
	 * The implementation should be as quick as possible. A check for
	 * the file extension is enough. If no suitable loader is found with
	 * this strategy, canRead() is called again, the 'checkSig' parameter
	 * set to true this time. Now the implementation is expected to
	 * perform a full check of the file structure, possibly searching the
	 * first bytes of the file for magic identifiers or keywords.
	 *
	 * @param pFile Path and file name of the file to be examined.
	 * @param pIOHandler The IO handler to use for accessing any file.
	 * @param checkSig Set to true if this method is called a second time.
	 *   This time, the implementation may take more time to examine the
	 *   contents of the file to be loaded for magic bytes, keywords, etc
	 *   to be able to load files with unknown/not existent file extensions.
	 * @return true if the class can read this file, false if not.
	 */
	protected abstract boolean canRead(String pFile,InputStream pIOHandler, boolean checkSig) throws IOException;

	// -------------------------------------------------------------------
	/** Imports the given file and returns the imported data.
	 * If the import succeeds, ownership of the data is transferred to 
	 * the caller. If the import fails, NULL is returned. The function
	 * takes care that any partially constructed data is destroyed
	 * beforehand.
	 *
	 * @param pImp #Importer object hosting this loader.
	 * @param pFile Path of the file to be imported. 
	 * @param pIOHandler IO-Handler used to open this and possible other files.
	 * @return The imported data or NULL if failed. If it failed a 
	 * human-readable error description can be retrieved by calling 
	 * GetErrorText()
	 *
	 * @note This function is not intended to be overridden. Implement 
	 * InternReadFile() to do the import. If an exception is thrown somewhere 
	 * in InternReadFile(), this function will catch it and transform it into
	 *  a suitable response to the caller.
	 */
	public Scene readFile( Importer pImp, File pFile, InputStream pIOHandler){
//		progress = pImp.getProgressHandler(); TODO
//		ai_assert(progress);

		// Gather configuration properties for this run
		setupProperties( pImp );

		// Construct a file system filter to improve our success ratio at reading external files
//		FileSystemFilter filter(pFile,pIOHandler);

		// create a scene object to hold the data
		Scene sc = new Scene();

		// dispatch importing
		try
		{
			internReadFile( pFile, sc);
		} catch(Exception err )	{
			// extract error description
			mErrorText = err.getMessage();
			DefaultLogger.error(mErrorText);
			return null;
		}

		// return what we gathered from the import. 
		return sc;
	}

	// -------------------------------------------------------------------
	/** Returns the error description of the last error that occured. 
	 * @return A description of the last error that occured. An empty
	 * string if there was no error.
	 */
	public String getErrorText() { return mErrorText;}

	// -------------------------------------------------------------------
	/** Called prior to readFile().<p>
	 * The function is a request to the importer to update its configuration
	 * basing on the Importer's configuration property list.
	 * @param pImp Importer instance
	 */
	public void setupProperties(Importer pImp){
		// the default implementation does nothing
	}

	
	// -------------------------------------------------------------------
	/** Called by #Importer::GetImporterInfo to get a description of 
	 *  some loader features. Importers must provide this information. */
	protected abstract ImporterDesc getInfo();



	// -------------------------------------------------------------------
	/** Called by #Importer::GetExtensionList for each loaded importer.
	 *  Take the extension list contained in the structure returned by
	 *  #GetInfo and insert all file extensions into the given set.
	 *  @param extension set to collect file extensions in*/
	public void getExtensionList(Set<String> extensions){
		ImporterDesc desc = getInfo();
		String ext = desc.mFileExtensions;
		StringTokenizer tokenizer = new StringTokenizer(ext, " ");
		while(tokenizer.hasMoreTokens()){
			extensions.add(tokenizer.nextToken());
		}
	}
	
	/** Imports the given file into the given scene structure. The 
	 * function is expected to throw an ImportErrorException if there is 
	 * an error. If it terminates normally, the data in aiScene is 
	 * expected to be correct. Override this function to implement the 
	 * actual importing.
	 * <br>
	 *  The output scene must meet the following requirements:<br>
	 * <ul>
	 * <li>At least a root node must be there, even if its only purpose
	 *     is to reference one mesh.</li>
	 * <li>aiMesh::mPrimitiveTypes may be 0. The types of primitives
	 *   in the mesh are determined automatically in this case.</li>
	 * <li>the vertex data is stored in a pseudo-indexed "verbose" format.
	 *   In fact this means that every vertex that is referenced by
	 *   a face is unique. Or the other way round: a vertex index may
	 *   not occur twice in a single aiMesh.</li>
	 * <li>aiAnimation::mDuration may be -1. Assimp determines the length
	 *   of the animation automatically in this case as the length of
	 *   the longest animation channel.</li>
	 * <li>aiMesh::mBitangents may be NULL if tangents and normals are
	 *   given. In this case bitangents are computed as the cross product
	 *   between normal and tangent.</li>
	 * <li>There needn't be a material. If none is there a default material
	 *   is generated. However, it is recommended practice for loaders
	 *   to generate a default material for yourself that matches the
	 *   default material setting for the file format better than Assimp's
	 *   generic default material. Note that default materials *should*
	 *   be named AI_DEFAULT_MATERIAL_NAME if they're just color-shaded
	 *   or AI_DEFAULT_TEXTURED_MATERIAL_NAME if they define a (dummy) 
	 *   texture. </li>
	 * </ul>
	 * If the AI_SCENE_FLAGS_INCOMPLETE-Flag is <b>not</b> set:<ul>
	 * <li> at least one mesh must be there</li>
	 * <li> there may be no meshes with 0 vertices or faces</li>
	 * </ul>
	 * This won't be checked (except by the validation step): Assimp will
	 * crash if one of the conditions is not met!
	 *
	 * @param pFile Path of the file to be imported.
	 * @param pScene The scene object to hold the imported data.
	 * null is not a valid parameter.
	 */
	protected abstract void internReadFile(File pFile, Scene pScene);
	
	/** A utility for CanRead().<p>
	 *
	 *  The function searches the header of a file for a specific token
	 *  and returns true if this token is found. This works for text
	 *  files only. There is a rudimentary handling of UNICODE files.
	 *  The comparison is case independent.
	 *
	 *  @param pIOSystem IO System to work with
	 *  @param file File name of the file
	 *  @param tokens List of tokens to search for
	 *  @param numTokens Size of the token array
	 *  @param searchBytes Number of bytes to be searched for the tokens.
	 * @throws IOException 
	 */
	public static boolean searchFileHeaderForToken(InputStream pIOSystem, String file, String[] tokens, int numTokens,int searchBytes /*= 200*/, boolean tokensSol /*= false*/) throws IOException{
		byte[] buffer = new byte[searchBytes + 1];
		int read = pIOSystem.read(buffer);
		if(read == 0)
			return false;
		
		// TODO  Need Test
		String source = new String(buffer, 0, read);
		for(int i = 0; i < tokens.length; i++){
			int r = source.indexOf(tokens[i]);
			
			if(r == -1)
				continue;
			if (!tokensSol || r == 0 || source.charAt(r - 1) == '\r' || source.charAt(r - 1) == '\n') {
				DefaultLogger.debug("Found positive match for header keyword: " + tokens[i]);
				return true;
			}
		}
		
		return false;
	}
	
	/** A utility for CanRead().<p>
	 *
	 *  The function searches the header of a file for a specific token
	 *  and returns true if this token is found. This works for text
	 *  files only. There is a rudimentary handling of UNICODE files.
	 *  The comparison is case independent.
	 *
	 *  @param pIOSystem IO System to work with
	 *  @param file File name of the file
	 *  @param tokens List of tokens to search for
	 *  @see #searchFileHeaderForToken(InputStream, String, String[], int, int, boolean)
	 * @throws IOException 
	 */
	public static boolean searchFileHeaderForToken(InputStream pIOSystem, String file, String[] tokens) throws IOException{
		return searchFileHeaderForToken(pIOSystem, file, tokens, tokens.length, 200, false);
	}


	// -------------------------------------------------------------------
	/** @brief Check whether a file has a specific file extension
	 *  @param pFile Input file
	 *  @param ext0 Extension to check for. Lowercase characters only, no dot!
	 *  @param ext1 Optional second extension
	 *  @param ext2 Optional third extension
	 *  @note Case-insensitive
	 */
	public static boolean simpleExtensionCheck (String pFile, String ext0, String ext1 /*= NULL*/, String ext2/* = NULL*/){
		int pos = pFile.lastIndexOf('.');
		if(pos == -1)
			return false;
		
		String end = pFile.substring(pos + 1);
		if(end.equalsIgnoreCase(ext0))
			return true;
		
		if(ext1 != null && end.equalsIgnoreCase(ext1))
			return true;
		
		if(ext2 != null && end.equalsIgnoreCase(ext2))
			return true;
		
		return false;
	}

	// -------------------------------------------------------------------
	/** @brief Extract file extension from a string
	 *  @param pFile Input file
	 *  @return Extension without trailing dot, all lowercase
	 */
	public static String getExtension (String pFile){
		int pos = pFile.lastIndexOf('.');
		if(pos == -1)
			return "";
		
		return pFile.substring(pos + 1).toLowerCase();
	}

	// -------------------------------------------------------------------
	/** 
	 *  Check whether a file starts with one or more magic tokens
	 *  @param pFile Input file
	 *  @param magic n magic tokens
	 *  @param offset Offset from file start where tokens are located
	 *  @param size of one token, in bytes. Maximally 16 bytes.
	 *  @return true if one of the given tokens was found
	 *
	 *  @note For convinence, the check is also performed for the
	 *  byte-swapped variant of all tokens (big endian). Only for
	 *  tokens of size 2,4.
	 */
	//TODO This method have bug.
	public static boolean checkMagicToken(File pFile, byte[] magic, int offset /*= 0*/, int size /*= 4*/){
		if(size > 16){
			throw new IllegalArgumentException("The size > 16");
		}
		
		try(InputStream in = new FileInputStream(pFile)){
			in.skip(offset);
			byte[] data = new byte[size];
			
			if(in.read(data) != size){
				return false;
			}
			
			int magic_index = 0;
			for (int i = 0; i < magic.length/size; ++i) {
				// also check against big endian versions of tokens with size 2,4
				// that's just for convinience, the chance that we cause conflicts
				// is quite low and it can save some lines and prevent nasty bugs
				if (2 == size) {
					short val = AssUtil.getShort(magic, magic_index);
					short rev = AssUtil.swap16(val);
					short t =   AssUtil.getShort(data, 0);
//					ByteSwap::Swap(&rev);
//					if (data_u16[0] == *magic_u16 || data_u16[0] == rev) {
//						return true;
//					}
					
					if(t == val || t == rev){
						return true;
					}
				}
				else if (4 == size) {
//					uint32_t rev = *magic_u32;
//					ByteSwap::Swap(&rev);
//					if (data_u32[0] == *magic_u32 || data_u32[0] == rev) {
//						return true;
//					}
					
					int val = AssUtil.getInt(magic, magic_index);
					int rev = AssUtil.swap32(val);
					int t =   AssUtil.getInt(data, 0);
					if(t == val || t == rev){
						return true;
					}
				}
				else {
					// any length ... just compare
//					if(!memcmp(magic,data,size)) {
//						return true;
//					}
					
					boolean returnTrue = true;
					for(int j = 0; j < size;j++){
						if(magic[j + magic_index] != data[j]){
							returnTrue = false;
							break;	
						}
					}
					
					if(returnTrue)
						return true;
				}
				magic_index += size;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	// -------------------------------------------------------------------
	/* An utility for all text file loaders. It converts a file to our
	 *   UTF8 character set. Errors are reported, but ignored.
	 *
	 *  @param data File buffer to be converted to UTF8 data. The buffer 
	 *  is resized as appropriate. */
	//public static void convertToUTF8(byte[] data);

	// -------------------------------------------------------------------
	/* An utility for all text file loaders. It converts a file from our
	 *   UTF8 character set back to ISO-8859-1. Errors are reported, but ignored.
	 *
	 *  @param data File buffer to be converted from UTF8 to ISO-8859-1. The buffer
	 *  is resized as appropriate. */
	//public static void ConvertUTF8toISO8859_1(std::string& data);

	// -------------------------------------------------------------------
	/** Utility for text file loaders which copies the contents of the
	 *  file into a memory buffer and converts it to our UTF8
	 *  representation.
	 *  @param stream Stream to read from. 
	 *  @param data Output buffer to be resized and filled with the
	 *   converted text file data. The buffer is terminated with
	 *   a binary 0. */
	public static void textFileToBuffer(InputStream stream, byte[] data){
		
	}

}
