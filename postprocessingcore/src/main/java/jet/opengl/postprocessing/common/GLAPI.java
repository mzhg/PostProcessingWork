package jet.opengl.postprocessing.common;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public enum GLAPI {
    ANDROID("jet.opengl.impl.android.AndroidOpenglFuncProvider"),
    LWJGL("jet.opengl.impl.lwjgl3.Lwjgl3OpenglFuncProvider"),
    JOGL("jet.opengl.impl.jogl.JoglOpenglFuncProvider");

    final String className;
    GLAPI(String _className){
        className = _className;
    }
}
