package jet.opengl.demos.intel.va;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/11/18.
 */

final class VaLightingDX11 extends VaLighting implements VaDirectXNotifyTarget {

    protected boolean m_helperMacroConstructorCalled;
    private VaDirectXPixelShader                    m_applyDirectionalAmbientPS;
    private VaDirectXPixelShader                    m_applyDirectionalAmbientShadowedPS;

    //vaDirectXPixelShader                    m_applyTonemapPS;

    private boolean                                    m_shadersDirty;

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

        VaDirectXPixelShader   pixelShader = &m_applyDirectionalAmbientPS;

        // Shadows?
        vaSimpleShadowMapDX11 * simpleShadowMapDX11 = NULL;
        if( drawContext.SimpleShadowMap != NULL )
        {
            simpleShadowMapDX11 = vaSaferStaticCast<vaSimpleShadowMapDX11*>( drawContext.SimpleShadowMap );
            pixelShader = &m_applyDirectionalAmbientShadowedPS;
        }

        vaRenderDeviceContextDX11 * apiContext = drawContext.APIContext.SafeCast<vaRenderDeviceContextDX11*>( );
        ID3D11DeviceContext * dx11Context = apiContext->GetDXImmediateContext( );

        // make sure we're not overwriting someone's stuff
        vaDirectXTools::AssertSetToD3DContextAllShaderTypes( dx11Context, ( ID3D11ShaderResourceView* )nullptr, LIGHTING_SLOT0 );
        vaDirectXTools::AssertSetToD3DContextAllShaderTypes( dx11Context, ( ID3D11ShaderResourceView* )nullptr, LIGHTING_SLOT1 );
        vaDirectXTools::AssertSetToD3DContextAllShaderTypes( dx11Context, ( ID3D11ShaderResourceView* )nullptr, LIGHTING_SLOT2 );

        // gbuffer stuff
        vaDirectXTools::SetToD3DContextAllShaderTypes( dx11Context, GBuffer.GetDepthBufferViewspaceLinear( )->SafeCast<vaTextureDX11*>( )->GetSRV( ),   LIGHTING_SLOT0 );
        vaDirectXTools::SetToD3DContextAllShaderTypes( dx11Context, GBuffer.GetAlbedo( )->SafeCast<vaTextureDX11*>( )->GetSRV( ),                       LIGHTING_SLOT1 );
        vaDirectXTools::SetToD3DContextAllShaderTypes( dx11Context, GBuffer.GetNormalMap( )->SafeCast<vaTextureDX11*>( )->GetSRV( ),                    LIGHTING_SLOT2 );

        // draw but only on things that are already in the zbuffer
        apiContext->FullscreenPassDraw( dx11Context, pixelShader->GetShader(), vaDirectXTools::GetBS_Additive(), vaDirectXTools::GetDSS_DepthEnabledG_NoDepthWrite(), 0, nullptr, 1.0f );

        //    Reset, leave stuff clean
        vaDirectXTools::SetToD3DContextAllShaderTypes( dx11Context, (ID3D11ShaderResourceView*) nullptr,  LIGHTING_SLOT0 );
        vaDirectXTools::SetToD3DContextAllShaderTypes( dx11Context, (ID3D11ShaderResourceView*) nullptr,  LIGHTING_SLOT1 );
        vaDirectXTools::SetToD3DContextAllShaderTypes( dx11Context, (ID3D11ShaderResourceView*) nullptr,  LIGHTING_SLOT2 );
    }

    @Override
    public void ApplyDynamicLighting(VaDrawContext drawContext, VaGBuffer GBuffer) {

    }
}
