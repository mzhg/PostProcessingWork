package jet.opengl.demos.gpupro.culling;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;

abstract class TransparencyRenderer implements Disposeable {

    GLFuncProvider gl;

    protected void onCreate(){
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }

    protected void onResize(int width, int height){ }

    abstract OITType getType();

    abstract void renderScene(Scene scene);
}
