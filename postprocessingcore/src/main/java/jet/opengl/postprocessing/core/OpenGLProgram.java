package jet.opengl.postprocessing.core;

import java.nio.ByteBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLAPIVersion;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.shader.ProgramResources;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public interface OpenGLProgram extends Disposeable{
    int getProgram();

    default void enable(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glUseProgram(getProgram());
    }

    default void disable(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glUseProgram(0);
    }

    /**
     * Get the name of the program. In most case, the value used to debug the application.
     * @return
     */
    default String getName(){
        return getClass().getName();
    }

    void setName(String name);

    /**
     * Returns the index containing the named vertex attribute
     * @param attribute the string name of the attribute
     * @param isOptional if true, the function logs an error if the attribute is not found
     * @return the non-negative index of the attribute if found.  -1 if not found
     */
    default int getAttribLocation(String attribute, boolean isOptional){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int result = gl.glGetAttribLocation(getProgram(), attribute);

        if (!isOptional && result == -1)
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("could not find attribute \"%s\" in program \"%s\"", attribute, getName()));
        }

        return result;
    }

    /**
     * Returns the index containing the named vertex attribute
     * @see #getAttribLocation(String, boolean)
     * @param attribute the string name of the attribute
     * @return he non-negative index of the attribute if found.  -1 if not found
     */
    default int getAttribLocation(String attribute){
        return getAttribLocation(attribute, false);
    }

    /**
     * Returns the index containing the named uniform
     * @param uniform the string name of the uniform
     * @return the non-negative index of the uniform if found.  -1 if not found
     */
    default int getUniformLocation(String uniform, boolean isOptional){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int result = gl.glGetUniformLocation(getProgram(), uniform);

        if (!isOptional && result == -1) {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("could not find uniform \"%s\" in program \"%s\"", uniform,  getName()));
        }

        return result;
    }

    /**
     * Returns the index containing the named uniform
     * @param uniform the string name of the uniform
     * @return the non-negative index of the uniform if found.  -1 if not found
     */
    default int getUniformLocation(String uniform){
        return getUniformLocation(uniform, false);
    }

    default void setTextureUniform(String name, int unit){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int loc = getUniformLocation(name);
        if(loc >= 0)
            gl.glUniform1i(loc, unit);
    }

    default void relink(){
        int programID = getProgram();
        if(programID == 0){
            LogUtil.i(LogUtil.LogType.DEFAULT, "The program \"" + getName() + "\" is NULL, igoren the 'relink' operation.");
            return;
        }
        GLFuncProviderFactory.getGLFuncProvider().glLinkProgram(programID);
        GLSLUtil.checkLinkError(programID);
    }

    /**
     * Retrieve the program binary data. Return null if the video card doesn't support the opengl-extension <i>ARB_get_program_binary</i>
     * @param binaryFormat the format of the returned binary data.
     * @return
     */
    @CachaRes
    default ByteBuffer getProgramBinary(int[] binaryFormat){
        int programId = getProgram();
        if(programId == 0){
            LogUtil.i(LogUtil.LogType.DEFAULT, "getProgramBinary:: return null when programId is 0.");
            return null;
        }

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        GLAPIVersion api = gl.getGLAPIVersion();
        if((api.major >= 4 && api.minor >= 1) || (api.ES && api.major >= 3 && api.minor >= 0) || gl.isSupportExt("ARB_get_program_binary")){
//			// Get the expected size of the program binary
            int binary_size = gl.glGetProgrami(programId, GLenum.GL_PROGRAM_BINARY_LENGTH);
//
//			// Allocate some memory to store the program binary
            ByteBuffer binary = BufferUtils.createByteBuffer(binary_size);

//			IntBuffer format = GLUtil.getCachedIntBuffer(1);
//			format.put(0).flip();
            // Now retrieve the binary from the program obj ect
            gl.glGetProgramBinary(programId, new int[1], binaryFormat, binary);
            return binary;
        }

        return null;
    }

    /**
     * Print the program states to the console, the method only used for debugging.
     */
    default void printPrograminfo(){
        System.out.println("----------------------------"+getName() +"-----------------------------------------" );
//        ProgramProperties props = GLSLUtil.getProperties(getProgram());
        ProgramResources resources = GLSLUtil.getProgramResources(getProgram());
        System.out.println(resources);
    }
}
