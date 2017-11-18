package jet.opengl.demos.intel.va;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

public class VaLightingDX11 extends VaLighting implements VaDirectXNotifyTarget {

    vaDirectXPixelShader                    m_applyDirectionalAmbientPS;
    vaDirectXPixelShader                    m_applyDirectionalAmbientShadowedPS;

    //vaDirectXPixelShader                    m_applyTonemapPS;

    bool                                    m_shadersDirty;

//        vaDirectXConstantsBuffer< GBufferConstants >
//                                                m_constantsBuffer;

    vector< pair< string, string > >        m_staticShaderMacros;

    wstring                                 m_shaderFileToUse;

    protected VaLightingDX11(){

    }

    @Override
    public void ApplyDirectionalAmbientLighting(VaDrawContext drawContext, VaGBuffer GBuffer) {

    }

    @Override
    public void ApplyDynamicLighting(VaDrawContext drawContext, VaGBuffer GBuffer) {

    }
}
