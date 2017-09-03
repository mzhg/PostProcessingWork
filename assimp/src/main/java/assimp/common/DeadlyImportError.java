package assimp.common;

/** FOR IMPORTER PLUGINS ONLY: Simple exception class to be thrown if an 
 *  unrecoverable error occurs while importing. Loading APIs return
 *  null instead of a valid Scene then.  */
public class DeadlyImportError extends Error{

	private static final long serialVersionUID = 162477432877208383L;

	public DeadlyImportError(String message) {
		super(message);
	}

	public DeadlyImportError(Throwable cause) {
		super(cause);
	}
	
}
