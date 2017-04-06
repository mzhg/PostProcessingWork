package jet.opengl.postprocessing.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.IntConsumer;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

public final class GLSLUtil {

	static void defaultImplemented(String functionName){
		throw new UnsupportedOperationException(functionName + " is not implemented!");
	}
	
	public static int compileShaderFromSource(CharSequence source, ShaderType type, boolean print_log){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int shader = gl.glCreateShader(type.shader);
		gl.glShaderSource(shader, source);
		gl.glCompileShader(shader);
		
		if (!checkCompileError(shader, type.name(), source,print_log))
	        return 0;
		
		return shader;
	}
	
	public static int createProgramFromShaders(int vertexShader,int tessControlShader, int tessEvalateShader, int geometyShader, int fragmentShader, IntConsumer taskBeforeLink){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		
		int program = gl.glCreateProgram();
		
		if(vertexShader != 0) gl.glAttachShader(program, vertexShader);
		if(tessControlShader != 0) gl.glAttachShader(program, tessControlShader);
		if(tessEvalateShader != 0) gl.glAttachShader(program, tessEvalateShader);
		if(geometyShader != 0) gl.glAttachShader(program, geometyShader);
		if(fragmentShader != 0) gl.glAttachShader(program, fragmentShader);
		
		if(taskBeforeLink != null)
			taskBeforeLink.accept(program);
		
		gl.glLinkProgram(program);
	    
	    try {
			checkLinkError(program);
			return program;
		} catch (GLSLException e) {
			gl.glDeleteProgram(program);
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * Create a computePogram by the computeShader
	 * @param computeShader
	 * @param taskBeforeLink
	 * @return
	 */
	public static int createProgramFromShaders(int computeShader, IntConsumer taskBeforeLink){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int program = gl.glCreateProgram();
		
		gl.glAttachShader(program, computeShader);
		
		if(taskBeforeLink != null)
			taskBeforeLink.accept(program);
		
		gl.glLinkProgram(program);
	    
	    try {
			checkLinkError(program);
			return program;
		} catch (GLSLException e) {
			gl.glDeleteProgram(program);
			e.printStackTrace();
			return 0;
		}
	}
	
	public static void checkLinkError(int program){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		
		int success = gl.glGetProgrami(program, GLenum.GL_LINK_STATUS);
	    if(success == 0){
//	    	int bufLength = gl.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH);
//	    	if(bufLength > 0){
//	    		gl.glGetProgramInfoLog(program)
//	    		String buf = gl.glGetProgramInfoLog(program, bufLength);
//	    		throw new OpenGLException(String.format("compileProgram::Could not link program:\n%s\n", buf));
//	    	}
	    	String log = gl.glGetProgramInfoLog(program);
	    	if(log.length() > 0)
	    		throw new GLSLException(String.format("compileProgram::Could not link program:\n%s\n", log));
	    }
	}
	
	public static boolean checkCompileError(int shader, String shaderName, CharSequence source, boolean print_log){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int compiled = gl.glGetShaderi(shader, GLenum.GL_COMPILE_STATUS);
		
		if(compiled == 0 || print_log){
//			if (compiled == 0) {
//	            throw new OpenGLException("Error compiling shader");
//	        }
//	        int infoLen = gl.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
			String buf = gl.glGetShaderInfoLog(shader);
	        if (buf.length() > 0) {
                StringTokenizer tokens = new StringTokenizer(buf, "\n");
                List<String> oldLines = new ArrayList<String>();
                List<Integer> oldMarks = new ArrayList<Integer>();
                List<Integer> lineCount = new ArrayList<Integer>();
                final String prefix = "ERROR: 0:";
                
                int c = 0;
                while(tokens.hasMoreTokens()){
                	String line = tokens.nextToken();
                	
                	if(line.startsWith(prefix)){
                		int mm = line.indexOf(':', prefix.length());
                		if(mm > 0){
                			String digit = line.substring(prefix.length(), mm);
                			try {
								int count = Integer.parseInt(digit);
								lineCount.add(count);
								oldMarks.add(c++);
								oldLines.add(line.substring(mm + 1));
							} catch (NumberFormatException e) {
								System.out.println(e);
							}
                			
                		}
                	}else{
                		oldLines.add(line);
                	}
                }
                
                if(lineCount.size() > 0){
                	StringBuilder sb;
                	List<CharSequence> lines = splitToLines(source);
                	
                	for(int i = 0; i < lineCount.size(); i++){
                		int lc = lineCount.get(i);
                		CharSequence line = lines.get(lc - 1);
                		c = oldMarks.get(i);
                		oldLines.set(c,lc + " " + oldLines.get(c) + "   <" + line + '>');
                	}
                	
                	int totalLength = 0;
                	for(String l : oldLines){
                		totalLength += l.length();
                	}
                	
                	sb= new StringBuilder(totalLength + oldLines.size() + 16);
                	for(String l : oldLines)
                		sb.append(l).append('\n');
                	
                	buf = sb.toString();
                }
                if(compiled == 0){
                	gl.glDeleteShader(shader);
    	            shader = 0;
                	throw new GLSLException(String.format("%s Shader log:\n%s\n", shaderName, buf));
                }else
                	System.out.println(String.format("%s Shader log:\n%s\n", shaderName, buf));
	        }
	        if (compiled == 0) {
	            return false;
	        }
		}
		
		return true;
	}
	
	public static String getShaderName(int shader){
		switch (shader) {
		case GLenum.GL_VERTEX_SHADER:
			return "Vertex";
		case GLenum.GL_FRAGMENT_SHADER:
			return "Fragment";
		case GLenum.GL_GEOMETRY_SHADER:
			return "Geometry";
		case GLenum.GL_TESS_CONTROL_SHADER:
			return "Tess_Control";
		case GLenum.GL_TESS_EVALUATION_SHADER:
			return "Tess_Evaluation";
		case GLenum.GL_COMPUTE_SHADER:
			return "Compute";
		default:
			return "Unkown";
		}
	}
	
	public static int getUniformLocation(int program, CharSequence name){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int location = gl.glGetUniformLocation(program, name);
		if(location == -1){
			throw new GLSLException("No found uniform location from the name: " + name);
		}
		
		return location;
	}
	
	public static int getAttribLocation(int program, CharSequence name){
		GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
		int location = gl.glGetAttribLocation(program, name);
		if(location == -1){
			throw new GLSLException("No found uniform location from the name: " + name);
		}
		
		return location;
	}
	
	/// --------------------------------- other stuff ----------------------------------------//////
	static List<CharSequence> splitToLines(CharSequence str){
		int pre = 0;
		int length = str.length();
		ArrayList<CharSequence> lines = new ArrayList<CharSequence>(128);
		
		for(int i=0; i < length; i ++){
			char c = str.charAt(i);
			if(c == '\n'){
				lines.add(str.subSequence(pre, i));
//				System.out.println(pre + ", " + i);
				pre = i + 1;
			}
		}
		
		if(pre < length){
			lines.add(str.subSequence(pre, length));
		}
		
		lines.trimToSize();
		return lines;
	}
}
