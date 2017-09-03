package assimp.common;

/** FOR IMPORTER PLUGINS ONLY: Simple exception class to be thrown if an 
 *  unrecoverable error occurs while exporting. Loading APIs return
 *  null instead of a valid Scene then.  */
public class DeadlyExportError extends Error{

	private static final long serialVersionUID = 162477432877208383L;

	public DeadlyExportError(String message) {
		super(message);
	}
	
}
