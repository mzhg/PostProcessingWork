package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public enum GLAPI {
    ANDROID(null),
    LWJGL("jet.opengl.impl.lwjgl3.Lwjgl3OpenglFuncProvider"),
    JOGL(null);

    final String className;
    GLAPI(String _className){
        className = _className;
    }
}
