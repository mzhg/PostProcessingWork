package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public class GLError {
    public static void checkError(){
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        int error = gl.glGetError();
        if(error != 0){
            throw new IllegalStateException(getErrorString(error));
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

    public static void turnOnDebug(){
        System.setProperty("org.lwjgl.util.Debug", "true");
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
