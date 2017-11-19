package jet.opengl.demos.intel.va;

/**
 * Created by Administrator on 2017/11/19 0019.
 */

public final class VaDirectXCore {

    private static int g_SwapChainWidth;
    private static int g_SwapChainHeight;

    public static void onContextCreated(){

    }

    public static boolean isContextCreated(){
        throw new UnsupportedOperationException();
    }

    public static void helperInitlize(VaDirectXNotifyTarget target){
        if(isContextCreated()){
            target.OnDeviceCreated();
            target.OnResizedSwapChain(g_SwapChainWidth, g_SwapChainHeight);
        }
    }
}
