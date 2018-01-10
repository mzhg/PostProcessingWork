package jet.opengl.demos.intel.va;

import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.common.GLCheck;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public class VaRenderMaterialDX11 extends VaRenderMaterial implements VaDirectXNotifyTarget {
    private VaRenderMaterialCachedShadersDX11 m_shaders;
    private VaDirectXConstantsBuffer/*< RenderMeshMaterialConstants >*/ m_constantsBuffer = new VaDirectXConstantsBuffer(RenderMeshMaterialConstants.SIZE);
    private int m_storeageIndex;

    protected VaRenderMaterialDX11(VaConstructorParamsBase params) {
        super(params);

        VaDirectXCore.helperInitlize(this);
    }

    @Override
    public void setStorageIndex(int index) {
        m_storeageIndex = index;
    }

    @Override
    public int getStorageIndex() {
        return m_storeageIndex;
    }

    @Override
    public void OnDeviceCreated() {
        m_constantsBuffer.Create();
    }

    @Override
    public void OnDeviceDestroyed() {
        m_constantsBuffer.dispose();
    }

    @Override
    public void ClearVertexBuffers(VaDrawContext drawContext) {
        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11) drawContext.APIContext;
        apiContext.VSSetShader(null);
        apiContext.PSSetShader( null);

        if( (drawContext.PassType == VaRenderPassType.DepthPrePass) || (drawContext.PassType ==VaRenderPassType.GenerateShadowmap) )
        {
            ID3D11InputLayout inputLayout = m_shaders.VS_PosOnly.GetInputLayout();
            inputLayout.unbind();
        }
        else if( (drawContext.PassType == VaRenderPassType.Deferred) || (drawContext.PassType == VaRenderPassType.ForwardOpaque) || (drawContext.PassType == VaRenderPassType.ForwardTransparent) ||
                (drawContext.PassType == VaRenderPassType.ForwardDebugWireframe) )
        {
            ID3D11InputLayout inputLayout = m_shaders.VS_Standard.GetInputLayout();
            inputLayout.unbind();
        }
        else // all other
        {
            assert( false ); // not implemented!
        }
    }

    @Override
    public void UploadToAPIContext(VaDrawContext drawContext) {
        UpdateShaderMacros( );

        if( m_shadersDirty || (m_shaders == null) )
        {
            m_shaders = ((VaRenderMaterialManagerDX11)m_renderMaterialManager).
                    FindOrCreateShaders( m_shaderFileName, m_settings.AlphaTest, m_shaderEntryVS_PosOnly, m_shaderEntryPS_DepthOnly, m_shaderEntryVS_Standard,
                            m_shaderEntryPS_Forward, m_shaderEntryPS_Deferred, m_shaderMacros );

            m_shadersDirty = false;
        }

        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11) drawContext.APIContext;
        apiContext.BeginRender();

        if( (drawContext.PassType == VaRenderPassType.DepthPrePass) || (drawContext.PassType ==VaRenderPassType.GenerateShadowmap) )
        {
//            dx11Context->IASetInputLayout( m_shaders->VS_PosOnly.GetInputLayout( ) );
            ID3D11InputLayout inputLayout = m_shaders.VS_PosOnly.GetInputLayout();
            inputLayout.bind();

//            dx11Context->VSSetShader( m_shaders->VS_PosOnly.GetShader( ), NULL, 0 );
            apiContext.VSSetShader(m_shaders.VS_PosOnly.GetShader( ));

            if( m_settings.AlphaTest )
                apiContext.PSSetShader( m_shaders.PS_DepthOnly.GetShader()/*, NULL, 0*/ );
            else
                apiContext.PSSetShader( null/*, NULL, 0*/ );
        }
        else if( (drawContext.PassType == VaRenderPassType.Deferred) || (drawContext.PassType == VaRenderPassType.ForwardOpaque) || (drawContext.PassType == VaRenderPassType.ForwardTransparent) ||
                (drawContext.PassType == VaRenderPassType.ForwardDebugWireframe) )
        {
            /*dx11Context->IASetInputLayout( m_shaders->VS_Standard.GetInputLayout( ) );*/
            ID3D11InputLayout inputLayout = m_shaders.VS_Standard.GetInputLayout();
            inputLayout.bind();

            apiContext.VSSetShader( m_shaders.VS_Standard.GetShader( )/*, NULL, 0*/ );
            if( drawContext.PassType == VaRenderPassType.Deferred )
                apiContext.PSSetShader( m_shaders.PS_Deferred.GetShader()/*, NULL, 0*/ );
            else
                apiContext.PSSetShader( m_shaders.PS_Forward.GetShader()/*, NULL, 0*/ );
        }
        else // all other
        {
            assert( false ); // not implemented!
        }

        /*ID3D11ShaderResourceView *      materialTextures[6] = { NULL, NULL, NULL, NULL, NULL, NULL };

        if( m_textureAlbedo != nullptr )    materialTextures[0] = m_textureAlbedo->SafeCast<vaTextureDX11*>( )->GetSRV();
        if( m_textureNormalmap != nullptr ) materialTextures[1] = m_textureNormalmap->SafeCast<vaTextureDX11*>( )->GetSRV( );
        if( m_textureSpecular!= nullptr )   materialTextures[2] = m_textureSpecular->SafeCast<vaTextureDX11*>( )->GetSRV( );

        dx11Context->PSSetShaderResources( RENDERMESH_TEXTURE_SLOT0, _countof( materialTextures ), materialTextures );*/
        if(m_textureAlbedo != null) VaDirectXTools.SetToD3DContextAllShaderTypes(((VaTextureDX11)m_textureAlbedo).GetSRV(), VaShaderDefine.RENDERMESH_TEXTURE_SLOT0);
        if(m_textureNormalmap != null) VaDirectXTools.SetToD3DContextAllShaderTypes(((VaTextureDX11)m_textureNormalmap).GetSRV(), VaShaderDefine.RENDERMESH_TEXTURE_SLOT0+1);
        if(m_textureSpecular != null) VaDirectXTools.SetToD3DContextAllShaderTypes(((VaTextureDX11)m_textureSpecular).GetSRV(), VaShaderDefine.RENDERMESH_TEXTURE_SLOT0 + 2);

        GLCheck.checkError();
    }
}
