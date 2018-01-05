package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by mazhen'gui on 2018/1/5.
 */

final class VaRenderingGlobalsDX11 extends VaRenderingGlobals{

    private final ShaderGlobalConstants consts = new ShaderGlobalConstants();
    private VaDirectXConstantsBuffer m_constantsBuffer;
    private int m_storageIndex;

    VaRenderingGlobalsDX11(VaConstructorParamsBase params){
        m_constantsBuffer = new VaDirectXConstantsBuffer(ShaderGlobalConstants.SIZE);
        VaDirectXCore.helperInitlize(this);
    }

    @Override
    public void setStorageIndex(int index) { m_storageIndex = index;}
    @Override
    public int getStorageIndex() { return m_storageIndex;}

    public VaTexture   GetCurrentShaderDebugOutput( ){
        return m_shaderDebugOutputGPUTextures[ (m_frameIndex % c_shaderDebugOutputSyncDelay) ];
    }

    public TextureGL GetCurrentShaderDebugOutputUAV( ){
        return ((VaTextureDX11)GetCurrentShaderDebugOutput()).GetUAV();
    }

    @Override
    public void OnDeviceCreated() {
        super.OnDeviceCreated();
        m_constantsBuffer.Create( );
    }

    @Override
    public void OnDeviceDestroyed() {
        super.OnDeviceDestroyed();

        m_constantsBuffer.dispose( );
    }

    @Override
    public void SetAPIGlobals(VaDrawContext drawContext) {
//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );

        {
            /*ShaderGlobalConstants consts;
            memset( &consts, 0, sizeof(consts) );*/

            final VaViewport viewport = drawContext.APIContext.GetViewport();

            if( drawContext.Lighting != null )
            {
                drawContext.Lighting.UpdateLightingGlobalConstants( drawContext, consts.Lighting );
            }
            else
            {
//                memset( &consts.Lighting, 0, sizeof(consts.Lighting) );
                consts.Lighting.zeros();
            }

            consts.View                             = drawContext.Camera.GetViewMatrix( );
            consts.Proj                             = drawContext.Camera.GetProjMatrix( );

            if( drawContext.PassType == VaRenderPassType.ForwardDebugWireframe )
                drawContext.Camera.GetZOffsettedProjMatrix( consts.Proj, 1.0001f, 0.0001f );

//            consts.ViewProj                         = consts.View * consts.Proj;
            Matrix4f.mul(consts.Proj, consts.View, consts.ViewProj);

            {
                consts.ViewportSize                 .set( (float)viewport.Width, (float)viewport.Height );
                consts.ViewportPixelSize            .set( 1.0f / (float)viewport.Width, 1.0f / (float)viewport.Height );
                consts.ViewportHalfSize             .set( (float)viewport.Width*0.5f, (float)viewport.Height*0.5f );
                consts.ViewportPixel2xSize          .set( 2.0f / (float)viewport.Width, 2.0f / (float)viewport.Height );

                float clipNear  = drawContext.Camera.GetNearPlane();
                float clipFar   = drawContext.Camera.GetFarPlane();
                float depthLinearizeMul = ( clipFar * clipNear ) / ( clipFar - clipNear );
                float depthLinearizeAdd = clipFar / ( clipFar - clipNear );
                consts.DepthUnpackConsts .set( depthLinearizeMul, depthLinearizeAdd );

                // this will only work if xxxPerspectiveFovLH (or equivalent) is used to create projection matrix
                float tanHalfFOVY = (float) Math.tan( Math.toRadians(drawContext.Camera.GetYFOV() * 0.5f) );
                float tanHalfFOVX = tanHalfFOVY * drawContext.Camera.GetAspect();
                consts.CameraTanHalfFOV     .set( tanHalfFOVX, tanHalfFOVY );

                consts.CameraNearFar        .set( clipNear, clipFar );

                consts.Dummy                .set( 0.0f, 0.0f );
            }

            consts.TransparencyPass                 = ( drawContext.PassType == VaRenderPassType.ForwardTransparent )?( 1.0f ) : ( 0.0f );
            consts.WireframePass                    = ( drawContext.PassType == VaRenderPassType.ForwardDebugWireframe )?( 1.0f ) : ( 0.0f );
            consts.Dummy0                           = 0.0f;
            consts.Dummy1                           = 0.0f;

            //consts.ProjToWorld = viewProj.Inverse( );

            m_constantsBuffer.Update( /*dx11Context,*/ consts );
        }

        m_constantsBuffer.SetToD3DContextAllShaderTypes( /*dx11Context,*/ VaShaderDefine.SHADERGLOBAL_CONSTANTSBUFFERSLOT );

        // Samplers
        /*ID3D11SamplerState * samplers[6] =  TODO: how to handle this???
                {
                        vaDirectXTools::GetSamplerStatePointClamp( ),
                        vaDirectXTools::GetSamplerStatePointWrap( ),
                        vaDirectXTools::GetSamplerStateLinearClamp( ),
                        vaDirectXTools::GetSamplerStateLinearWrap( ),
                        vaDirectXTools::GetSamplerStateAnisotropicClamp( ),
                        vaDirectXTools::GetSamplerStateAnisotropicWrap( ),
                };
        VaDirectXTools.SetToD3DContextAllShaderTypes( dx11Context, samplers, SHADERGLOBAL_POINTCLAMP_SAMPLERSLOT, _countof( samplers ) );*/
        // this fills SHADERGLOBAL_POINTCLAMP_SAMPLERSLOT, SHADERGLOBAL_POINTWRAP_SAMPLERSLOT, SHADERGLOBAL_LINEARCLAMP_SAMPLERSLOT, SHADERGLOBAL_LINEARWRAP_SAMPLERSLOT, SHADERGLOBAL_ANISOTROPICCLAMP_SAMPLERSLOT, SHADERGLOBAL_ANISOTROPICWRAP_SAMPLERSLOT

        MarkAPIGlobalsUpdated( drawContext );
    }

    @Override
    public void UpdateDebugOutputFloats(VaDrawContext drawContext) {
        // 1.) queue resource GPU->CPU copy
        VaTextureDX11 src = (VaTextureDX11) ( m_shaderDebugOutputGPUTextures[m_frameIndex % c_shaderDebugOutputSyncDelay]);
        VaTextureDX11 dst = (VaTextureDX11) ( m_shaderDebugOutputCPUTextures[m_frameIndex % c_shaderDebugOutputSyncDelay]);

//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );
//        dx11Context->CopyResource( dst->GetResource(), src->GetResource() );
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glCopyImageSubData(src.GetResource().getTexture(), src.GetResource().getTarget(), 0, 0,0,0,
                              dst.GetResource().getTexture(), dst.GetResource().getTarget(), 0, 0,0,0,
                              dst.GetResource().getWidth(), dst.GetResource().getHeight(), dst.GetResource().getDepth());

        // 2.) get data from oldest (next) CPU resource  TODO
        /*vaTextureDX11 * readTex = vaSaferStaticCast< vaTextureDX11 * >( m_shaderDebugOutputCPUTextures[(int)( (m_frameIndex + 1) % c_shaderDebugOutputSyncDelay )].get( ) );
        ID3D11Texture1D * readTexDX11 = readTex->GetTexture1D();
        D3D11_MAPPED_SUBRESOURCE mapped;
        HRESULT hr = dx11Context->Map( readTexDX11, 0, D3D11_MAP_READ, 0, &mapped );
        if( !FAILED(hr) )
        {
//            new float[ c_shaderDebugFloatOutputCount ];
            memcpy( m_shaderDebugFloats, mapped.pData, sizeof(m_shaderDebugFloats) );
            dx11Context->Unmap( readTexDX11, 0 );
        }
        else
        {
            assert( false );
        }*/
    }
}
