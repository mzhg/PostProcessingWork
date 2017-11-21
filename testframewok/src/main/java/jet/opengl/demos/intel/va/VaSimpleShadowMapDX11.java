package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.texture.SamplerDesc;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

final class VaSimpleShadowMapDX11 extends VaSimpleShadowMap implements VaDirectXNotifyTarget {

//    static {
//        VaRenderingModuleRegistrar.RegisterModule("vaSimpleShadowMap", (params)->new VaSimpleShadowMapDX11(params));
//    }
    //vaDirectXVertexShader               m_vertexShader;
    //vaDirectXPixelShader                m_pixelShader;

    //vaDirectXVertexBuffer < vaVector3 >
    //    m_screenTriangleVertexBuffer;
    //ID3D11DepthStencilState *           m_depthStencilState;

    private int /*ID3D11SamplerState **/m_shadowCmpSamplerState;

    private RasterizerState /*ID3D11RasterizerState*/ m_rasterizerStateCullNone;
    private RasterizerState /*ID3D11RasterizerState*/ m_rasterizerStateCullCW;
    private RasterizerState /*ID3D11RasterizerState*/ m_rasterizerStateCullCCW;

    private VaDirectXConstantsBuffer /*< ShaderSimpleShadowsGlobalConstants >*/
            m_constantsBuffer = new VaDirectXConstantsBuffer(ShaderSimpleShadowsGlobalConstants.SIZE);

    // this should maybe go into platform independent vaSimpleShadowMap
    private final VaViewport m_prevCanvasVP = new VaViewport();
    private VaTexture      m_prevCanvasRT;
    private VaTexture      m_prevCanvasDS;

    private boolean        m_depthSlopeDirty;
    protected boolean        m_helperMacroConstructorCalled;

    private final ShaderSimpleShadowsGlobalConstants m_globalConstants = new ShaderSimpleShadowsGlobalConstants();

    protected VaSimpleShadowMapDX11(VaConstructorParamsBase params){
        m_prevCanvasRT = null;
        m_prevCanvasDS = null;

        m_shadowCmpSamplerState = 0;

        m_depthSlopeDirty = true;
        m_rasterizerStateCullNone = null;
        m_rasterizerStateCullCW   = null;
        m_rasterizerStateCullCCW  = null;
        m_helperMacroConstructorCalled = true;

        VaDirectXCore.helperInitlize(this);
    }

    @Override
    public void InternalResolutionOrTexelWorldSizeChanged() {
        m_depthSlopeDirty = true;
        m_rasterizerStateCullNone = null;
        m_rasterizerStateCullCW = null;
        m_rasterizerStateCullCCW = null;
    }

    @Override
    public void OnDeviceCreated() {
        // samplers
        {
            /*CD3D11_SAMPLER_DESC desc = CD3D11_SAMPLER_DESC( CD3D11_DEFAULT( ) );

            desc.Filter = D3D11_FILTER_COMPARISON_MIN_MAG_MIP_LINEAR;   // D3D11_FILTER_COMPARISON_ANISOTROPIC
            desc.AddressU = desc.AddressV = desc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP;
            desc.ComparisonFunc = D3D11_COMPARISON_LESS_EQUAL;

            V( device->CreateSamplerState( &desc, &m_shadowCmpSamplerState ) );*/
            SamplerDesc desc = new SamplerDesc();
            desc.minFilter = GLenum.GL_LINEAR_MIPMAP_LINEAR;
            desc.magFilter = GLenum.GL_LINEAR;
            desc.wrapR = desc.wrapS = desc.wrapT = GLenum.GL_CLAMP_TO_EDGE;
            desc.compareFunc = GLenum.GL_LEQUAL;
            desc.compareMode = GLenum.GL_COMPARE_REF_TO_TEXTURE;

            m_shadowCmpSamplerState = SamplerUtils.createSampler(desc);
        }

        m_constantsBuffer.Create( );
        LogUtil.i(LogUtil.LogType.DEFAULT, getClass().getSimpleName() + "::OnDeviceCreated called!");
    }

    @Override
    public void OnDeviceDestroyed() {
        m_constantsBuffer.dispose( );

        assert( m_prevCanvasRT == null );
        assert( m_prevCanvasDS == null );

//        SAFE_RELEASE( m_shadowCmpSamplerState );
        m_shadowCmpSamplerState = 0;

        m_depthSlopeDirty = true;
        /*SAFE_RELEASE( m_rasterizerStateCullNone );
        SAFE_RELEASE( m_rasterizerStateCullCW   );
        SAFE_RELEASE( m_rasterizerStateCullCCW  );*/
    }

    @Override
    public void InternalStartGenerating(VaDrawContext drawContext) {
//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );
        final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if( m_depthSlopeDirty )
        {
            m_depthSlopeDirty = false;

            /*ID3D11Device * device = NULL;
            dx11Context->GetDevice( &device );*/

            /*CD3D11_RASTERIZER_DESC desc = CD3D11_RASTERIZER_DESC( CD3D11_DEFAULT( ) );

            float biasBase  = GetTexelSize().Length() / GetVolume().Extents.z;

            if( SHADERSIMPLESHADOWSGLOBAL_QUALITY == 1 )
                biasBase *= 3.0f;
            else if( SHADERSIMPLESHADOWSGLOBAL_QUALITY == 2 )
                biasBase *= 4.5f;

            desc.DepthBias              = (int)(biasBase * 15000.0f);
            desc.SlopeScaledDepthBias   = biasBase * 1500.0f;
            desc.DepthBiasClamp         = biasBase * 3.0f;

            desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_NONE;

            desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_NONE;
            device->CreateRasterizerState( &desc, &m_rasterizerStateCullNone );*/

            m_rasterizerStateCullNone.fillMode = GLenum.GL_FILL;
            m_rasterizerStateCullNone.cullMode = GLenum.GL_NONE;


            /*desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_BACK;
            desc.FrontCounterClockwise = true;
            device->CreateRasterizerState( &desc, &m_rasterizerStateCullCW );*/
            m_rasterizerStateCullCW.fillMode = GLenum.GL_FILL;
            m_rasterizerStateCullCW.cullMode = GLenum.GL_CULL_FACE;
            m_rasterizerStateCullCW.frontCounterClockwise = true;

            /*desc.FillMode = D3D11_FILL_SOLID;
            desc.CullMode = D3D11_CULL_BACK;
            desc.FrontCounterClockwise = false;
            device->CreateRasterizerState( &desc, &m_rasterizerStateCullCCW );*/
            m_rasterizerStateCullCCW.fillMode = GLenum.GL_FILL;
            m_rasterizerStateCullCCW.cullMode = GLenum.GL_CULL_FACE;
            m_rasterizerStateCullCCW.frontCounterClockwise = false;

//            SAFE_RELEASE( device );
        }

        {
            ShaderSimpleShadowsGlobalConstants consts = m_globalConstants;

            consts.View         .load(GetViewMatrix());
            consts.Proj         .load(GetProjMatrix( ));
            consts.ViewProj     .load(GetViewProjMatrix( ));

//            consts.CameraViewToShadowView               = drawContext.Camera.GetInvViewMatrix( )  * GetViewMatrix( );
//            consts.CameraViewToShadowViewProj           = drawContext.Camera.GetInvViewMatrix( )  * GetViewProjMatrix( );
            Matrix4f.mul(GetViewMatrix( ), drawContext.Camera.GetInvViewMatrix( ), consts.CameraViewToShadowView);
            Matrix4f.mul(GetViewProjMatrix( ), drawContext.Camera.GetInvViewMatrix( ), consts.CameraViewToShadowViewProj);

            /*vaMatrix4x4 toUVNormalizedSpace             = vaMatrix4x4::Scaling( 0.5f, -0.5f, 1.0f ) * vaMatrix4x4::Translation( +0.5f, +0.5f, 0.0f );
            consts.CameraViewToShadowUVNormalizedSpace  = consts.CameraViewToShadowViewProj * toUVNormalizedSpace;*/
            Matrix4f toUVNormalizedSpace = consts.CameraViewToShadowUVNormalizedSpace;
            toUVNormalizedSpace.set(0.5f, 0.0f, 0.0f, 0.0f,
                                    0.0f, 0.5f, 0.0f, 0.0f,
                                    0.0f, 0.0f, 0.5f, 0.0f,
                                    0.5f, 0.5f, 0.5f, 1.0f);
            Matrix4f.mul(toUVNormalizedSpace, consts.CameraViewToShadowViewProj, consts.CameraViewToShadowUVNormalizedSpace);

            consts.ShadowMapRes                         = (float)GetResolution( );
            consts.OneOverShadowMapRes                  = 1.0f / (float)GetResolution();
//            consts.Dummy2                               = 0.0f;
//            consts.Dummy3                               = 0.0f;

            m_constantsBuffer.Update( /*dx11Context,*/ consts );

            // make sure we're not overwriting anything else, and set our constant buffers
//            vaDirectXTools::AssertSetToD3DContextAllShaderTypes( dx11Context, (ID3D11Buffer*)NULL, SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );
            m_constantsBuffer.SetToD3DContextAllShaderTypes( /*dx11Context,*/ VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );
        }

        if( drawContext.PassType != VaRenderPassType.GenerateVolumeShadowmap )
        {

//            VaTextureDX11 shadowMapTextureDX11   = /*vaSaferStaticCast<vaTextureDX11*>( GetShadowMapTexture( ).get() )*/GetShadowMapTexture( ).SafeCast();

            assert( m_prevCanvasRT == null );
            assert( m_prevCanvasDS == null );

            m_prevCanvasVP.Set(drawContext.APIContext.GetViewport());
            m_prevCanvasRT = drawContext.APIContext.GetRenderTarget();
            m_prevCanvasDS = drawContext.APIContext.GetDepthStencil();

            drawContext.APIContext.SetRenderTarget( null, GetShadowMapTexture( ), true );

//            dx11Context.ClearDepthStencilView( shadowMapTextureDX11->GetDSV(), D3D11_CLEAR_DEPTH, 1.0f, 0 );
            gl.glClearDepthf(1.0f);
            gl.glClear(GLenum.GL_DEPTH_BUFFER_BIT);
        }
    }

    @Override
    public void InternalStopGenerating(VaDrawContext drawContext) {
//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );

        // make sure nothing messed with our constant buffers and nothing uses them after
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ m_constantsBuffer.GetBuffer(), VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (BufferGL) null, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );

        if( drawContext.PassType != VaRenderPassType.GenerateVolumeShadowmap )
        {
            assert( drawContext.APIContext.GetRenderTarget( ) == null );
            assert( drawContext.APIContext.GetDepthStencil( ) == GetShadowMapTexture( ) );

            drawContext.APIContext.SetRenderTarget( m_prevCanvasRT, m_prevCanvasDS, false );
            drawContext.APIContext.SetViewport( m_prevCanvasVP );
            m_prevCanvasRT = null;
            m_prevCanvasDS = null;
            m_prevCanvasVP.Reset();
        }
    }

    @Override
    public void InternalStartUsing(VaDrawContext context) {
//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );

        // make sure we're not overwriting anything
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/(BufferGL) null, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );
        m_constantsBuffer.SetToD3DContextAllShaderTypes( /*dx11Context,*/ VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );

        VaTextureDX11 shadowMapTextureDX11 = /*vaSaferStaticCast<vaTextureDX11*>( GetShadowMapTexture( ).get( ) )*/GetShadowMapTexture( ).SafeCast();

        // make sure we're not overwriting anything else, and set our sampler and shadow map texture
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context, (ID3D11SamplerState*)NULL*/0, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CMPSAMPLERSLOT );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ (TextureGL) null, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_TEXTURESLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ m_shadowCmpSamplerState, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CMPSAMPLERSLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ shadowMapTextureDX11.GetSRV(), VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_TEXTURESLOT );

        assert( m_prevCanvasRT == null );
        assert( m_prevCanvasDS == null );
    }

    @Override
    public void InternalStopUsing(VaDrawContext context) {
//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );

//        vaTextureDX11 * shadowMapTextureDX11 = vaSaferStaticCast<vaTextureDX11*>( GetShadowMapTexture( ).get( ) );
        VaTextureDX11 shadowMapTextureDX11 = GetShadowMapTexture().SafeCast();

        // make sure nothing messed with our sampler and shadow map texture and nothing uses them after
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ m_shadowCmpSamplerState, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CMPSAMPLERSLOT );
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ shadowMapTextureDX11.GetSRV( ), VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_TEXTURESLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ 0, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CMPSAMPLERSLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (TextureGL) null, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_TEXTURESLOT );

        // make sure nothing messed with our constant buffers and nothing uses them after
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ m_constantsBuffer.GetBuffer( ), VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (BufferGL) null, VaShaderDefine.SHADERSIMPLESHADOWSGLOBAL_CONSTANTSBUFFERSLOT );
    }
}
