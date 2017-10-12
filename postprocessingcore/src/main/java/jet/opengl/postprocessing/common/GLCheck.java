package jet.opengl.postprocessing.common;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public final class GLCheck {

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
            case GLenum.GL_INVALID_FRAMEBUFFER_OPERATION:  return "GL_INVALID_FRAMEBUFFER_OPERATION";

            default:
                return "Unkown Error Code: " + Integer.toHexString(code);
        }
    }

    /**
     * Check the state of the current binded framebuffer.
     */
    public static void checkFramebufferStatus()
    {
        int status;
        String msg = null;
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();

        status = gl.glCheckFramebufferStatus(GLenum.GL_FRAMEBUFFER_EXT);
        switch(status) {
            case GLenum.GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case GLenum.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                msg = "Unsupported framebuffer format";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                msg = "Framebuffer incomplete, missing attachment";
                break;
            //case GL_FRAMEBUFFER_INCOMPLETE_DUPLICATE_ATTACHMENT_EXT:
            //    printf("Framebuffer incomplete, duplicate attachment");
            //   break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                msg = "Framebuffer incomplete attachment";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                msg = "Framebuffer incomplete, attached images must have same dimensions";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                msg = "Framebuffer incomplete, attached images must have same format";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                msg = "Framebuffer incomplete, missing draw buffer";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                msg = "Framebuffer incomplete, missing read buffer";
                break;
            case GLenum.GL_FRAMEBUFFER_UNDEFINED:
                msg = "Framebuffer undefined";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE      :
                msg = "Framebuffer incomplete, attached images must have same samples";
                break;
            case GLenum.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS  :
                msg = "Framebuffer incomplete, attached layed error.";
                break;
            default:
                msg = "Framebuffer incomplete, UNKNOW ERROR(" + Integer.toHexString(status) + ")";
                break;
        }

        if(msg != null){
            int framebuffer = gl.glGetInteger(GLenum.GL_DRAW_FRAMEBUFFER_BINDING);
            throw new IllegalStateException(String.format("Framebuffer(%d) error: %s", framebuffer, msg));
        }

    }
}
