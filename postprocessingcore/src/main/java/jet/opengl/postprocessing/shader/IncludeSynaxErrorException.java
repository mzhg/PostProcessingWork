package jet.opengl.postprocessing.shader;

public class IncludeSynaxErrorException extends RuntimeException{

	private static final long serialVersionUID = -861153313146271911L;

	public IncludeSynaxErrorException() {
		super();
	}

	public IncludeSynaxErrorException(String message) {
		super(message);
	}

	public IncludeSynaxErrorException(Throwable cause) {
		super(cause);
	}

}
