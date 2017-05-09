package jet.opengl.postprocessing.core;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.CachaRes;
import jet.opengl.postprocessing.util.CacheBuffer;
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

    @CachaRes
    default ByteBuffer getProgramBinary(){
        int programId = getProgram();
        if(programId == 0){
            LogUtil.i(LogUtil.LogType.DEFAULT, "getProgramBinary:: return null when programId is 0.");
            return null;
        }

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int formats = gl.glGetInteger(GLenum.GL_NUM_PROGRAM_BINARY_FORMATS);
        int[] binaryFormats = new int[formats];
        IntBuffer _binaryFormats = CacheBuffer.getCachedIntBuffer(formats);
        gl.glGetIntegerv(GLenum.GL_PROGRAM_BINARY_FORMATS, _binaryFormats);
        int len = gl.glGetProgrami(programId, GLenum.GL_PROGRAM_BINARY_LENGTH);
        _binaryFormats.get(binaryFormats);

        ByteBuffer binary = BufferUtils.createByteBuffer(len);
        gl.glGetProgramBinary(programId, new int[len], binaryFormats, binary);
        return  binary;
    }
}
