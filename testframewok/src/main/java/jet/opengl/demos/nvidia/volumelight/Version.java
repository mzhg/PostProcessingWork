package jet.opengl.demos.nvidia.volumelight;

public final class Version {
	
	private Version(){}

	/** Major version of the product, changed manually with every product release with a large new feature set. API refactoring. Breaks backwards compatibility  */
	public static final int MAJOR = 1;
	/** Minor version of the product, changed manually with every minor product release containing some features. Minor API changes */
	public static final int MINOR = 0;
	/** Very minor version of the product, mostly for bug fixing. No API changes, serialization compatible. */
	public static final int BUILD = 0;
	/** Latest Perforce revision of the codebase used for this build. */
	public static final int REVISION = 0;
}
