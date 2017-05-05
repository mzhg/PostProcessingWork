package jet.opengl.postprocessing.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.core.OpenGLProgram;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.StringUtils;

import static jet.opengl.postprocessing.common.GLenum.GL_NUM_PROGRAM_BINARY_FORMATS;
import static jet.opengl.postprocessing.common.GLenum.GL_PROGRAM_BINARY_FORMATS;

public class GLSLProgram implements OpenGLProgram{

	protected int m_program;
	protected final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
	private AttribBinder[] m_Attribs;

	/**
	 * Creates and returns a shader object from a array of #{@link ShaderSourceItem}.<br>
	 * @param items
	 * @return a reference to an <code>GLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromShaderItems(ShaderSourceItem... items){
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromStrings(items);
		return prog;
	}
	
	/**
	 * Creates and returns a shader object from a pair of filenames/paths.<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return a reference to an <code>GLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromClassFiles(String vertFilename, String fragFilename, Macro...macros) throws IOException{
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromClassFiles(vertFilename, fragFilename, macros);
		return prog;
	}
	
	/**
	 * Initializes an existing shader object from a pair of filenames/paths<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return true on success and false on failure
	 */
	protected void setSourceFromClassFiles(String vertFilename, String fragFilename, Macro...macros) throws IOException{
		CharSequence vertSrc = ShaderLoader.loadShaderFile(vertFilename, true);
		CharSequence fragSrc = ShaderLoader.loadShaderFile(fragFilename, true);
		setSourceFromStrings(vertSrc, fragSrc, macros);
	}

	/**
	 * Initializes an existing shader object from a given binary data <br>
	 * @param binary the binary data used to initlize the program.
	 */
	@CachaRes
	protected void setSourceFromBinary(byte[] binary){
		dispose();

		int formats = gl.glGetInteger(GL_NUM_PROGRAM_BINARY_FORMATS);
		int[] binaryFormats = new int[formats];
		IntBuffer _binaryFormats = CacheBuffer.getCachedIntBuffer(formats);
		gl.glGetIntegerv(GL_PROGRAM_BINARY_FORMATS, _binaryFormats);
		_binaryFormats.get(binaryFormats);

		m_program = gl.glCreateProgram();
//		gl.glProgramBinary(m_program, binaryFormats, CacheBuffer.wrap(binary));
	}

	////------------------
	/**
	 * Creates and returns a shader object from a pair of filenames/paths.<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return a reference to an <code>GLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromFiles(String vertFilename, String fragFilename, Macro...macros) throws IOException{
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromFiles(vertFilename, fragFilename, macros);
		return prog;
	}
	
	/**
	 * Initializes an existing shader object from a pair of filenames/paths<br>
	 * @param vertFilename the filename and partial path to the text file containing the vertex shader source
	 * @param fragFilename the filename and partial path to the text file containing the fragment shader source
	 * @return true on success and false on failure
	 */
	protected void setSourceFromFiles(String vertFilename, String fragFilename, Macro...macros) throws IOException{
		CharSequence vertSrc = ShaderLoader.loadShaderFile(vertFilename, false);
		CharSequence fragSrc = ShaderLoader.loadShaderFile(fragFilename, false);
		setSourceFromStrings(vertSrc, fragSrc, macros);
	}
	
	/**
	 * Creates and returns a shader object from a pair of source strings.
	 * @param vertSrc the string containing the vertex shader source
	 * @param fragSrc the string containing the fragment shader source
	 * @return a reference to an <code>NvGLSLProgram</code> on success and null on failure
	 */
	public static GLSLProgram createFromStrings(CharSequence vertSrc, CharSequence fragSrc, Macro...macros){
		GLSLProgram prog = new GLSLProgram();
		prog.setSourceFromStrings(vertSrc, fragSrc, macros);
		return prog;
	}
	
	/**
	 * Creates and returns a shader object from a pair of source strings.
	 * @param vertSrc the string containing the vertex shader source
	 * @param fragSrc the string containing the fragment shader source
	 * @return true on success and false on failure
	 */
	protected void setSourceFromStrings(CharSequence vertSrc, CharSequence fragSrc, Macro...macros){
		ShaderSourceItem vs_item = new ShaderSourceItem(vertSrc, ShaderType.VERTEX);
		ShaderSourceItem ps_item = new ShaderSourceItem(fragSrc, ShaderType.FRAGMENT);

		vs_item.macros = macros;
		ps_item.macros = macros;

		setSourceFromStrings(vs_item, ps_item);
	}

	static final int GL_VERSIONS[] = {
			110, 120, 130, 140, 150, 300, 330, 400, 410, 420, 430, 440, 450
	};

	static final int GLES_VERSIONS[] = {
			100, 300, 310, 320
	};

	private CharSequence constructSource(ShaderSourceItem item){
		CharSequence source = item.source;

		StringBuilder result = new StringBuilder(source);
		int versionIndex = result.indexOf("#version");

		if(item.attribs != null && item.type == ShaderType.VERTEX){
			m_Attribs = item.attribs;
		}

		if((item.macros == null || item.macros.length == 0) && (versionIndex != -1))
			return source;

		StringBuilder macroString = new StringBuilder();
		GLAPIVersion version = GLFuncProviderFactory.getGLFuncProvider().getGLAPIVersion();

		if(item.compileVersion == 0 && versionIndex == -1){
			item.compileVersion = version.toInt();
		}

		if(item.compileVersion != 0){
			if(!version.ES){
				if(Arrays.binarySearch(GL_VERSIONS, item.compileVersion) < 0)
					throw new IllegalArgumentException("Invalid OpenGL shader language version: " + item.compileVersion);

				macroString.append("#version ").append(Integer.toString(item.compileVersion)).append('\n');
			}else{
				if(Arrays.binarySearch(GLES_VERSIONS, item.compileVersion) < 0)
					throw new IllegalArgumentException("Invalid OpenGL shader language version: " + item.compileVersion);

				macroString.append("#version ").append(Integer.toString(item.compileVersion)).append(" es\n");
			}
		}

		if(item.macros != null){
			for(Macro m : item.macros){
				if(m == null /*|| StringUtils.isEmpty(m.name)*/)
					continue;

				if(!StringUtils.isEmpty(m.key)){
					macroString.append("#define ").append(m.key).append(' ');
				}
				if(m.value != null){
					if(m.value instanceof Boolean){
						Boolean value = (Boolean)m.value;
						macroString.append(value.booleanValue() ? 1 : 0);
					}else{
						macroString.append(m.value.toString());
					}

				}
				macroString.append('\n');
			}
		}

		int versionLineEnd = -1;
		if(versionIndex >= 0){
			versionLineEnd = result.indexOf("\n", versionIndex + 8);
			if(versionLineEnd < 0){
				System.err.println("Error glsl shader source, only contain version tag!");
				versionLineEnd = source.length();
			}
		}

		if(item.compileVersion != 0){  // the macros string contain version tag.
			if(versionIndex >= 0){  // replace the old version.
				result.replace(versionIndex, versionLineEnd, macroString.toString());
			}else{ // insert the macroString to the head of old source.
				result.insert(0, macroString);
			}
		}else{
			result.insert(versionLineEnd + 1, macroString);
		}

		return result;
	}
	
	/**
	 * Creates and returns a shader object from an array of #ShaderSourceItem source objects
	 * @param srcs an array of <code>ShaderSourceItem</code> objects containing the shaders sources to
	 * be loaded.  Unlike the vert/frag-only creation functions, this version can accept additional
	 * shader types such as geometry and tessellation shaders (if supported)
	 * @return true on success and false on failure
	 */
	protected boolean setSourceFromStrings(ShaderSourceItem... srcs){
		if(m_program != 0){
			gl.glDeleteProgram(m_program);
			m_program = 0;
		}
		 m_program = compileProgram(srcs);

		 return m_program != 0;
	}
	
	/** Binds the given shader program as current in the GL context */
	public void enable(){
//		GLStateTracker.getInstance().bindProgram(m_program);
		gl.glUseProgram(m_program);
	}
	
	/** Unbinds the given shader program from the GL context (binds shader 0) */
	public void disable(){
		gl.glUseProgram(0);
	}

	private String m_name = getClass().getSimpleName();
	@Override
	public void setName(String name) {
		m_name = name;
	}

	public String getName(){return m_name;}

	private int compileProgram(ShaderSourceItem[] src){
		int program = gl.glCreateProgram();
		int count = src.length;
//
//	    int i;
//	    for (i = 0; i < count; i++) {
//	        int shader = GLSLUtil.compileShaderFromSource(src[i].src, src[i].type, m_strict);
//	        gl.glAttachShader(program, shader);
//	        // can be deleted since the program will keep a reference
//	        gl.glDeleteShader(shader);
//	    }
//
//	    return linkProgram(program);

		int i;
		for (i = 0; i < count; i++) {
			if(src[i] == null)
				continue;

			CharSequence source = constructSource(src[i]);
			int shader = /*GL20.glCreateShader(src[i].type);
			CharSequence source = constructSource(src[i]);
			GL20.glShaderSource(shader, source);
			GL20.glCompileShader(shader);
			if (!checkCompileError(shader, src[i].type, source))
				return 0;*/
					GLSLUtil.compileShaderFromSource(source, src[i].type, GLCheck.CHECK);

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

	public void setAttribBinding(AttribBinder...bindings){
		m_Attribs = bindings;
	}
	
	private int linkProgram(int program){
		if(m_Attribs != null){
			for(AttribBinder attrib:m_Attribs){
				gl.glBindAttribLocation(program, attrib.index, attrib.attributeName);
			}
		}
		
		// Set the binary retrievable hint and link the program
		if(gl.isSupportExt("ARB_get_program_binary")){
	    	gl.glProgramParameteri(program, GLenum.GL_PROGRAM_BINARY_RETRIEVABLE_HINT, 1);
	    }
	    
	    gl.glLinkProgram(program);
	    try {
			GLSLUtil.checkLinkError(program);
			m_program = program;
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
	public ByteBuffer getProgramBinary(int[] format){
		if(gl.isSupportExt("ARB_get_program_binary")){
//			// Get the expected size of the program binary
			int binary_size = gl.glGetProgrami(m_program, GLenum.GL_PROGRAM_BINARY_LENGTH);
//			
//			// Allocate some memory to store the program binary
			ByteBuffer binary = BufferUtils.createByteBuffer(binary_size);
			
//			IntBuffer format = GLUtil.getCachedIntBuffer(1);
//			format.put(0).flip();
			// Now retrieve the binary from the program obj ect
			gl.glGetProgramBinary(m_program, new int[1], format, binary);
			return binary;
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



	public void dispose() {
		if(m_program != 0){
			gl.glDeleteProgram(m_program);
			m_program = 0;
		}
	}

	public static GLSLProgram createProgram(String vertFile, String fragFile, Macro[] macros){
		return createProgram(vertFile, null, null, null, fragFile, macros);
	}

	public static GLSLProgram createProgram(String vertFile, String gsFile, String fragFile, Macro[] macros){
		return createProgram(vertFile, null, null, gsFile, fragFile, macros);
	}

	public static GLSLProgram createProgram(String vertFile, String tcFile, String teFile, String fragFile, Macro[] macros){
		return createProgram(vertFile, tcFile, teFile, null, fragFile, macros);
	}

	/** Conversion method for creating program object, but not safe. */
	public static GLSLProgram createProgram(String vertFile, String tcFile, String teFile, String gsFile, String fragFile, Macro[] macros){
		ShaderSourceItem vs_item = new ShaderSourceItem();
		if(vertFile != null){
			try {
				vs_item.source = ShaderLoader.loadShaderFile(vertFile, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
			vs_item.macros = macros;
			vs_item.type = ShaderType.VERTEX;
		}

		ShaderSourceItem tc_item = null;
		if(tcFile != null){
			tc_item = new ShaderSourceItem();
			try {
				tc_item.source = ShaderLoader.loadShaderFile(tcFile, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
//			tc_item.compileVersion = Integer.parseInt(GLSLUtil.getGLSLVersion());
			tc_item.macros = macros;
			tc_item.type = ShaderType.TESS_CONTROL;
		}

		ShaderSourceItem te_item = null;
		if(teFile != null){
			te_item = new ShaderSourceItem();
			try {
				te_item.source = ShaderLoader.loadShaderFile(teFile, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
//			te_item.compileVersion = Integer.parseInt(GLSLUtil.getGLSLVersion());
			te_item.macros = macros;
			te_item.type = ShaderType.TESS_EVAL;
		}

		ShaderSourceItem gs_item = null;
		if(gsFile != null){
			gs_item = new ShaderSourceItem();
			try {
				gs_item.source = ShaderLoader.loadShaderFile(gsFile, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
//			gs_item.compileVersion = Integer.parseInt(GLSLUtil.getGLSLVersion());
			gs_item.macros = macros;
			gs_item.type = ShaderType.GEOMETRY;
		}

		ShaderSourceItem ps_item = new ShaderSourceItem();
		if(fragFile != null){
			try {
				ps_item.source = ShaderLoader.loadShaderFile(fragFile, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
//			ps_item.compileVersion = Integer.parseInt(GLSLUtil.getGLSLVersion());
			ps_item.macros = macros;
			ps_item.type = ShaderType.FRAGMENT;
		}


		GLSLProgram program = new GLSLProgram();
		program.setSourceFromStrings(vs_item,tc_item, te_item, gs_item, ps_item);
		return program;
	}
}
