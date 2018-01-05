package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Vector4i;

import jet.opengl.demos.intel.va.VaDirectXCore;
import jet.opengl.demos.intel.va.VaDirectXNotifyTarget;
import jet.opengl.demos.intel.va.VaDrawContext;
import jet.opengl.demos.intel.va.VaRenderingModuleImpl;
import jet.opengl.demos.intel.va.VaTexture;
import jet.opengl.demos.intel.va.VaTextureDX11;
import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/11/23.
 */

final class ASSAOWrapper extends VaRenderingModuleImpl implements VaDirectXNotifyTarget{

    private final ASSAO_Settings m_settings = new ASSAO_Settings();
    private final ASSAO_InputsOpenGL inputs = new ASSAO_InputsOpenGL();
    private int m_storeageIndex;
    private ASSAO_Effect                      m_effect;

    ASSAOWrapper( ){
        VaDirectXCore.helperInitlize(this);
    }

    ASSAO_Settings GetSettings( ) { return m_settings; }

    @Override
    public void OnDeviceCreated() {
        if(m_effect != null)
            throw new IllegalStateException();

        m_effect = new ASSAOGL();
    }

    @Override
    public void OnDeviceDestroyed() {
        if(m_effect != null){
            m_effect.dispose();
            m_effect = null;
        }
    }

    public void Draw(VaDrawContext drawContext, VaTexture depthTexture, boolean blend, VaTexture normalmapTexture, Vector4i scissorRect) {
        inputs.ScissorLeft                  = scissorRect.x;
        inputs.ScissorTop                   = scissorRect.y;
        inputs.ScissorRight                 = scissorRect.z;
        inputs.ScissorBottom                = scissorRect.w;
//        inputs.DeviceContext                = apiContext->GetDXImmediateContext();
        inputs.ProjectionMatrix             .load(drawContext.Camera.GetProjMatrix());
        inputs.ViewportWidth                = drawContext.APIContext.GetViewport().Width;
        inputs.ViewportHeight               = drawContext.APIContext.GetViewport().Height;
        inputs.DepthSRV                     = (Texture2D) ((VaTextureDX11)depthTexture).GetSRV();
        inputs.NormalSRV                    = (normalmapTexture != null)? (Texture2D) ((VaTextureDX11)normalmapTexture).GetSRV() :(null);
        inputs.MatricesRowMajorOrder        = true;
        inputs.DrawOpaque                   = !blend;
        inputs.CameraNear                   = drawContext.Camera.GetNearPlane();
        inputs.CameraFar                    = drawContext.Camera.GetFarPlane();

        m_effect.Draw( m_settings, inputs );
    }

    @Override
    public void setStorageIndex(int index) { m_storeageIndex = index;}

    @Override
    public int getStorageIndex() {
        return m_storeageIndex;
    }
}
