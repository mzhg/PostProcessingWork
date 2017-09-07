package com.nvidia.developer.opengl.models.obj;

import java.io.IOException;

/**
 * Interface class to be implemented by the caller to allow the loading
 * process to read files, both the mesh and any embedded files such as
 * material libraries. 
 */
public interface NvModelFileLoader {

	/**
	 * Method to be called to load a resource file
	 * @param fileName Name of the file to load
	 * @return A pointer to a buffer containing the contents of the requested file or
     *         null if the file could not be read.  The buffer is still owned by the
     *         file loader and must be kept in memory until ReleaseData() is
     *         called on the returned buffer.
	 */
	byte[] loadDataFromFile(String fileName) throws IOException;
}
