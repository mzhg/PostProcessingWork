package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2018/1/13.
 */

public final class CPUTLibrary {
    private CPUTLibrary(){}

    public static void InitlizeCPUT(){
        CPUTAssetLibrary.CreateAssetLibrary();
    }

    public static void ReleaseCPUT(){
        CPUTAssetLibrary.DeleteAssetLibrary();
    }
}
