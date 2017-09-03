package assimp.common;

/**
 * Abstract interface for custom progress report receivers.<p>
 * Each {@link Importer} instance maintains its own <code>ProgressHandler</code>. The default 
 *  implementation provided by Assimp doesn't do anything at all.
 */
public interface ProgressHandler {

	/** Progress callback.
	 *  @param percentage An estimate of the current loading progress,
	 *    in percent. Or -1.f if such an estimate is not available.<p>
	 *
	 *  There are restriction on what you may do from within your 
	 *  implementation of this method: no exceptions may be thrown and no
	 *  non-const #Importer methods may be called. It is 
	 *  not generally possible to predict the number of callbacks 
	 *  fired during a single import.
	 *
	 *  @return Return false to abort loading at the next possible
	 *   occasion (loaders and Assimp are generally allowed to perform
	 *   all needed cleanup tasks prior to returning control to the
	 *   caller). If the loading is aborted, #Importer::ReadFile()
	 *   returns always NULL.<p>
	 *
	 *  <b>NOTE: </b> Currently, percentage is always -1.f because there is 
	 *   no reliable way to compute it.
	 *   */
	boolean update(float percentage /*= -1.f*/);
}
