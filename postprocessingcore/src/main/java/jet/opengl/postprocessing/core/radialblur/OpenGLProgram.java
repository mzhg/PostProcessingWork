package jet.opengl.postprocessing.core.radialblur;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.shader.GLSLUtil;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/18.
 */

public interface OpenGLProgram extends Disposeable{
    int getProgram();

    default void enable(){
        GLStateTracker.getInstance().bindProgram(getProgram());
    }

    default void disable(){
        GLStateTracker.getInstance().bindProgram(0);
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

        if (!isOptional && result == -1)
        {
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

    default void relink(){
        int programID = getProgram();
        if(programID == 0){
            LogUtil.i(LogUtil.LogType.DEFAULT, "The program \"" + getName() + "\" is NULL, igoren the 'relink' operation.");
            return;
        }
        GLFuncProviderFactory.getGLFuncProvider().glLinkProgram(programID);
        GLSLUtil.checkLinkError(programID);
    }
}
