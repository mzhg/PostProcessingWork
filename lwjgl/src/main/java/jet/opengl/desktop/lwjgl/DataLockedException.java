package jet.opengl.desktop.lwjgl;

public class DataLockedException extends RuntimeException{
	private static final long serialVersionUID = 4342022942243586633L;

	public DataLockedException() {
	}

	public DataLockedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DataLockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataLockedException(String message) {
		super(message);
	}

	public DataLockedException(Throwable cause) {
		super(cause);
	}

}
