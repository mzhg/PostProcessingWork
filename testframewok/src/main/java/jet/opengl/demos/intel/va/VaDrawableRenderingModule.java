package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public abstract class VaDrawableRenderingModule extends VaRenderingModuleImpl {

    protected VaDrawableRenderingModule() {super();}

    public abstract void  Draw( VaDrawContext drawContext );
}
