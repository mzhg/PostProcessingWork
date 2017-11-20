package jet.opengl.demos.intel.va;

import com.nvidia.developer.opengl.utils.Holder;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

final class VaGBufferDX11 extends VaGBuffer implements VaDirectXNotifyTarget {

    private VaDirectXPixelShader                    m_pixelShader;

    private VaDirectXPixelShader                    m_depthToViewspaceLinearPS;
    private VaDirectXPixelShader                    m_debugDrawDepthPS;
    private VaDirectXPixelShader                    m_debugDrawDepthViewspaceLinearPS;
    private VaDirectXPixelShader                    m_debugDrawNormalMapPS;
    private VaDirectXPixelShader                    m_debugDrawAlbedoPS;
    private VaDirectXPixelShader                    m_debugDrawRadiancePS;
    private boolean                                 m_shadersDirty;

//        vaDirectXConstantsBuffer< GBufferConstants >
//                                                m_constantsBuffer;

    private final List<Macro> m_staticShaderMacros = new ArrayList<>();
    private String                                 m_shaderFileToUse;

    protected VaGBufferDX11( VaConstructorParamsBase params ){
        m_shadersDirty = true;

        m_shaderFileToUse                   = "vaGBuffer.hlsl";

        VaDirectXCore.helperInitlize(this);
    }

    static float ReCreateIfNeeded(Holder<VaTexture> inoutTex, int width, int height, int format, float inoutTotalSizeSum )
    {
        inoutTotalSizeSum += width * height * VaTexture.GetPixelSizeInBytes( format );

        int bindFlags = VaTexture.BSF_RenderTarget | VaTexture.BSF_UnorderedAccess | VaTexture.BSF_ShaderResource;

        if( (width == 0) || (height == 0) || (format == VaTexture.Unknown ) )
        {
            CommonUtil.safeRelease(inoutTex.set(null));
        }
        else
        {
            int resourceFormat  = format;
            int srvFormat       = format;
            int rtvFormat       = format;
            int dsvFormat       = VaTexture.Unknown;
            int uavFormat       = format;

            // handle special cases
            if( format == VaTexture.D32_FLOAT )
            {
                bindFlags = (bindFlags & ~(VaTexture.BSF_RenderTarget | VaTexture.BSF_UnorderedAccess)) | VaTexture.BSF_DepthStencil;
                resourceFormat  = VaTexture.R32_TYPELESS;
                srvFormat       = VaTexture.R32_FLOAT;
                rtvFormat       = VaTexture.Unknown;
                dsvFormat       = VaTexture.D32_FLOAT;
                uavFormat       = VaTexture.Unknown;
            }
            else if( format == VaTexture.D24_UNORM_S8_UINT )
            {
                bindFlags = ( bindFlags & ~( VaTexture.BSF_RenderTarget | VaTexture.BSF_UnorderedAccess ) ) | VaTexture.BSF_DepthStencil;
                resourceFormat = VaTexture.R24G8_TYPELESS;
                srvFormat = VaTexture.R24_UNORM_X8_TYPELESS;
                rtvFormat = VaTexture.Unknown;
                dsvFormat = VaTexture.D24_UNORM_S8_UINT;
                uavFormat = VaTexture.Unknown;
            }
            else if( format == VaTexture.R8G8B8A8_UNORM_SRGB )
            {
                //resourceFormat  = VaTexture.R8G8B8A8_TYPELESS;
                //srvFormat       = VaTexture.R8G8B8A8_UNORM_SRGB;
                //rtvFormat       = VaTexture.R8G8B8A8_UNORM_SRGB;
                //uavFormat       = VaTexture.R8G8B8A8_UNORM;
                uavFormat  = VaTexture.Unknown;
                bindFlags &= ~VaTexture.BSF_UnorderedAccess;
            }

            VaTexture _inoutTex = inoutTex.get();
            if( (_inoutTex != null) && (_inoutTex.GetSizeX() == width) && (_inoutTex.GetSizeY()==height) &&
                    (_inoutTex.GetResourceFormat()==resourceFormat) && (_inoutTex.GetSRVFormat()==srvFormat) && (_inoutTex.GetRTVFormat()==rtvFormat) && (_inoutTex.GetDSVFormat()==dsvFormat) && (_inoutTex.GetUAVFormat()==uavFormat) )
                return inoutTotalSizeSum;

            inoutTex.set(VaTexture.Create2D( resourceFormat, width, height, 1, 1, 1, bindFlags, /*vaTextureAccessFlags::None, nullptr, 0, srvFormat, rtvFormat, dsvFormat, uavFormat*/0, null ));
        }

        return inoutTotalSizeSum;
    }

    @Override
    public void UpdateResources(VaDrawContext drawContext, int width, int height) {
        if( width == -1 )   width     = drawContext.APIContext.GetViewport().Width;
        if( height == -1 )  height    = drawContext.APIContext.GetViewport().Height;

        m_resolution.set(width, height);

        float totalSizeInMB = 0.0f;

        ReCreateIfNeeded( m_depthBuffer,                width, height, m_formats.DepthBuffer,                   totalSizeInMB );
        ReCreateIfNeeded( m_depthBufferViewspaceLinear, width, height, m_formats.DepthBufferViewspaceLinear,    totalSizeInMB );
        ReCreateIfNeeded( m_normalMap                 , width, height, m_formats.NormalMap,                     totalSizeInMB );
        ReCreateIfNeeded( m_albedo                    , width, height, m_formats.Albedo,                        totalSizeInMB );
        ReCreateIfNeeded( m_radiance                  , width, height, m_formats.Radiance,                      totalSizeInMB );
        ReCreateIfNeeded( m_outputColor               , width, height, m_formats.OutputColor,                   totalSizeInMB );

        totalSizeInMB /= 1024 * 1024;

        m_debugInfo = String.format( "GBuffer (approx. %.2fMB) ", totalSizeInMB );

        //    vaSimpleShadowMapDX11 * simpleShadowMapDX11 = NULL;
        //    if( drawContext.SimpleShadowMap != NULL )
        //        simpleShadowMapDX11 = vaSaferStaticCast<vaSimpleShadowMapDX11*>( drawContext.SimpleShadowMap );
        //
        //    std::vector< std::pair< std::string, std::string > > newStaticShaderMacros;
        //
        //    if( drawContext.SimpleShadowMap != NULL && drawContext.SimpleShadowMap->GetVolumeShadowMapPlugin( ) != NULL )
        //        newStaticShaderMacros.insert( newStaticShaderMacros.end( ), drawContext.SimpleShadowMap->GetVolumeShadowMapPlugin( )->GetShaderMacros( ).begin( ), drawContext.SimpleShadowMap->GetVolumeShadowMapPlugin( )->GetShaderMacros( ).end( ) );
        //
        //    if( newStaticShaderMacros != m_staticShaderMacros )
        //    {
        //        m_staticShaderMacros = newStaticShaderMacros;
        //
        //        m_shadersDirty = true;
        //    }
        //
        if( m_shadersDirty )
        {
            m_shadersDirty = false;

            m_depthToViewspaceLinearPS.CreateShaderFromFile(        GetShaderFilePath( ), "ps_5_0", "DepthToViewspaceLinearPS",         m_staticShaderMacros );
            m_debugDrawDepthPS.CreateShaderFromFile(                GetShaderFilePath( ), "ps_5_0", "DebugDrawDepthPS",                 m_staticShaderMacros );
            m_debugDrawDepthViewspaceLinearPS.CreateShaderFromFile( GetShaderFilePath( ), "ps_5_0", "DebugDrawDepthViewspaceLinearPS",  m_staticShaderMacros );
            m_debugDrawNormalMapPS.CreateShaderFromFile(            GetShaderFilePath( ), "ps_5_0", "DebugDrawNormalMapPS",             m_staticShaderMacros );
            m_debugDrawAlbedoPS.CreateShaderFromFile(               GetShaderFilePath( ), "ps_5_0", "DebugDrawAlbedoPS",                m_staticShaderMacros );
            m_debugDrawRadiancePS.CreateShaderFromFile(             GetShaderFilePath( ), "ps_5_0", "DebugDrawRadiancePS",              m_staticShaderMacros );
        }
    }

    @Override
    public void RenderDebugDraw(VaDrawContext drawContext) {
        assert( drawContext.GetRenderingGlobalsUpdated( ) );    if( !drawContext.GetRenderingGlobalsUpdated( ) ) return;
        assert( !m_shadersDirty );                              if( m_shadersDirty ) return;

        if( m_debugSelectedTexture == -1 )
            return;

        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11) drawContext.APIContext;
//        ID3D11DeviceContext * dx11Context = apiContext->GetDXImmediateContext( );

        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ (TextureGL)null, VaShaderDefine.GBUFFER_SLOT0 );

        if( m_debugSelectedTexture == 0 )
        {
            VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)m_depthBuffer.get()).GetSRV(), VaShaderDefine.GBUFFER_SLOT0 );
            apiContext.FullscreenPassDraw( /*dx11Context,*/ m_debugDrawDepthPS.GetShader() );
        }
        else if( m_debugSelectedTexture == 1 )
        {
            VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)m_depthBufferViewspaceLinear.get()).GetSRV( ), VaShaderDefine.GBUFFER_SLOT0 );
            apiContext.FullscreenPassDraw( /*dx11Context,*/ m_debugDrawDepthViewspaceLinearPS.GetShader() );
        }
        else if( m_debugSelectedTexture == 2 )
        {
            VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)m_normalMap.get()).GetSRV( ), VaShaderDefine.GBUFFER_SLOT0 );
            apiContext.FullscreenPassDraw( /*dx11Context,*/ m_debugDrawNormalMapPS.GetShader() );
        }
        else if( m_debugSelectedTexture == 3 )
        {
            VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)m_albedo.get()).GetSRV( ), VaShaderDefine.GBUFFER_SLOT0 );
            apiContext.FullscreenPassDraw( /*dx11Context,*/ m_debugDrawAlbedoPS.GetShader() );
        }
        else if( m_debugSelectedTexture == 4 )
        {
            VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)m_radiance.get()).GetSRV( ), VaShaderDefine.GBUFFER_SLOT0 );
            apiContext.FullscreenPassDraw( /*dx11Context,*/ m_debugDrawRadiancePS.GetShader() );
        }

        // Reset
        VaDirectXTools.SetToD3DContextAllShaderTypes(/* dx11Context,*/ (Texture2D) null, VaShaderDefine.GBUFFER_SLOT0 );
    }

    @Override
    public void DepthToViewspaceLinear(VaDrawContext drawContext, VaTexture depthTexture) {
        assert( drawContext.GetRenderingGlobalsUpdated( ) );    if( !drawContext.GetRenderingGlobalsUpdated( ) ) return;
        assert( !m_shadersDirty );                              if( m_shadersDirty ) return;

        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11) drawContext.APIContext/*.SafeCast<vaRenderDeviceContextDX11*>( )*/;
//        ID3D11DeviceContext * dx11Context = apiContext->GetDXImmediateContext( );

        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ ( TextureGL) null, VaShaderDefine.GBUFFER_SLOT0 );

        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ((VaTextureDX11)depthTexture).GetSRV( ), VaShaderDefine.GBUFFER_SLOT0 );
        apiContext.FullscreenPassDraw( /*dx11Context,*/ m_depthToViewspaceLinearPS.GetShader() );

        // Reset
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ ( TextureGL )null, VaShaderDefine.GBUFFER_SLOT0 );
    }

    public void OnDeviceCreated( /*ID3D11Device* device, IDXGISwapChain* swapChain*/ ){

    }
    public void OnDeviceDestroyed( ){
        SAFE_RELEASE(m_pixelShader);  m_pixelShader = null;
        SAFE_RELEASE(m_depthToViewspaceLinearPS);  m_depthToViewspaceLinearPS = null;
        SAFE_RELEASE(m_debugDrawDepthPS);  m_debugDrawDepthPS = null;
        SAFE_RELEASE(m_debugDrawDepthViewspaceLinearPS);  m_debugDrawDepthViewspaceLinearPS = null;
        SAFE_RELEASE(m_debugDrawNormalMapPS);  m_debugDrawNormalMapPS = null;
        SAFE_RELEASE(m_debugDrawAlbedoPS);  m_debugDrawAlbedoPS = null;
        SAFE_RELEASE(m_debugDrawRadiancePS);  m_debugDrawRadiancePS = null;

        SAFE_RELEASE(m_depthBuffer.set(null));
        SAFE_RELEASE(m_depthBufferViewspaceLinear.set(null));
        SAFE_RELEASE(m_normalMap.set(null));
        SAFE_RELEASE(m_albedo.set(null));
        SAFE_RELEASE(m_radiance.set(null));
        SAFE_RELEASE(m_outputColor.set(null));
    }

    @Override
    public void dispose() {
        if(VaDirectXCore.isContextCreated()){
            OnReleasingSwapChain();
            OnDeviceDestroyed();
        }
    }

    String                         GetShaderFilePath( )       { return m_shaderFileToUse;  };

}
