package jet.opengl.demos.intel.assao;

import jet.opengl.demos.intel.va.VaDirectXConstantsBuffer;
import jet.opengl.demos.intel.va.VaDirectXCore;
import jet.opengl.demos.intel.va.VaDirectXPixelShader;
import jet.opengl.demos.intel.va.VaDirectXShaderManager;
import jet.opengl.demos.intel.va.VaDrawContext;
import jet.opengl.demos.intel.va.VaRenderDevice;
import jet.opengl.demos.intel.va.VaRenderDeviceDX11;
import jet.opengl.demos.intel.va.VaRenderingCore;
import jet.opengl.demos.intel.va.VaRenderingModuleRegistrar;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2018/1/3.
 */

public final class SSAODemoDX11 extends ASSAODemo /*implements VaDirectXNotifyTarget*/{

    private VaDirectXConstantsBuffer/*< SSAODemoGlobalConstants >*/ m_constantsBuffer;

    private VaDirectXPixelShader m_overlayPS;
    private VaDirectXPixelShader                                    m_opacityGraphPS;
//    std::vector< std::pair< std::string, std::string > >    m_staticShaderMacros;
    private boolean                                                    m_shadersDirty;

    private SSAODemoDX11(){
        // this is for the project-related shaders
//        VaDirectXShaderManager.GetInstance( ).RegisterShaderSearchPath( resourceRoot + L"Media/Shaders" );
//        vaRenderingCore::GetInstance( ).RegisterAssetSearchPath( vaCore::GetExecutableDirectory( ) + L"Media/Textures" );
//        vaRenderingCore::GetInstance( ).RegisterAssetSearchPath( vaCore::GetExecutableDirectory( ) + L"Media/Meshes" );
//        VaDirectXShaderManager.GetInstance( ).RegisterShaderSearchPath( vaCore::GetExecutableDirectory( ) + L"Media/Shaders" );
        
        m_shadersDirty = true;
    }

    public static SSAODemoDX11 newInstance(){
        ExternalSSAOWrapper.RegisterExternalSSAOWrapperDX11();
        VaRenderingModuleRegistrar.RegisterModule("ASSAOWrapper", (params->new ASSAOWrapper()));

        final String resourceRoot = "intel/va/";

        VaRenderingCore.GetInstance().RegisterAssetSearchPath(resourceRoot + "models");
        VaDirectXShaderManager.GetInstance( ).RegisterShaderSearchPath( resourceRoot + "shaders" );

        return new SSAODemoDX11();
    }

    @Override
    protected void initRendering() {
        OnStarted();
//        m_constantsBuffer.Create( );
        VaDirectXCore.GetInstance().PostDeviceCreated();

        VaRenderDevice renderDevice = new VaRenderDeviceDX11();

        Initialize(renderDevice);
        GLCheck.checkError();
    }

    @Override
    protected void update(float dt) {
        OnTick(dt);
    }

    @Override
    public void display() {
        OnRender();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        OnResized(width, height, true);
    }

    @Override
    public void onDestroy() {
//        VaDirectXCore.GetInstance().PostDeviceDestroyed();
        super.onDestroy();
    }

    private void UpdateShadersIfDirty(VaDrawContext drawContext ){
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
