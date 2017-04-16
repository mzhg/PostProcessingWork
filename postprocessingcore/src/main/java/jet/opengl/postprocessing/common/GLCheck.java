package jet.opengl.postprocessing.common;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class GLCheck {

    public static final boolean CHECK;
    public static final boolean INGORE_UNSUPPORT_FUNC;

    static {
        CHECK = Boolean.parseBoolean(System.getProperty("jet.opengl.postprocessing.debug", "false"));
        INGORE_UNSUPPORT_FUNC = Boolean.parseBoolean(System.getProperty("jet.opengl.postprocessing.ingore.unpport.func", "false"));
    }

    public static void checkError(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int error = gl.glGetError();
        if(error != 0){
            throw new IllegalStateException(getErrorString(error));
        }
    }

    public static void printUnsupportFuncError(String errorMsg){
        if(GLCheck.INGORE_UNSUPPORT_FUNC){
            LogUtil.e(LogUtil.LogType.DEFAULT, errorMsg);
        }else{
            throw new UnsupportedOperationException(errorMsg);
        }
    }

    public static void checkError(String msg){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        int error = gl.glGetError();
        if(error != 0){
            String s_msg = String.format("%s [0x%X] at %s", getErrorString(error), error, msg);
            throw new IllegalStateException(s_msg);
        }
    }

    static String getErrorString(int code){
        switch (code) {
            case GLenum.GL_INVALID_ENUM:  return "GL_INVALID_ENUM";
            case GLenum.GL_INVALID_VALUE:  return "GL_INVALID_VALUE";
            case GLenum.GL_INVALID_OPERATION:  return "GL_INVALID_OPERATION";
            case GLenum.GL_NO_ERROR:  return "GL_NO_ERROR";
            case GLenum.GL_STACK_OVERFLOW:  return "GL_STACK_OVERFLOW";
            case GLenum.GL_STACK_UNDERFLOW:  return "GL_STACK_UNDERFLOW";
            case GLenum.GL_OUT_OF_MEMORY:  return "GL_OUT_OF_MEMORY";

            default:
                return "Unkown Error Code: " + Integer.toHexString(code);
        }
    }
}
