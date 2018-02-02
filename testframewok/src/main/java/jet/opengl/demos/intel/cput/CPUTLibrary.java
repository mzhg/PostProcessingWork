package jet.opengl.demos.intel.cput;

/**
 * Created by mazhen'gui on 2018/1/13.
 */

public final class CPUTLibrary {
    private CPUTLibrary(){}

    public static void InitlizeCPUT(){
        CPUTAssetLibrary.CreateAssetLibrary();

        // If the WARP or Reference rasterizer is being used, the performance is probably terrible.
        // we throw up a dialog right after drawing the loading screen in CPUTCreateWindowAndContext
        // warning about that perf problem

        // call the DeviceCreated callback/backbuffer/etc creation

        CPUTRenderStateBlock pBlock = new CPUTRenderStateBlockDX11();
        pBlock.CreateNativeResources();
        CPUTRenderStateBlock.SetDefaultRenderStateBlock( pBlock );
    }

    public static void ReleaseCPUT(){
        CPUTAssetLibrary.DeleteAssetLibrary();
    }
}
