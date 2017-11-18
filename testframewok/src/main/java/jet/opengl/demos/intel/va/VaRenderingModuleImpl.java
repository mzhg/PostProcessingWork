package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaRenderingModuleImpl implements VaRenderingModule {
    private String m_renderingModuleTypeName;

    protected VaRenderingModuleImpl(){
        assert( VaRenderingCore.IsInitialized( ));
    }

    @Override
    public String GetRenderingModuleTypeName() {
        return m_renderingModuleTypeName;
    }

    @Override
    public void InternalRenderingModuleSetTypeName(String name) {
        m_renderingModuleTypeName = name;
    }
}
