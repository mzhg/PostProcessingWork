package assimp.common;

import java.util.Map;

/** The BaseProcess defines a common interface for all post processing steps.
 * A post processing step is run after a successful import if the caller
 * specified the corresponding flag when calling readFile(). <p>
 * Enum {@link PostProcessSteps} defines which flags are available. <p>
 * After a successful import the Importer iterates over its internal array 
 * of processes and calls isActive() on each process to evaluate if the step 
 * should be executed. If the function returns true, the class' Execute() 
 * function is called subsequently.
 */
public abstract class BaseProcess {

	/** Shared PostProcess Info*/
	protected Map<String, Object> shared;

	/** Currently active progress handler */
	protected ProgressHandler progress;
	
	/** Constructor to be privately used by Importer */
	public BaseProcess() {}
	
	// -------------------------------------------------------------------
	/** Returns whether the processing step is present in the given flag.
	 * @param pFlags The processing flags the importer was called with. A
	 *   bitwise combination of {@link PostProcessSteps}.
	 * @return true if the process is present in this flag fields, 
	 *   false if not.
	*/
	public abstract boolean isActive(int pFlags);

	// -------------------------------------------------------------------
	/** Check whether this step expects its input vertex data to be 
	 *  in verbose format. */
	public boolean requireVerboseFormat(){ return true;}

	// -------------------------------------------------------------------
	/** Executes the post processing step on the given imported data.
	* The function deletes the scene if the postprocess step fails (
	* the object pointer will be set to null).
	* @param pImp Importer instance (pImp.mScene must be valid)
	*/
	public void executeOnScene( Importer pImp){
		progress = pImp.getProgressHandler();

		setupProperties( pImp );

		// catch exceptions thrown inside the PostProcess-Step
		try
		{
			execute(pImp.pimpl.mScene);

		} catch(Exception err )	{

			// extract error description
			pImp.pimpl.mErrorString = err.getMessage();
			DefaultLogger.error(pImp.pimpl.mErrorString);

			// and kill the partially imported data
//			delete pImp->Pimpl()->mScene;
//			pImp->Pimpl()->mScene = NULL;
			pImp.pimpl.mScene = null;
		}
	}

	// -------------------------------------------------------------------
	/** Called prior to {@link #executeOnScene}.<p>
	* The function is a request to the process to update its configuration
	* basing on the Importer's configuration property list.
	*/
	public void setupProperties(Importer pImp){}

	// -------------------------------------------------------------------
	/** Executes the post processing step on the given imported data.
	* A process should throw an ImportErrorException* if it fails.
	* This method must be implemented by deriving classes.
	* @param pScene The imported data to work at.
	*/
	public abstract void execute(Scene pScene);


	// -------------------------------------------------------------------
	/** Assign a new SharedPostProcessInfo to the step. This object
	 *  allows multiple postprocess steps to share data.
	 * @param sh May be NULL
	*/
	public void SetSharedData(Map<String, Object> sh)	{
		shared = sh;
	}

	// -------------------------------------------------------------------
	/** Get the shared data that is assigned to the step.
	*/
	public Map<String, Object> getSharedData()	{
		return shared;
	}
}
