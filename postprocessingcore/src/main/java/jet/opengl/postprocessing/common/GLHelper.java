package jet.opengl.postprocessing.common;

import java.nio.IntBuffer;

import jet.opengl.postprocessing.util.BufferUtils;
import jet.opengl.postprocessing.util.Recti;

public final class GLHelper {

    private final static Recti gViewPort = new Recti();
    private final static IntBuffer nativeBuffer = BufferUtils.createIntBuffer(16);

    private static boolean gViewportSaved = false;
    public static void saveViewport(){
        if(!GLFuncProviderFactory.isInitlized())
            return;

        if(gViewportSaved)
            throw new IllegalStateException("saveViewport had called before");

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glGetIntegerv(GLenum.GL_VIEWPORT, nativeBuffer);

        gViewPort.x = nativeBuffer.get(0);
        gViewPort.y = nativeBuffer.get(1);
        gViewPort.width = nativeBuffer.get(2);
        gViewPort.height = nativeBuffer.get(3);

        gViewportSaved = true;
    }

    public static void restoreViewport(){
        if(!GLFuncProviderFactory.isInitlized())
            return;

        if(!gViewportSaved)
            throw new IllegalStateException("There is no viewport saved.");

        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glViewport(gViewPort.x,gViewPort.y,gViewPort.width,gViewPort.height);

        gViewportSaved = false;
    }
}
