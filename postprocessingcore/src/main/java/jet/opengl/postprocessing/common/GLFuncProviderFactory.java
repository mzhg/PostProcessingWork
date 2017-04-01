package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/3/28.
 */

public class GLFuncProviderFactory {
    private GLFuncProviderFactory(){}

    private static GLFuncProvider g_GLFuncProviderImpl = null;

    public static GLFuncProvider initlizeGLFuncProvider(GLAPI api, Object obj){
        if(g_GLFuncProviderImpl == null){

        }
        return g_GLFuncProviderImpl;
    }

    public static GLFuncProvider getGLFuncProvider(){
        if(g_GLFuncProviderImpl == null)
            throw new NullPointerException("You should call initlizeGLFuncProvider(GLAPI api) before this!");
        return g_GLFuncProviderImpl;
    }
}
