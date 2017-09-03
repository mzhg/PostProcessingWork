/*
---------------------------------------------------------------------------
Open Asset Import Library (assimp)
---------------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team

All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the following 
conditions are met:

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
---------------------------------------------------------------------------
*/
package assimp.common;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.util.vector.Matrix4f;

/** CPP-API: The Importer class forms an C++ interface to the functionality of the 
*   Open Asset Import Library.<p>
*
* Create an object of this class and call ReadFile() to import a file. 
* If the import succeeds, the function returns a pointer to the imported data. 
* The data remains property of the object, it is intended to be accessed 
* read-only. The imported data will be destroyed along with the Importer 
* object. If the import fails, ReadFile() returns a NULL pointer. In this
* case you can retrieve a human-readable error description be calling 
* GetErrorString(). You can call ReadFile() multiple times with a single Importer
* instance. Actually, constructing Importer objects involves quite many
* allocations and may take some time, so it's better to reuse them as often as
* possible.<p>
*
* If you need the Importer to do custom file handling to access the files,
* implement IOSystem and IOStream and supply an instance of your custom 
* IOSystem implementation by calling SetIOHandler() before calling ReadFile().
* If you do not assign a custion IO handler, a default handler using the 
* standard C++ IO logic will be used.<p>
*
* <b>NOTE: </b>One Importer instance is not thread-safe. If you use multiple
* threads for loading, each thread should maintain its own Importer instance.
*/
public class Importer {

	public static final int AI_PROPERTY_WAS_NOT_EXISTING = 0xffffffff;
	
	// Just because we don't want you to know how we're hacking around.
	protected ImporterPimpl pimpl;
	
	/** Constructor. Creates an empty importer object. 
	 * 
	 * Call ReadFile() to start the import process. The configuration
	 * property table is initially empty.
	 */
	public Importer(){
		// allocate the pimpl first
		pimpl = new ImporterPimpl();

		pimpl.mScene = null;
		pimpl.mErrorString = "";

		// Allocate a default IO handler
//		pimpl.mIOHandler = new DefaultIOSystem;
//		pimpl.mIsDefaultHandler = true; 
		pimpl.bExtraVerbose     = false; // disable extra verbose mode by default

		pimpl.mProgressHandler = new DefaultProgressHandler();
		pimpl.mIsDefaultProgressHandler = true;

		pimpl.mImporter = getImporterInstanceList();
		pimpl.mPostProcessingSteps = getPostProcessingStepInstanceList();

		// Allocate a SharedPostProcessInfo object and store pointers to it in all post-process steps in the list.
//		pimpl.mPPShared = new HashMap<String, Object>();
		// TODO
//		for (std::vector<BaseProcess*>::iterator it =  pimpl.mPostProcessingSteps.begin();
//			it != pimpl.mPostProcessingSteps.end(); 
//			++it)	{
//
//			(*it).SetSharedData(pimpl.mPPShared);
//		}
	}

	private static List<BaseProcess> getPostProcessingStepInstanceList() {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<BaseImporter> getImporterInstanceList() {
		// TODO Auto-generated method stub
		return null;
	}

	// -------------------------------------------------------------------
	/** Copy constructor.
	 * 
	 * This copies the configuration properties of another Importer.
	 * If this Importer owns a scene it won't be copied.
	 * Call ReadFile() to start the import process.
	 */
	public Importer(Importer other){
		this();
		
		pimpl.mIntProperties.putAll(other.pimpl.mIntProperties);
		pimpl.mFloatProperties.putAll(other.pimpl.mFloatProperties);
		pimpl.mStringProperties.putAll(other.pimpl.mStringProperties);
		pimpl.mMatrixProperties.putAll(other.pimpl.mMatrixProperties);
	}

	// -------------------------------------------------------------------
	/* Destructor. The object kept ownership of the imported data,
	 * which now will be destroyed along with the object. 
	 */
//	~Importer();


	// -------------------------------------------------------------------
	/** Registers a new loader.
	 *
	 * @param pImp Importer to be added. The Importer instance takes 
	 *   ownership of the pointer, so it will be automatically deleted
	 *   with the Importer instance.
	 * @return true if the loader has been added. The registration
	 *   fails if there is already a loader for a specific file extension.
	 */
	public boolean registerLoader(BaseImporter pImp){
		if(pImp == null)
			return false;
		
		if(AssimpConfig.ASSIMP_BUILD_DEBUG){
			// --------------------------------------------------------------------
			// Check whether we would have two loaders for the same file extension 
			// This is absolutely OK, but we should warn the developer of the new
			// loader that his code will probably never be called if the first 
			// loader is a bit too lazy in his file checking.
			// --------------------------------------------------------------------
			Set<String> st = new ObjectArraySet<String>(); // The worst performance.
			pImp.getExtensionList(st);
			StringBuilder baked = new StringBuilder();
			for(String it : st) {
				if (isExtensionSupported(it)) {
					DefaultLogger.warn("The file extension " + it + " is already in use");
				}
				
				if(DefaultLogger.LOG_OUT)
					baked.append(it);
			}
			
			DefaultLogger.info("Registering custom importer for these file extensions: " + baked);
		}

		// add the loader
		pimpl.mImporter.add(pImp);
		return true;
	}

	// -------------------------------------------------------------------
	/** Unregisters a loader.
	 *
	 * @param pImp Importer to be unregistered.
	 * @return true if the loader has been removed. The function
	 *   fails if the loader is currently in use (this could happen
	 *   if the {@link Importer} instance is used by more than one thread) or
	 *   if it has not yet been registered.
	 */
	public boolean unregisterLoader(BaseImporter pImp){
		if(pImp == null)
			// unregistering a NULL importer is no problem for us ... really!
			return true;
		
		boolean result = pimpl.mImporter.remove(pImp);
		if(DefaultLogger.LOG_OUT){
			if(result)
				DefaultLogger.info("Unregistering custom importer: ");
			else
				DefaultLogger.warn("Unable to remove custom importer: I can't find you ...");
		}
		
		return result;
	}

	// -------------------------------------------------------------------
	/** Registers a new post-process step.<p>
	 *
	 * At the moment, there's a small limitation: new post processing 
	 * steps are added to end of the list, or in other words, executed 
	 * last, after all built-in steps.
	 * @param pImp Post-process step to be added. The Importer instance 
	 *   takes ownership of the pointer, so it will be automatically 
	 *   deleted with the Importer instance.
	 * @return true if the step has been added correctly.
	 */
	public boolean registerPPStep(BaseProcess pImp){
		if(pImp == null)
			return false;
		
		pimpl.mPostProcessingSteps.add(pImp);
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.info("Registering custom post-processing step");
		return true;
	}

	// -------------------------------------------------------------------
	/** Unregisters a post-process step.
	 *
	 * @param pImp Step to be unregistered. 
	 * @return true if the step has been removed. The function
	 *   fails if the step is currently in use (this could happen
	 *   if the #Importer instance is used by more than one thread) or
	 *   if it has not yet been registered.
	 */
	public boolean unregisterPPStep(BaseProcess pImp){
		if(pImp == null)
			return true;
		
		boolean result = pimpl.mPostProcessingSteps.remove(pImp);
		if(DefaultLogger.LOG_OUT){
			if(result)
				DefaultLogger.info("Unregistering custom post-processing step: ");
			else
				DefaultLogger.warn("Unable to remove custom post-processing step: I can't find you ...");
		}
		return false;
	}


	// -------------------------------------------------------------------
	/** Set an integer configuration property.
	 * <p><b>NOTE: </b> Property of different types (float, int, string ..) are kept
	 *   on different stacks, so calling SetPropertyInteger() for a 
	 *   floating-point property has no effect - the loader will call
	 *   GetPropertyFloat() to read the property, but it won't be there.
	 * @param szName Name of the property. All supported properties
	 *   are defined in the aiConfig.g header (all constants share the
	 *   prefix AI_CONFIG_XXX and are simple strings).
	 * @param iValue New value of the property
	 * @return the previous value set before.
	 */
	public int setPropertyInteger(String szName, int iValue){
		return pimpl.mIntProperties.put(szName, iValue);
	}

	// -------------------------------------------------------------------
	/** Set a boolean configuration property. Boolean properties
	 *  are stored on the integer stack internally so it's possible
	 *  to set them via #SetPropertyBool and query them with
	 *  {@link #getPropertyBoolean()} and vice versa.
	 * @see #setPropertyInteger(String, int)
	 */
	public int setPropertyBoolean(String szName, boolean value)	{
		return setPropertyInteger(szName,value ? 1 : 0);
	}

	// -------------------------------------------------------------------
	/** Set a floating-point configuration property.
	 * @see #setPropertyInteger(String, int)
	 */
	public float setPropertyFloat(String szName, float fValue){
		return pimpl.mFloatProperties.put(szName, fValue);
	}

	// -------------------------------------------------------------------
	/** Set a string configuration property.
	 * @see #setPropertyInteger(String, int)
	 */
	public String setPropertyString(String szName, String sValue){
		return pimpl.mStringProperties.put(szName, sValue);
	}

	// -------------------------------------------------------------------
	/** Set a matrix configuration property.
	 * @see #setPropertyInteger(String, int)
	 */
	public Matrix4f setPropertyMatrix(String szName, Matrix4f sValue){
		return pimpl.mMatrixProperties.put(szName, sValue);
	}

	// -------------------------------------------------------------------
	/** Get a configuration property.<p>
	 *  <b>NOTE: </b> Property of different types (float, int, string ..) are kept
	 *   on different lists, so calling SetPropertyInteger() for a 
	 *   floating-point property has no effect - the loader will call
	 *   GetPropertyFloat() to read the property, but it won't be there.
	 * @param szName Name of the property. All supported properties
	 *   are defined in the aiConfig.g header (all constants share the
	 *   prefix AI_CONFIG_XXX).
	 * @param iErrorReturn Value that is returned if the property 
	 *   is not found. 
	 * @return Current value of the property
	 */
	public int getPropertyInteger(String szName, int iErrorReturn /*= 0xffffffff*/){
		pimpl.mIntProperties.defaultReturnValue(iErrorReturn);
		return pimpl.mIntProperties.getInt(szName);
	}

	// -------------------------------------------------------------------
	/** Get a boolean configuration property. Boolean properties
	 *  are stored on the integer stack internally so it's possible
	 *  to set them via #SetPropertyBool and query them with
	 *  #GetPropertyBool and vice versa.
	 * @see GetPropertyInteger()
	 */
	public boolean getPropertyBoolean(String szName, boolean bErrorReturn) {
		return getPropertyInteger(szName,bErrorReturn ? 1 : 0)!=0;
	}

	// -------------------------------------------------------------------
	/** Get a floating-point configuration property
	 * @see GetPropertyInteger()
	 */
	public float getPropertyFloat(String szName, float fErrorReturn /*= 10e10f*/){
		pimpl.mFloatProperties.defaultReturnValue(fErrorReturn);
		return pimpl.mFloatProperties.getFloat(szName);
	}

	// -------------------------------------------------------------------
	/** Get a string configuration property
	 *
	 *  The return value remains valid until the property is modified.
	 * @see GetPropertyInteger()
	 */
	public String getPropertyString(String szName,String sErrorReturn /*= ""*/){
		String r = pimpl.mStringProperties.get(szName);
		if(r  == null)
			r = sErrorReturn;
		
		return r;
	}

	// -------------------------------------------------------------------
	/** Get a matrix configuration property
	 *
	 *  The return value remains valid until the property is modified.
	 * @see GetPropertyInteger()
	 */
	public Matrix4f GetPropertyMatrix(String szName,Matrix4f sErrorReturn/* = aiMatrix4x4()*/){
		Matrix4f r = pimpl.mMatrixProperties.get(szName);
		if(r == null)
			r = sErrorReturn;
		return r;
	}

	// -------------------------------------------------------------------
	/* Supplies a custom IO handler to the importer to use to open and
	 * access files. If you need the importer to use custion IO logic to 
	 * access the files, you need to provide a custom implementation of 
	 * IOSystem and IOFile to the importer. Then create an instance of 
	 * your custion IOSystem implementation and supply it by this function.
	 *
	 * The Importer takes ownership of the object and will destroy it 
	 * afterwards. The previously assigned handler will be deleted.
	 * Pass NULL to take again ownership of your IOSystem and reset Assimp
	 * to use its default implementation.
	 *
	 * @param pIOHandler The IO handler to be used in all file accesses 
	 *   of the Importer. 
	 */
//	public void setIOHandler(File pIOHandler);

	// -------------------------------------------------------------------
	/* Retrieves the IO handler that is currently set.
	 * You can use #IsDefaultIOHandler() to check whether the returned
	 * interface is the default IO handler provided by ASSIMP. The default
	 * handler is active as long the application doesn't supply its own
	 * custom IO handler via #SetIOHandler().
	 * @return A valid IOSystem interface, never NULL.
	 */
	//IOSystem* GetIOHandler() const;

	// -------------------------------------------------------------------
	/* Checks whether a default IO handler is active 
	 * A default handler is active as long the application doesn't 
	 * supply its own custom IO handler via #SetIOHandler().
	 * @return true by default
	 */
	//bool IsDefaultIOHandler() const;

	// -------------------------------------------------------------------
	/** Supplies a custom progress handler to the importer. This 
	 *  interface exposes a #Update() callback, which is called
	 *  more or less periodically (please don't sue us if it
	 *  isn't as periodically as you'd like it to have ...).
	 *  This can be used to implement progress bars and loading
	 *  timeouts. 
	 *  @param pHandler Progress callback interface. Pass NULL to 
	 *    disable progress reporting. 
	 *  @note Progress handlers can be used to abort the loading
	 *    at almost any time.*/
	public void setProgressHandler ( ProgressHandler pHandler ){
		// If the new handler is zero, allocate a default implementation.
		if (pHandler == null)
		{
			// Release pointer in the possession of the caller
			pimpl.mProgressHandler = new DefaultProgressHandler();
			pimpl.mIsDefaultProgressHandler = true;
		}
		// Otherwise register the custom handler
		else if (pimpl.mProgressHandler != pHandler)
		{
			pimpl.mProgressHandler = pHandler;
			pimpl.mIsDefaultProgressHandler = false;
		}
	}

	// -------------------------------------------------------------------
	/** Retrieves the progress handler that is currently set. 
	 * You can use #IsDefaultProgressHandler() to check whether the returned
	 * interface is the default handler provided by ASSIMP. The default
	 * handler is active as long the application doesn't supply its own
	 * custom handler via #SetProgressHandler().
	 * @return A valid ProgressHandler interface, never NULL.
	 */
	public ProgressHandler getProgressHandler(){ return pimpl.mProgressHandler;}

	// -------------------------------------------------------------------
	/** Checks whether a default progress handler is active 
	 * A default handler is active as long the application doesn't 
	 * supply its own custom progress handler via #SetProgressHandler().
	 * @return true by default
	 */
	public boolean isDefaultProgressHandler(){
		return pimpl.mIsDefaultProgressHandler;
	}
	
	// -------------------------------------------------------------------
	/** Check whether a given set of postprocessing flags
	 *  is supported.<p>
	 *
	 *  Some flags are mutually exclusive, others are probably
	 *  not available because your excluded them from your
	 *  Assimp builds. Calling this function is recommended if 
	 *  you're unsure.
	 *
	 *  @param pFlags Bitwise combination of the aiPostProcess flags.
	 *  @return true if this flag combination is fine.
	 */
	public boolean validateFlags(int pFlags){
		// run basic checks for mutually exclusive flags
		if(!_validateFlags(pFlags)) {
			return false;
		}

		// ValidateDS does not anymore occur in the pp list, it plays an awesome extra role ...
		if(AssimpConfig.ASSIMP_BUILD_NO_VALIDATEDS_PROCESS){
			if ((pFlags & PostProcessSteps.aiProcess_ValidateDataStructure)!= 0) {
				return false;
			}
		}
		pFlags &= ~PostProcessSteps.aiProcess_ValidateDataStructure;

		// Now iterate through all bits which are set in the flags and check whether we find at least
		// one pp plugin which handles it.
		for (int mask = 1; mask < (1 << (4*8-1));mask <<= 1) {
			
			if ((pFlags & mask)!=0) {
				boolean have = false;
				for(int a = 0; a < pimpl.mPostProcessingSteps.size(); a++)	{
					if (pimpl.mPostProcessingSteps.get(a).isActive(mask) ) {
						have = true;
						break;
					}
				}
				if (!have) {
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean _validateFlags(int pFlags){
		if ((pFlags & PostProcessSteps.aiProcess_GenSmoothNormals) != 0 && (pFlags & PostProcessSteps.aiProcess_GenNormals) != 0)	{
			DefaultLogger.error("#aiProcess_GenSmoothNormals and #aiProcess_GenNormals are incompatible");
			return false;
		}
		if ((pFlags & PostProcessSteps.aiProcess_OptimizeGraph) != 0 && (pFlags & PostProcessSteps.aiProcess_PreTransformVertices) != 0)	{
			DefaultLogger.error("#aiProcess_OptimizeGraph and #aiProcess_PreTransformVertices are incompatible");
			return false;
		}
		return true;
	}

	// -------------------------------------------------------------------
	/** Reads the given file and returns its contents if successful. <p>
	 * 
	 * If the call succeeds, the contents of the file are returned as a 
	 * pointer to an aiScene object. The returned data is intended to be 
	 * read-only, the importer object keeps ownership of the data and will
	 * destroy it upon destruction. If the import fails, NULL is returned.
	 * A human-readable error description can be retrieved by calling 
	 * GetErrorString(). The previous scene will be deleted during this call.
	 * @param pFile Path and filename to the file to be imported.
	 * @param pFlags Optional post processing steps to be executed after 
	 *   a successful import. Provide a bitwise combination of the 
	 *   #aiPostProcessSteps flags. If you wish to inspect the imported
	 *   scene first in order to fine-tune your post-processing setup,
	 *   consider to use #ApplyPostProcessing().
	 * @return A pointer to the imported data, NULL if the import failed.
	 *   The pointer to the scene remains in possession of the Importer
	 *   instance. Use GetOrphanedScene() to take ownership of it.
	 *
	 * <b>NOTE: </b> Assimp is able to determine the file format of a file
	 * automatically. 
	 */
	public Scene readFile( String pFile, int pFlags){
		{
			File file = new File(pFile);
			// Check whether this Importer instance has already loaded
			// a scene. In this case we need to delete the old one
			if (pimpl.mScene != null)	{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.debug("(Deleting previous scene)");
				freeScene();
			}

			// First check if the file is accessable at all
//			if( !pimpl.mIOHandler.Exists( pFile))	{
//
//				pimpl.mErrorString = "Unable to open file \"" + pFile + "\".";
//				DefaultLogger::get().error(pimpl.mErrorString);
//				return NULL;
//			}
			
			if(!file.exists() || !file.canRead()){
				pimpl.mErrorString = "Unable to open file \"" + pFile + "\".";
				DefaultLogger.error(pimpl.mErrorString);
				return null;
			}

//			boost::scoped_ptr<Profiler> profiler(GetPropertyInteger(AI_CONFIG_GLOB_MEASURE_TIME,0)?new Profiler():NULL);
//			if (profiler) {
//				profiler.BeginRegion("total");
//			}
			
			Profiler profiler = getPropertyBoolean(AssimpConfig.AI_CONFIG_GLOB_MEASURE_TIME , false) ? new Profiler() : null;
            if(profiler != null)
            	profiler.beginRegion("total");
			
			// Find an worker class which can handle the file
			BaseImporter imp = null;
			for(int a = 0; a < pimpl.mImporter.size(); a++)	{

				BaseImporter curr = pimpl.mImporter.get(a);
				if( curr.canRead( pFile, pimpl.mIOHandler, false)) {
					imp = curr;
					break;
				}
			}

			if (imp == null)	{
				// not so bad yet ... try format auto detection.
				int s = pFile.indexOf('.');
				if (s != -1) {
					DefaultLogger.info("File extension not known, trying signature-based detection");
					for(int a = 0; a < pimpl.mImporter.size(); a++)	{

						BaseImporter importer = pimpl.mImporter.get(a);
						if(importer.canRead( pFile, pimpl.mIOHandler, true)) {
							imp = importer;
							break;
						}
					}
				}
				
				// Put a proper error message if no suitable importer was found
				if(imp == null)	{
					pimpl.mErrorString = "No suitable reader found for the file format of file \"" + pFile + "\".";
					DefaultLogger.error(pimpl.mErrorString);
					return null;
				}
			}

			// Dispatch the reading to the worker class for this format
			DefaultLogger.info("Found a matching importer for this file format");
			pimpl.mProgressHandler.update(-1);

			if (profiler != null) {
				profiler.beginRegion("import");
			}

			pimpl.mScene = imp.readFile( this, file, pimpl.mIOHandler);
			pimpl.mProgressHandler.update(-1);

			if (profiler != null) {
				profiler.endRegion("import");
			}

			// If successful, apply all active post processing steps to the imported data
			if( pimpl.mScene != null)	{

				if(AssimpConfig.ASSIMP_BUILD_NO_VALIDATEDS_PROCESS){
					// The ValidateDS process is an exception. It is executed first, even before ScenePreprocessor is called.
					if ((pFlags & PostProcessSteps.aiProcess_ValidateDataStructure)!=0)
					{
						ValidateDSProcess ds = new ValidateDSProcess();
						ds.executeOnScene (this);
						if (pimpl.mScene == null) {
							return null;
						}
					}
				} // no validation

				// Preprocess the scene and prepare it for post-processing 
				if (profiler != null) {
					profiler.beginRegion("preprocess");
				}

				ScenePreprocessor pre = new ScenePreprocessor(pimpl.mScene);
				pre.processScene();

				pimpl.mProgressHandler.update( -1);
				if (profiler != null) {
					profiler.endRegion("preprocess");
				}

				// Ensure that the validation process won't be called twice
				applyPostProcessing(pFlags & (~PostProcessSteps.aiProcess_ValidateDataStructure));
			}
			// if failed, extract the error string
			else if( pimpl.mScene == null) {
				pimpl.mErrorString = imp.getErrorText();
			}

			// clear any data allocated by post-process steps
			pimpl.mPPShared.clear();

			if (profiler != null) {
				profiler.endRegion("total");
			}
		}
		
		return pimpl.mScene;
	}

	// -------------------------------------------------------------------
	/** Reads the given file from a memory buffer and returns its
	 *  contents if successful.<p>
	 * 
	 * If the call succeeds, the contents of the file are returned as a 
	 * pointer to an aiScene object. The returned data is intended to be 
	 * read-only, the importer object keeps ownership of the data and will
	 * destroy it upon destruction. If the import fails, NULL is returned.
	 * A human-readable error description can be retrieved by calling 
	 * GetErrorString(). The previous scene will be deleted during this call.
	 * Calling this method doesn't affect the active IOSystem.
	 * @param pBuffer Pointer to the file data
	 * @param pLength Length of pBuffer, in bytes
	 * @param pFlags Optional post processing steps to be executed after 
	 *   a successful import. Provide a bitwise combination of the 
	 *   #aiPostProcessSteps flags. If you wish to inspect the imported
	 *   scene first in order to fine-tune your post-processing setup,
	 *   consider to use #ApplyPostProcessing().
	 * @param pHint An additional hint to the library. If this is a non
	 *   empty string, the library looks for a loader to support 
	 *   the file extension specified by pHint and passes the file to
	 *   the first matching loader. If this loader is unable to completely
	 *   the request, the library continues and tries to determine the
	 *   file format on its own, a task that may or may not be successful.
	 *   Check the return value, and you'll know ...
	 * @return A pointer to the imported data, NULL if the import failed.
	 *   The pointer to the scene remains in possession of the Importer
	 *   instance. Use GetOrphanedScene() to take ownership of it.
	 *
	 * @note This is a straightforward way to decode models from memory
	 * buffers, but it doesn't handle model formats that spread their 
	 * data across multiple files or even directories. Examples include
	 * OBJ or MD3, which outsource parts of their material info into
	 * external scripts. If you need full functionality, provide
	 * a custom IOSystem to make Assimp find these files and use
	 * the regular ReadFile() API.
	 */
	public Scene readFileFromMemory( byte[] pBuffer, int offset, int pLength, int pFlags,String pHint){
		if(pHint == null)
			pHint = "";
		
		pimpl.mIOHandler = new ByteArrayInputStream(pBuffer, offset, pLength);
		String fbuff = String.format("%s.%s", "$$$___magic___$$$", pHint);
		readFile(fbuff, pFlags);
		pimpl.mIOHandler = null;
		return pimpl.mScene;
	}

	// -------------------------------------------------------------------
	/** Apply post-processing to an already-imported scene.<p>
	 *
	 *  This is strictly equivalent to calling {@link #readFile(String, int)} with the same
	 *  flags. However, you can use this separate function to inspect
	 *  the imported scene first to fine-tune your post-processing setup.
	 *  @param pFlags Provide a bitwise combination of the 
	 *   {@link PostProcessSteps} flags.
	 *  @return A pointer to the post-processed data. This is still the
	 *   same as the pointer returned by {@link #readFile(String, int)}. However, if
	 *   post-processing fails, the scene could now be null.
	 *   That's quite a rare case, post processing steps are not really
	 *   designed to 'fail'. To be exact, the #aiProcess_ValidateDS
	 *   flag is currently the only post processing step which can actually
	 *   cause the scene to be reset to null.
	 *
	 *  <b>NOTE: </b>The method does nothing if no scene is currently bound
	 *    to the <code>Importer</code> instance.  */
	public Scene applyPostProcessing(int pFlags){
		// Return immediately if no scene is active
		if (pimpl.mScene == null) {
			return null;
		}

		// If no flags are given, return the current scene with no further action
		if (pFlags == 0) {
			return pimpl.mScene;
		}

		// In debug builds: run basic flag validation
//		ai_assert(_ValidateFlags(pFlags));
		if(!_validateFlags(pFlags)){
			throw new AssertionError();
		}
		if(DefaultLogger.LOG_OUT)
			DefaultLogger.info("Entering post processing pipeline");
		
		if(!AssimpConfig.ASSIMP_BUILD_NO_VALIDATEDS_PROCESS){
			// The ValidateDS process plays an exceptional role. It isn't contained in the global
			// list of post-processing steps, so we need to call it manually.
			if ((pFlags & PostProcessSteps.aiProcess_ValidateDataStructure)!=0)
			{
				ValidateDSProcess ds = new ValidateDSProcess();
				ds.executeOnScene (this);
				if (pimpl.mScene == null) {
					return null;
				}
			}
		}
		
		if(AssimpConfig.ASSIMP_BUILD_DEBUG){
			if (pimpl.bExtraVerbose) {
				if(AssimpConfig.ASSIMP_BUILD_NO_VALIDATEDS_PROCESS){
					DefaultLogger.error("Verbose Import is not available due to build settings");
				}
				pFlags |= PostProcessSteps.aiProcess_ValidateDataStructure;
			}
		}else{
			if (pimpl.bExtraVerbose) {
				DefaultLogger.warn("Not a debug build, ignoring extra verbose setting");
			}
		}
		
		Profiler profiler = getPropertyInteger(AssimpConfig.AI_CONFIG_GLOB_MEASURE_TIME, 0) != 0?new Profiler() : null;
		for(int a = 0; a < pimpl.mPostProcessingSteps.size(); a++)	{

			BaseProcess process = pimpl.mPostProcessingSteps.get(a);
			if( process.isActive( pFlags))	{

				if (profiler != null) {
					profiler.beginRegion("postprocess");
				}

				process.executeOnScene	( this );
				pimpl.mProgressHandler.update(-1);

				if (profiler != null) {
					profiler.endRegion("postprocess");
				}
			}
			if(pimpl.mScene == null) {
				break; 
			}
			
			if(AssimpConfig.ASSIMP_BUILD_DEBUG){
				if(AssimpConfig.ASSIMP_BUILD_NO_VALIDATEDS_PROCESS){
					continue;
				}  // no validation
		
				// If the extra verbose mode is active, execute the ValidateDataStructureStep again - after each step
				if (pimpl.bExtraVerbose)	{
					DefaultLogger.debug("Verbose Import: revalidating data structures");
	
					ValidateDSProcess ds = new ValidateDSProcess(); 
					ds.executeOnScene (this);
					if( pimpl.mScene == null)	{
						DefaultLogger.error("Verbose Import: failed to revalidate data structures");
						break; 
					}
				}
			} // ! DEBUG
		}
		
		return pimpl.mScene;
	}

	// -------------------------------------------------------------------
	/** Frees the current scene.<p>
	 *
	 *  The function does nothing if no scene has previously been 
	 *  read via ReadFile(). FreeScene() is called automatically by the
	 *  destructor and ReadFile() itself.  */
	public void freeScene( ){
		pimpl.mScene = null;
		pimpl.mErrorString = "";
	}

	// -------------------------------------------------------------------
	/** Returns an error description of an error that occurred in ReadFile(). <p>
	 *
	 * Returns an empty string if no error occurred.
	 * @return A description of the last error, an empty string if no 
	 *   error occurred. The string is never null.
	 *
	 * @note The returned function remains valid until one of the 
	 * following methods is called: #ReadFile(), #FreeScene(). */
	public String getErrorString(){
		 /* Must remain valid as long as ReadFile() or FreeFile() are not called */
		return pimpl.mErrorString;
	}

	// -------------------------------------------------------------------
	/** Returns the scene loaded by the last successful call to {@link #readFile(String, int)}<p>
	 *
	 * @return Current scene or null if there is currently no scene loaded */
	public Scene getScene(){ return pimpl.mScene;}

	// -------------------------------------------------------------------
	/** Returns the scene loaded by the last successful call to ReadFile()
	 *  and releases the scene from the ownership of the Importer 
	 *  instance. The application is now responsible for deleting the
	 *  scene. Any further calls to GetScene() or GetOrphanedScene()
	 *  will return NULL - until a new scene has been loaded via ReadFile().
	 *
	 * @return Current scene or NULL if there is currently no scene loaded
	 * @note Use this method with maximal caution, and only if you have to.
	 *   By design, aiScene's are exclusively maintained, allocated and
	 *   deallocated by Assimp and no one else. The reasoning behind this
	 *   is the golden rule that deallocations should always be done
	 *   by the module that did the original allocation because heaps
	 *   are not necessarily shared. GetOrphanedScene() enforces you
	 *   to delete the returned scene by yourself, but this will only
	 *   be fine if and only if you're using the same heap as assimp.
	 *   On Windows, it's typically fine provided everything is linked
	 *   against the multithreaded-dll version of the runtime library.
	 *   It will work as well for static linkage with Assimp.*/
	public Scene getOrphanedScene(){
		Scene s = pimpl.mScene;

//		ASSIMP_BEGIN_EXCEPTION_REGION();
		pimpl.mScene = null;
		pimpl.mErrorString = ""; /* reset error string */
//		ASSIMP_END_EXCEPTION_REGION(aiScene*);
		return s;
	}


	// -------------------------------------------------------------------
	/** Returns whether a given file extension is supported by ASSIMP.
	 *
	 * @param szExtension Extension to be checked.
	 *   Must include a trailing dot '.'. Example: ".3ds", ".md3".
	 *   Cases-insensitive.
	 * @return true if the extension is supported, false otherwise */
	public boolean isExtensionSupported(String szExtension){
		return getImporter(szExtension) != null;
	}

	// -------------------------------------------------------------------
	/** Get a full list of all file extensions supported by ASSIMP.<p>
	 *
	 * If a file extension is contained in the list this does of course not
	 * mean that ASSIMP is able to load all files with this extension ---
     * it simply means there is an importer loaded which claims to handle
	 * files with this file extension.
	 * @param szOut String to receive the extension list. 
	 *   Format of the list: "*.3ds;*.obj;*.dae". This is useful for
	 *   use with the WinAPI call GetOpenFileName(Ex). */
	public String getExtensionList(){
		StringBuilder sb = new StringBuilder();
		Set<String> extSet = new HashSet<>();
		
		for(BaseImporter imp : pimpl.mImporter){
			imp.getExtensionList(extSet);
		}
		
		for(String str : extSet){
			sb.append("*.");
			sb.append(str);
			sb.append(':');
		}
		
		return sb.substring(0, sb.length() - 1);
	}

	// -------------------------------------------------------------------
	/** Get the number of importrs currently registered with Assimp. */
	public int getImporterCount(){ return pimpl.mImporter.size();}

	// -------------------------------------------------------------------
	/** Get meta data for the importer corresponding to a specific index..<p>
	*
	*  For the declaration of #aiImporterDesc, include <assimp/importerdesc.h>.
	*  @param index Index to query, must be within [0,GetImporterCount())
	*  @return Importer meta data structure, null if the index does not
	*     exist or if the importer doesn't offer meta information (
	*     importers may do this at the cost of being hated by their peers).*/
	public ImporterDesc getImporterInfo(int index){
		if(index >= pimpl.mImporter.size())
			return null;
		
		return pimpl.mImporter.get(index).getInfo();
	}

	// -------------------------------------------------------------------
	/** Find the importer corresponding to a specific index.
	*
	*  @param index Index to query, must be within [0,GetImporterCount())
	*  @return Importer instance. NULL if the index does not
	*     exist. */
	public BaseImporter getImporter(int index){
		if(index >= pimpl.mImporter.size())
			return null;
		
		return pimpl.mImporter.get(index);
	}

	// -------------------------------------------------------------------
	/** Find the importer corresponding to a specific file extension.
	*
	*  This is quite similar to #IsExtensionSupported except a
	*  BaseImporter instance is returned.
	*  @param szExtension Extension to check for. The following formats
	*    are recognized (BAH being the file extension): "BAH" (comparison
	*    is case-insensitive), ".bah", "*.bah" (wild card and dot
	*    characters at the beginning of the extension are skipped).
	*  @return NULL if no importer is found*/
	public BaseImporter getImporter (String szExtension){
		int index = getImporterIndex(szExtension);
		return index >= 0? getImporter(index) : null;
	}

	// -------------------------------------------------------------------
	/** Find the importer index corresponding to a specific file extension.
	*
	*  @param szExtension Extension to check for. The following formats
	*    are recognized (BAH being the file extension): "BAH" (comparison
	*    is case-insensitive), ".bah", "*.bah" (wild card and dot
	*    characters at the beginning of the extension are skipped).
	*  @return (int)-1 if no importer is found */
	public int getImporterIndex (String szExtension){
		// skip over wildcard and dot characters at string head --
		int index = 0;
//		for(;*szExtension == '*' || *szExtension == '.'; ++szExtension);
		while(index < szExtension.length()){
			char c = szExtension.charAt(index++);
			if(!(c == '*' || c == '.'))
				break;
		}
		
		if(index == szExtension.length())
			return -1;
		
		String ext = index == 0 ? szExtension : szExtension.substring(index);

//		std::string ext(szExtension);
//		if (ext.empty()) {
//			return static_cast<size_t>(-1);
//		}
//		std::transform(ext.begin(),ext.end(), ext.begin(), tolower);
//
//		std::set<std::string> str;
//		for (std::vector<BaseImporter*>::const_iterator i =  pimpl.mImporter.begin();i != pimpl.mImporter.end();++i)	{
//			str.clear();
//
//			(*i).GetExtensionList(str);
//			for (std::set<std::string>::const_iterator it = str.begin(); it != str.end(); ++it) {
//				if (ext == *it) {
//					return std::distance(static_cast< std::vector<BaseImporter*>::const_iterator >(pimpl.mImporter.begin()), i);
//				}
//			}
//		}
	
	    Set<String> str = new HashSet<>();
		int count = pimpl.mImporter.size();
		for(int i = 0; i < count; i++){
			str.clear();
			pimpl.mImporter.get(i).getExtensionList(str);
			for(String s : str){
				if(s.equalsIgnoreCase(ext))
					return i;
			}
		}
		
		return -1;
	
	}

	// -------------------------------------------------------------------
	/** Returns the storage allocated by ASSIMP to hold the scene data
	 * in memory.
	 *
	 * This refers to the currently loaded file, see #ReadFile().
	 * @param in Data structure to be filled. 
	 * @note The returned memory statistics refer to the actual
	 *   size of the use data of the aiScene. Heap-related overhead
	 *   is (naturally) not included.*/
	public void getMemoryRequirements(MemoryInfo in){
		in.reset();;
		Scene mScene = pimpl.mScene;

		// return if we have no scene loaded
		if (pimpl.mScene == null)
			return;

//		in.total = sizeof(Scene); Not include the memory consumed by the class-self.  TODO

		// add all meshes
		for (int i = 0; i < mScene.getNumMeshes();++i)
		{
//			in.meshes += sizeof(aiMesh);Not include the memory consumed by the class-self. TODO
			if (mScene.mMeshes[i].hasPositions()) {
				in.meshes += /*sizeof(aiVector3D)*/AssUtil.SIZE_OF_VEC3 * mScene.mMeshes[i].mNumVertices;
			}

			if (mScene.mMeshes[i].hasNormals()) {
				in.meshes += /*sizeof(aiVector3D)*/AssUtil.SIZE_OF_VEC3 * mScene.mMeshes[i].mNumVertices;
			}

			if (mScene.mMeshes[i].hasTangentsAndBitangents()) {
				in.meshes += /*sizeof(aiVector3D)*/AssUtil.SIZE_OF_VEC3 * mScene.mMeshes[i].mNumVertices * 2;
			}

			for (int a = 0; a < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS;++a) {
				if (mScene.mMeshes[i].hasVertexColors(a)) {
					in.meshes += /*sizeof(aiVector4D)*/AssUtil.SIZE_OF_VEC4 * mScene.mMeshes[i].mNumVertices;
				}
				else break;
			}
			for (int a = 0; a < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS;++a) {
				if (mScene.mMeshes[i].hasTextureCoords(a)) {
					in.meshes += /*sizeof(aiVector3D)*/AssUtil.SIZE_OF_VEC3 * mScene.mMeshes[i].mNumVertices;
				}
				else break;
			}
			if (mScene.mMeshes[i].hasBones()) {
//				in.meshes += sizeof(void*) * mScene.mMeshes[i].mNumBones; Not include the memory consumed by the class-self. TODO
				for (int p = 0; p < mScene.mMeshes[i].getNumBones();++p) {
//					in.meshes += sizeof(aiBone);   Not include the memory consumed by the class-self.
					in.meshes += mScene.mMeshes[i].mBones[p].getNumWeights() * /*sizeof(VertexWeight)*/ VertexWeight.SIZE;
				}
			}
//			in.meshes += (sizeof(aiFace) + 3 * sizeof(unsigned int))*mScene.mMeshes[i].mNumFaces; Not include the memory consumed by the class-self. TODO
		}
	    in.total += in.meshes;

		// add all embedded textures
		for (int i = 0; i < mScene.getNumTextures();++i) {
			Texture pc = mScene.mTextures[i];
//			in.textures += sizeof(aiTexture);   Not include the memory consumed by the class-self. TODO
			if (pc.mHeight > 0) {
				in.textures += 4 * pc.mHeight * pc.mWidth;
			}
			else in.textures += pc.mWidth;
		}
		in.total += in.textures;

		// add all animations
		for (int i = 0; i < mScene.getNumAnimations();++i) {
			Animation pc = mScene.mAnimations[i];
//			in.animations += sizeof(aiAnimation); Not include the memory consumed by the class-self. TODO

			// add all bone anims
			for (int a = 0; a < pc.getNumChannels(); ++a) {
				NodeAnim pc2 = pc.mChannels[i];
//				in.animations += sizeof(aiNodeAnim); Not include the memory consumed by the class-self. TODO
				in.animations += pc2.getNumPositionKeys() * /*sizeof(VectorKey)*/ VectorKey.SIZE;
				in.animations += pc2.getNumScalingKeys() * /*sizeof(aiVectorKey)*/VectorKey.SIZE;
				in.animations += pc2.getNumRotationKeys() * /*sizeof(QuatKey)*/QuatKey.SIZE;
			}
		}
		in.total += in.animations;

		// add all cameras and all lights
		in.total += in.cameras = /*sizeof(Camera)*/Camera._SIZE *  mScene.getNumCameras();
		in.total += in.lights  = /*sizeof(Light)*/Light._SIZE  *  mScene.getNumLights();

		// add all nodes
//		addNodeWeight(in.nodes,mScene.mRootNode);   Not include the memory consumed by the class-self. TODO
		in.total += in.nodes;

		// add all materials
		for (int i = 0; i < mScene.getNumMaterials();++i) {
			Material pc = mScene.mMaterials[i];
//			in.materials += sizeof(aiMaterial); Not include the memory consumed by the class-self. TODO
//			in.materials += pc.mProperties.length * sizeof(void*);

			for (int a = 0; a < pc.mNumProperties;++a) {
				in.materials += pc.mProperties[a]._size();
			}
		}
		in.total += in.materials;
	}

	// -------------------------------------------------------------------
	/** Enables "extra verbose" mode. <p>
	 *
	 * 'Extra verbose' means the data structure is validated after *every*
	 * single post processing step to make sure everyone modifies the data
	 * structure in a well-defined manner. This is a debug feature and not
	 * intended for use in production environments. */
	public void setExtraVerbose(boolean bDo){
		pimpl.bExtraVerbose = bDo;
	}

}
