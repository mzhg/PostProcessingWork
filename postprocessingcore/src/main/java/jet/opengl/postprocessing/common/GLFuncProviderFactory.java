package jet.opengl.postprocessing.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by mazhen'gui on 2017/3/28.
 */

public class GLFuncProviderFactory {
    private GLFuncProviderFactory(){}

    private static GLFuncProvider g_GLFuncProviderImpl = null;

    public static GLFuncProvider initlizeGLFuncProvider(GLAPI api, Object obj){
        if(g_GLFuncProviderImpl == null){
            try {
                Class<?> clazz = Class.forName(api.className);
                if(obj == null){
                    g_GLFuncProviderImpl = (GLFuncProvider) clazz.newInstance();
                }else{
                    Constructor<?>[] constructors = clazz.getConstructors();
                    for(Constructor<?> constructor : constructors){
                        g_GLFuncProviderImpl = (GLFuncProvider) constructor.newInstance(obj);
                        if(g_GLFuncProviderImpl !=null){
                            break;
                        }
                    }
                }

                if(g_GLFuncProviderImpl == null){
                    throw new NullPointerException("Internal Error! Can't initlize the GLfuncProvider implementation.");
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
//                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
//                e.printStackTrace();
            }
        }
        return g_GLFuncProviderImpl;
    }

    public static GLFuncProvider getGLFuncProvider(){
        if(g_GLFuncProviderImpl == null)
            throw new NullPointerException("You should call initlizeGLFuncProvider(GLAPI api) before this!");
        return g_GLFuncProviderImpl;
    }
}
