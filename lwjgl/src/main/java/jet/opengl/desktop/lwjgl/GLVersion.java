package jet.opengl.desktop.lwjgl;

import static org.lwjgl.opengl.GL11.GL_EXTENSIONS;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;
import static org.lwjgl.opengl.GL30.glGetStringi;
import static org.lwjgl.opengl.GL32.GL_CONTEXT_PROFILE_MASK;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.lwjgl.opengl.GL11;

public final class GLVersion {

	/** lazy init */
	private static String gl_vendor = null;
	private static String gl_renderer = null;
	private static String gl_version = null;
	private static final Set<String> gl_extensions = new HashSet<String>();
	
	public static String getVendor(){
		if(gl_vendor == null){
			gl_vendor = GL11.glGetString(GL11.GL_VENDOR);
		}
		
		return gl_vendor;
	}
	
	public static String getRenderer(){
		if(gl_renderer == null){
			gl_renderer = GL11.glGetString(GL11.GL_RENDERER);
		}
		
		return gl_renderer;
	}
	
	public static String getVersion(){
		if(gl_version == null){
			gl_version = GL11.glGetString(GL11.GL_VERSION);
		}
		
		return gl_version;
	}
	
	public static String[] getExtensions(){
		String[] result;
		if(gl_extensions.isEmpty()){
			getSupportedExtensions(gl_extensions);
		}
		
		result = gl_extensions.toArray(new String[gl_extensions.size()]);
		return result;
	}
	
	/**
	 * Determine which extensions are available and returns the context profile mask. Helper method to ContextCapabilities.
	 *
	 * @param supported_extensions the Set to fill with the available extension names
	 *
	 * @return the context profile mask, will be 0 for any version < 3.2
	 */
	static int getSupportedExtensions(final Set<String> supported_extensions) {
		// Detect OpenGL version first

		final String version = glGetString(GL_VERSION);
		if ( version == null )
			throw new IllegalStateException("glGetString(GL_VERSION) returned null - possibly caused by missing current context.");

		final StringTokenizer version_tokenizer = new StringTokenizer(version, ". ");
		final String major_string = version_tokenizer.nextToken();
		final String minor_string = version_tokenizer.nextToken();

		int majorVersion = 0;
		int minorVersion = 0;
		try {
			majorVersion = Integer.parseInt(major_string);
			minorVersion = Integer.parseInt(minor_string);
		} catch (NumberFormatException e) {
//			LWJGLUtil.log("The major and/or minor OpenGL version is malformed: " + e.getMessage());
		}

		final int[][] GL_VERSIONS = {
			{ 1, 2, 3, 4, 5 },  // OpenGL 1
			{ 0, 1 },           // OpenGL 2
			{ 0, 1, 2, 3 },     // OpenGL 3
			{ 0, 1, 2, 3 },     // OpenGL 4
		};

		for ( int major = 1; major <= GL_VERSIONS.length; major++ ) {
			int[] minors = GL_VERSIONS[major - 1];
			for ( int minor : minors ) {
				if ( major < majorVersion || (major == majorVersion && minor <= minorVersion) )
					supported_extensions.add("OpenGL" + Integer.toString(major) + Integer.toString(minor));
			}
		}

		int profileMask = 0;

		if ( majorVersion < 3 ) {
			// Parse EXTENSIONS string
			final String extensions_string = glGetString(GL_EXTENSIONS);
			if ( extensions_string == null )
				throw new IllegalStateException("glGetString(GL_EXTENSIONS) returned null - is there a context current?");

			final StringTokenizer tokenizer = new StringTokenizer(extensions_string);
			while ( tokenizer.hasMoreTokens() )
				supported_extensions.add(tokenizer.nextToken());
		} else {
			// Use forward compatible indexed EXTENSIONS
			final int extensionCount = glGetInteger(GL_NUM_EXTENSIONS);

			for ( int i = 0; i < extensionCount; i++ )
				supported_extensions.add(glGetStringi(GL_EXTENSIONS, i));

			// Get the context profile mask for versions >= 3.2
			if ( 3 < majorVersion || 2 <= minorVersion ) {
//				Util.checkGLError(); // Make sure we have no errors up to this point

				try {
					profileMask = glGetInteger(GL_CONTEXT_PROFILE_MASK);
					// Retrieving GL_CONTEXT_PROFILE_MASK may generate an INVALID_OPERATION error on certain implementations, ignore.
					// Happens on pre10.1 ATI drivers, when ContextAttribs.withProfileCompatibility is not used
//					Util.checkGLError();
				} catch (Exception e) {
//					LWJGLUtil.log("Failed to retrieve CONTEXT_PROFILE_MASK");
				}
			}
		}

		return profileMask;
	}
	
	public static boolean isVersionAbove(String version){
		return getVersion().compareTo(version) > 0;
	}
	
	/** Use glfwExtensionSupported instead of this method. */
	@Deprecated
	public static boolean isSupportExt(String ext){
		if(gl_extensions.isEmpty())
			getSupportedExtensions(gl_extensions);
		return gl_extensions.contains(ext);
	}
	
	private GLVersion(){}
}
