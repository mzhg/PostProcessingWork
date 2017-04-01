package jet.opengl.postprocessing.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.function.IntConsumer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public class GLSLProgram {

	protected static boolean ms_logAllMissing;
	
	protected boolean m_strict = true;
	protected int m_program;
	protected final LinkedList<IntConsumer> beforeLinks = new LinkedList<IntConsumer>();
	protected final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
	
	/**
	 * Creates and returns a shader object from a array of #{@link ShaderSourceItem}.<br>
	 * @param items
	 * @return a reference to an <code>GLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromShaderItems(ShaderSourceItem[] items){
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromStrings(items, true);
		return prog;
	}
	
	/**
	 * Creates and returns a shader object from a pair of filenames/paths.<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @param strict if set to true, then later calls to retrieve the locations of nonexistent uniforms and 
	 * vertex attributes will log a warning to the output
	 * @return a reference to an <code>GLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromClassFiles(String vertFilename, String fragFilename, boolean strict) throws IOException{
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromClassFiles(vertFilename, fragFilename, strict);
		return prog;
	}
	
	/**
	 * Creates and returns a shader object from a pair of filenames/paths.<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return a reference to an <code>NvGLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromClassFiles(String vertFilename, String fragFilename) throws IOException{
		return createFromFiles(vertFilename, fragFilename, false);
	}
	
	/**
	 * Initializes an existing shader object from a pair of filenames/paths<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @param strict if set to true, then later calls to retrieve the 
	 * locations of nonexistent uniforms and vertex attributes will 
	 * log a warning to the output
	 * @return true on success and false on failure
	 */
	protected void setSourceFromClassFiles(String vertFilename, String fragFilename, boolean strict) throws IOException{
		CharSequence vertSrc = ShaderLoader.loadShaderFile(vertFilename, true);
		CharSequence fragSrc = ShaderLoader.loadShaderFile(fragFilename, true);
		setSourceFromStrings(vertSrc, fragSrc, strict);
	}
	
	/**
	 * Initializes an existing shader object from a pair of filenames/paths<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return true on success and false on failure
	 * @see #setSourceFromFiles(String, String, boolean)
	 */
	protected void setSourceFromClassFiles(String vertFilename, String fragFilename) throws IOException{
		setSourceFromClassFiles(vertFilename, fragFilename, true);
	}
	////------------------
	/**
	 * Creates and returns a shader object from a pair of filenames/paths.<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @param strict if set to true, then later calls to retrieve the locations of nonexistent uniforms and 
	 * vertex attributes will log a warning to the output
	 * @return a reference to an <code>GLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromFiles(String vertFilename, String fragFilename, boolean strict) throws IOException{
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromFiles(vertFilename, fragFilename, strict);
		return prog;
	}
	
	/**
	 * Creates and returns a shader object from a pair of filenames/paths.<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return a reference to an <code>NvGLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromFiles(String vertFilename, String fragFilename) throws IOException{
		return createFromFiles(vertFilename, fragFilename, false);
	}
	
	/**
	 * Initializes an existing shader object from a pair of filenames/paths<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @param strict if set to true, then later calls to retrieve the 
	 * locations of nonexistent uniforms and vertex attributes will 
	 * log a warning to the output
	 * @return true on success and false on failure
	 */
	protected void setSourceFromFiles(String vertFilename, String fragFilename, boolean strict) throws IOException{
		CharSequence vertSrc = ShaderLoader.loadShaderFile(vertFilename, false);
		CharSequence fragSrc = ShaderLoader.loadShaderFile(fragFilename, false);
		setSourceFromStrings(vertSrc, fragSrc, strict);
	}
	
	/**
	 * Initializes an existing shader object from a pair of filenames/paths<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return true on success and false on failure
	 * @see #setSourceFromFiles(String, String, boolean)
	 */
	protected void setSourceFromFiles(String vertFilename, String fragFilename) throws IOException{
		setSourceFromFiles(vertFilename, fragFilename, true);
	}
	
	/**
	 * Creates and returns a shader object from a pair of source strings.
	 * @param vertSrc the string containing the vertex shader source
	 * @param fragSrc the string containing the fragment shader source
	 * @param strict if set to true, then later calls to retrieve the 
	 * locations of nonexistent uniforms and vertex attributes will 
	 * log a warning to the output
	 * @return a reference to an <code>NvGLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromStrings(CharSequence vertSrc, CharSequence fragSrc, boolean strict){
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromStrings(vertSrc, fragSrc, strict);
		return prog;
	}
	
	/**
	 * Creates and returns a shader object from a pair of source strings.
	 * @param vertSrc the string containing the vertex shader source
	 * @param fragSrc the string containing the fragment shader source
	 * @return a reference to an <code>NvGLSLProgram</code> on success and null on failure
	 * @see #createFromStrings(CharSequence, CharSequence, boolean)
	 */
	public static GLSLProgram createFromStrings(CharSequence vertSrc, CharSequence fragSrc){
		return createFromStrings(vertSrc, fragSrc, true);
	}
	
	public void addBeforeLinkTask(IntConsumer r){
		if(r != null)
			beforeLinks.add(r);
	}
	
	public boolean removeBeforeLinkTask(IntConsumer r){
		return beforeLinks.remove(r);
	}
	
	public void clearTasks() { beforeLinks.clear();}
	
	/**
	 * Creates and returns a shader object from a pair of source strings.
	 * @param vertSrc the string containing the vertex shader source
	 * @param fragSrc the string containing the fragment shader source
	 * @param strict if set to true, then later calls to retrieve the 
	 * locations of nonexistent uniforms and vertex attributes will 
	 * log a warning to the output
	 * @return true on success and false on failure
	 */
	protected void setSourceFromStrings(CharSequence vertSrc, CharSequence fragSrc, boolean strict){
		if(m_program != 0){
			gl.glDeleteProgram(m_program);
			m_program = 0;
		}
		
		 m_strict = strict;
		 m_program = compileProgram(vertSrc, fragSrc);
	}
	
	/**
	 * Creates and returns a shader object from an array of #ShaderSourceItem source objects
	 * @param src an array of <code>ShaderSourceItem</code> objects containing the shaders sources to
	 * be loaded.  Unlike the vert/frag-only creation functions, this version can accept additional
	 * shader types such as geometry and tessellation shaders (if supported)
	 * @param strict if set to true, then later calls to retrieve the 
	 * locations of nonexistent uniforms and vertex attributes will 
	 * log a warning to the output
	 * @return true on success and false on failure
	 */
	protected boolean setSourceFromStrings(ShaderSourceItem[] src, boolean strict){
		if(m_program != 0){
			gl.glDeleteProgram(m_program);
			m_program = 0;
		}
		
		 m_strict = strict;

		 m_program = compileProgram(src, src.length);

		 return m_program != 0;
	}
	
	/**
	 * Creates and returns a shader object from an array of #ShaderSourceItem source objects
	 * @param src an array of <code>ShaderSourceItem</code> objects containing the shaders sources to
	 * be loaded.  Unlike the vert/frag-only creation functions, this version can accept additional
	 * shader types such as geometry and tessellation shaders (if supported)
	 * @return true on success and false on failure
	 */
	public boolean setSourceFromStrings(ShaderSourceItem[] src){
		return setSourceFromStrings(src, false);
	}
	
	/** Binds the given shader program as current in the GL context */
	public void enable(){
		gl.glUseProgram(m_program);
	}
	
	/** Unbinds the given shader program from the GL context (binds shader 0) */
	public void disable(){
		gl.glUseProgram(0);
	}
	
	private int compileProgram(CharSequence vsource, CharSequence fsource){
//		int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
//	    int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
//
//	    GL20.glShaderSource(vertexShader, vsource);
//	    GL20.glShaderSource(fragmentShader, fsource);
//
//	    GL20.glCompileShader(vertexShader);
//	    if (!checkCompileError(vertexShader, GL20.GL_VERTEX_SHADER, vsource))
//	        return 0;
//
//	    GL20.glCompileShader(fragmentShader);
//	    if (!checkCompileError(fragmentShader, GL20.GL_FRAGMENT_SHADER, fsource))
//	        return 0;
		
		int vertexShader = GLSLUtil.compileShaderFromSource(vsource, ShaderType.VERTEX, m_strict);
		int fragmentShader = GLSLUtil.compileShaderFromSource(fsource, ShaderType.FRAGMENT, m_strict);

	    int program  = GLSLUtil.createProgramFromShaders(vertexShader, 0, 0, 0, fragmentShader, this::bindAttributes);
	 // can be deleted since the program will keep a reference
	    gl.glDeleteShader(vertexShader);
	    gl.glDeleteShader(fragmentShader);
	    
	    return program;
	}
	
	private void bindAttributes(int programId){
		if(!beforeLinks.isEmpty()){
	    	for(IntConsumer r : beforeLinks){
	    		r.accept(programId);
	    	}
	    }
	}
	
	private int compileProgram(ShaderSourceItem[] src, int count){
		int program = gl.glCreateProgram();

	    int i;
	    for (i = 0; i < count; i++) {
	        int shader = GLSLUtil.compileShaderFromSource(src[i].src, src[i].type, m_strict);
	        gl.glAttachShader(program, shader);
	        // can be deleted since the program will keep a reference
	        gl.glDeleteShader(shader);
	    }

	    return linkProgram(program);
	}
	
	/** Relinks an existing shader program to update based on external changes */
	public void relink(){
		m_program = linkProgram(m_program);
	}
	
	private int linkProgram(int program){
		if(!beforeLinks.isEmpty()){
	    	for(IntConsumer r : beforeLinks){
	    		r.accept(m_program);
	    	}
	    }
		
		// Set the binary retrievable hint and link the program
		if(gl.isSupportExt("ARB_get_program_binary")){
	    	gl.glProgramParameteri(m_program, GLenum.GL_PROGRAM_BINARY_RETRIEVABLE_HINT, 1);
	    }
	    
	    gl.glLinkProgram(program);
	    try {
			GLSLUtil.checkLinkError(program);
		} catch (GLSLException e) {
			gl.glDeleteProgram(program);
			e.printStackTrace();
			return 0;
		}
	    
	    return program;
	}
	
	/**
	 * Retrieve the program binary data. Return null if the video card doesn't support the opengl-extension <i>ARB_get_program_binary</i>
	 * @return
	 */
	public ByteBuffer getProgramBinary(IntBuffer format){
		if(gl.isSupportExt("ARB_get_program_binary")){
//			// Get the expected size of the program binary
//			int binary_size = GL20.glGetProgrami(m_program, ARBGetProgramBinary.GL_PROGRAM_BINARY_LENGTH);
//			
//			// Allocate some memory to store the program binary
//			ByteBuffer binary = BufferUtils.createByteBuffer(binary_size);
			
//			IntBuffer format = GLUtil.getCachedIntBuffer(1);
//			format.put(0).flip();
			// Now retrieve the binary from the program obj ect
			return gl.glGetProgramBinary(m_program, format);
		}
		
		return null;
	}
	
	/**
	 * Returns the GL program object for the shader
	 * @return the GL program object ID
	 */
	public int getProgram(){
		return m_program;
	}
	
	/**
	 * Represents a piece of shader source and the shader type.<p>
	 * Used with creation functions to pass in arrays of multiple shader source types.
	 */
	public static final class ShaderSourceItem{
		/** Shader source code */
		public CharSequence src;
		/** The GL_*_SHADER enum representing the shader type */
		public ShaderType type;
		
		public ShaderSourceItem() {
		}

		public ShaderSourceItem(CharSequence src, ShaderType type) {
			this.src = src;
			this.type = type;
		}
	}

	public void dispose() {
		if(m_program != 0){
			gl.glDeleteProgram(m_program);
			m_program = 0;
		}
	}
}
