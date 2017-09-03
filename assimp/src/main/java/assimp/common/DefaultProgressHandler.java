package assimp.common;

/**
 * The Default implementation of the ProgressHandler, it does nothing.
 */
public class DefaultProgressHandler implements ProgressHandler{

	@Override
	public boolean update(float percentage) {
		return false;
	}

}
