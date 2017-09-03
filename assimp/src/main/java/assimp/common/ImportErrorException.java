package assimp.common;

public class ImportErrorException extends RuntimeException{

	private static final long serialVersionUID = 1627357023748389868L;

	public ImportErrorException() {
		super();
	}

	public ImportErrorException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ImportErrorException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImportErrorException(String message) {
		super(message);
	}

	public ImportErrorException(Throwable cause) {
		super(cause);
	}
}
