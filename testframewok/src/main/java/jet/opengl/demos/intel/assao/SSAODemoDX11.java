package jet.opengl.demos.intel.assao;

import jet.opengl.demos.intel.va.VaDirectXConstantsBuffer;
import jet.opengl.demos.intel.va.VaDirectXPixelShader;
import jet.opengl.demos.intel.va.VaDrawContext;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2018/1/3.
 */

public final class SSAODemoDX11 extends ASSAODemo /*implements VaDirectXNotifyTarget*/{

    static {
        ExternalSSAOWrapper.RegisterExternalSSAOWrapperDX11();
    }

    private VaDirectXConstantsBuffer/*< SSAODemoGlobalConstants >*/ m_constantsBuffer;

    private VaDirectXPixelShader m_overlayPS;
    private VaDirectXPixelShader                                    m_opacityGraphPS;
//    std::vector< std::pair< std::string, std::string > >    m_staticShaderMacros;
    private boolean                                                    m_shadersDirty;

    public SSAODemoDX11(){
        m_shadersDirty = true;
    }

    @Override
    protected void initRendering() {
        m_constantsBuffer.Create( );
    }

    private void UpdateShadersIfDirty( VaDrawContext drawContext ){
        Macro[] newStaticShaderMacros = null;

        if( ( drawContext.SimpleShadowMap != null ) && ( drawContext.SimpleShadowMap.GetVolumeShadowMapPlugin( ) != null ) ) {
            newStaticShaderMacros = drawContext.SimpleShadowMap.GetVolumeShadowMapPlugin().GetShaderMacros();
        }

        if( newStaticShaderMacros != m_staticShaderMacros )
        {
            m_staticShaderMacros = newStaticShaderMacros;
            m_shadersDirty = true;
        }

        if( m_shadersDirty )
        {
            m_shadersDirty = false;

//            std::vector<D3D11_INPUT_ELEMENT_DESC> inputElements = vaVertexInputLayoutsDX11::BillboardSpriteVertexDecl( );
//            m_overlayPS.CreateShaderFromFile( L"VSMGlobals.hlsl", "ps_5_0", "AVSMDebugOverlayPS", m_staticShaderMacros );
//            m_opacityGraphPS.CreateShaderFromFile( L"VSMGlobals.hlsl", "ps_5_0", "AVSMDebugOpacityGraphPS", m_staticShaderMacros );
        }
    }

    private void UpdateConstants( VaDrawContext drawContext ){

    }

    @Override
    protected void DrawDebugOverlay(VaDrawContext drawContext) {

    }
}
