package jet.opengl.demos.intel.va;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

final class VaLightingDX11 extends VaLighting implements VaDirectXNotifyTarget {

    protected boolean m_helperMacroConstructorCalled;
    private VaDirectXPixelShader                    m_applyDirectionalAmbientPS;
    private VaDirectXPixelShader                    m_applyDirectionalAmbientShadowedPS;

    //vaDirectXPixelShader                    m_applyTonemapPS;
    private int                                     m_storeageIndex;
    private boolean                                 m_shadersDirty;

//        vaDirectXConstantsBuffer< GBufferConstants >
//                                                m_constantsBuffer;

    private final List<Macro> m_staticShaderMacros = new ArrayList<>();
    private String m_shaderFileToUse;

    protected VaLightingDX11(VaConstructorParamsBase params){
        m_shadersDirty = true;

        m_shaderFileToUse                   = "vaLighting.hlsl";
        VaDirectXCore.helperInitlize(this);
        m_helperMacroConstructorCalled = true;
    }

    @Override
    public void setStorageIndex(int index) {
        m_storeageIndex = index;
    }

    @Override
    public int getStorageIndex() {
        return m_storeageIndex;
    }

    public void OnDeviceCreated(){

    }

    public void OnDeviceDestroyed(){

    }

    String GetShaderFilePath( ) { return m_shaderFileToUse;  }

    void UpdateResourcesIfNeeded( VaDrawContext drawContext ){
        if( m_shadersDirty )
        {
            m_shadersDirty = false;

            m_applyDirectionalAmbientPS.CreateShaderFromFile( GetShaderFilePath( ), "ps_5_0", "ApplyDirectionalAmbientPS", m_staticShaderMacros );
            m_applyDirectionalAmbientShadowedPS.CreateShaderFromFile( GetShaderFilePath( ), "ps_5_0", "ApplyDirectionalAmbientShadowedPS", m_staticShaderMacros );

            //m_applyTonemapPS.CreateShaderFromFile( GetShaderFilePath( ), "ps_5_0", "ApplyTonemapPS", m_staticShaderMacros );
        }
    }

    @Override
    public void ApplyDirectionalAmbientLighting(VaDrawContext drawContext, VaGBuffer GBuffer) {
        assert( drawContext.GetRenderingGlobalsUpdated( ) );    if( !drawContext.GetRenderingGlobalsUpdated( ) ) return;
        UpdateResourcesIfNeeded( drawContext );
        assert( !m_shadersDirty );                              if( m_shadersDirty ) return;

        VaDirectXPixelShader   pixelShader = m_applyDirectionalAmbientPS;

        // Shadows?
        VaSimpleShadowMapDX11  simpleShadowMapDX11 = null;
        if( drawContext.SimpleShadowMap != null )
        {
            simpleShadowMapDX11 = (VaSimpleShadowMapDX11) ( drawContext.SimpleShadowMap );
            pixelShader = m_applyDirectionalAmbientShadowedPS;
        }

        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11)drawContext.APIContext;
//        ID3D11DeviceContext * dx11Context = apiContext->GetDXImmediateContext( );

        // make sure we're not overwriting someone's stuff
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( TextureGL )null, VaShaderDefine.LIGHTING_SLOT0 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( TextureGL )null, VaShaderDefine.LIGHTING_SLOT1 );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( TextureGL )null, VaShaderDefine.LIGHTING_SLOT2 );

        // gbuffer stuff
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)GBuffer.GetDepthBufferViewspaceLinear( )).GetSRV( ),   VaShaderDefine.LIGHTING_SLOT0 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)GBuffer.GetAlbedo( )).GetSRV( ),                       VaShaderDefine.LIGHTING_SLOT1 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)GBuffer.GetNormalMap( )).GetSRV( ),                    VaShaderDefine.LIGHTING_SLOT2 );

        // draw but only on things that are already in the zbuffer
        apiContext.FullscreenPassDraw(/* dx11Context,*/ pixelShader.GetShader(), VaDirectXTools.GetBS_Additive(), VaDirectXTools.GetDSS_DepthEnabledG_NoDepthWrite(), 0, null, 1.0f );

        //    Reset, leave stuff clean
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (Texture2D) null,  VaShaderDefine.LIGHTING_SLOT0 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (Texture2D) null,  VaShaderDefine.LIGHTING_SLOT1 );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (Texture2D) null,  VaShaderDefine.LIGHTING_SLOT2 );
    }

    @Override
    public void ApplyDynamicLighting(VaDrawContext drawContext, VaGBuffer GBuffer) {

    }
}
