package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/16.
 */

public interface VaRenderingModule {

    String GetRenderingModuleTypeName();
    void InternalRenderingModuleSetTypeName(String name);


    default  <T> T SafeCast(){
        try {
            return (T)this;
        }catch (ClassCastException e){
            return null;
        }
    }
}
