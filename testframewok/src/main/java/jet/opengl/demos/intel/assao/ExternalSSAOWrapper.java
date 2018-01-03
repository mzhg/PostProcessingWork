package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Vector4i;

import jet.opengl.demos.intel.va.VaConstructorParamsBase;
import jet.opengl.demos.intel.va.VaDirectXCore;
import jet.opengl.demos.intel.va.VaDirectXNotifyTarget;
import jet.opengl.demos.intel.va.VaDrawContext;
import jet.opengl.demos.intel.va.VaRenderingModuleImpl;
import jet.opengl.demos.intel.va.VaRenderingModuleRegistrar;
import jet.opengl.demos.intel.va.VaTexture;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/11/23.
 */

final class ExternalSSAOWrapper extends VaRenderingModuleImpl implements VaDirectXNotifyTarget{

    private ExternalSSAOWrapper(VaConstructorParamsBase base){
        VaDirectXCore.helperInitlize(this);
    }

    public void  OnDeviceCreated(  ){}

    public void  OnDeviceDestroyed( ){}

    public void  Draw(VaDrawContext drawContext, Texture2D depthTexture, Texture2D normalmapTexture, boolean blend, Vector4i scissorRect ){}
    public void Draw(VaDrawContext drawContext, VaTexture depthTexture, VaTexture normalmapTexture, boolean blend, Vector4i scissorRect) {}

    static void RegisterExternalSSAOWrapperDX11(){
        VaRenderingModuleRegistrar.RegisterModule( "ExternalSSAOWrapper", (params)-> new ExternalSSAOWrapper(params));
    }
}
