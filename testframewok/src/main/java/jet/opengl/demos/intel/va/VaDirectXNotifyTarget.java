package jet.opengl.demos.intel.va;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public interface VaDirectXNotifyTarget extends Disposeable {

    default void                  OnDeviceCreated( /*ID3D11Device* device, IDXGISwapChain* swapChain*/ )      {}
    default void                  OnDeviceDestroyed( )                                                    {}
    default void                  OnReleasingSwapChain( )                                                 {}
    default void                  OnResizedSwapChain( /*const DXGI_SURFACE_DESC & backBufferSurfaceDesc*/int width, int height )   {}

    default void dispose(){
        if(VaDirectXCore.isContextCreated()){
            OnReleasingSwapChain();
            OnDeviceDestroyed();
        }
    }
}
