package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;

/**
 * Created by mazhen'gui on 2017/11/21.
 */

public class VaSkyDX11 extends VaSky implements VaDirectXNotifyTarget {
    private VaDirectXVertexShader               m_vertexShader;
    private VaDirectXPixelShader                m_pixelShader;

    private VaDirectXVertexBuffer /*< vaVector3 >*/ m_screenTriangleVertexBuffer = new VaDirectXVertexBuffer(Vector3f.SIZE);
    private VaDirectXVertexBuffer /*< vaVector3 >*/ m_screenTriangleVertexBufferReversedZ = new VaDirectXVertexBuffer(Vector3f.SIZE);
//    private ID3D11DepthStencilState           m_depthStencilState;
    private final DepthStencilState             m_depthStencilState = new DepthStencilState();

    private VaDirectXConstantsBuffer /*< SimpleSkyConstants >*/ m_constantsBuffer = new VaDirectXConstantsBuffer(SimpleSkyConstants.SIZE);
    private final SimpleSkyConstants            m_constants = new SimpleSkyConstants();

    private int m_storeageIndex;

    protected VaSkyDX11( VaConstructorParamsBase params ){
//        m_depthStencilState = NULL;
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
        // Create screen triangle vertex buffer
        {
            final float skyFarZ = 1.0f;
            Vector3f[] screenTriangle = new Vector3f[3];
            screenTriangle[0] = new Vector3f( -1.0f, 1.0f, skyFarZ );
            screenTriangle[1] = new Vector3f( 3.0f, 1.0f, skyFarZ );
            screenTriangle[2] = new Vector3f( -1.0f, -3.0f, skyFarZ );

            m_screenTriangleVertexBuffer.Create( 3, screenTriangle, GLenum.GL_STATIC_DRAW );
        }

        // Create screen triangle vertex buffer
        {
            final float skyFarZ = /*0.0f*/ -1.f;
            Vector3f[] screenTriangle = new Vector3f[3];
            screenTriangle[0] = new Vector3f( -1.0f, 1.0f, skyFarZ );
            screenTriangle[1] = new Vector3f( 3.0f, 1.0f, skyFarZ );
            screenTriangle[2] = new Vector3f( -1.0f, -3.0f, skyFarZ );

            m_screenTriangleVertexBufferReversedZ.Create( 3, screenTriangle, GLenum.GL_STATIC_DRAW );
        }

        // Create depth stencil
        {
            /*CD3D11_DEPTH_STENCIL_DESC desc = CD3D11_DEPTH_STENCIL_DESC( CD3D11_DEFAULT( ) );
            desc.DepthEnable = TRUE;
            desc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ZERO;
            desc.DepthFunc = D3D11_COMPARISON_LESS_EQUAL;
            V( device->CreateDepthStencilState( &desc, &m_depthStencilState ) );*/
            m_depthStencilState.depthEnable = true;
            m_depthStencilState.depthWriteMask = false;
            m_depthStencilState.depthFunc = GLenum.GL_LEQUAL;
        }

        m_constantsBuffer.Create( );
        GLCheck.checkError();

        final int D3D11_APPEND_ALIGNED_ELEMENT = 0;
        final int D3D11_INPUT_PER_VERTEX_DATA = 0;
        D3D11_INPUT_ELEMENT_DESC inputElements[] =
                {
                        VaDirectXTools.CD3D11_INPUT_ELEMENT_DESC( "SV_Position", 0, GLenum.GL_RGB32F, 0,
                                D3D11_APPEND_ALIGNED_ELEMENT, D3D11_INPUT_PER_VERTEX_DATA, 0 ),
                };

        m_vertexShader = new VaDirectXVertexShader();
        m_vertexShader.CreateShaderAndILFromFile( "vaSkyVS.vert", "vs_5_0", "SimpleSkyboxVS", (Macro[]) null, inputElements/*, _countof( inputElements )*/ );

        m_pixelShader = new VaDirectXPixelShader();
        m_pixelShader.CreateShaderFromFile( "vaSkyPS.frag", "ps_5_0", "SimpleSkyboxPS", (Macro[]) null );

        GLCheck.checkError();
    }

    @Override
    public void OnDeviceDestroyed() {
        m_screenTriangleVertexBuffer.dispose( );
        m_screenTriangleVertexBufferReversedZ.dispose( );
//        SAFE_RELEASE( m_depthStencilState );
        m_constantsBuffer.dispose( );
    }

    @Override
    public void Draw(VaDrawContext drawContext) {
        if( drawContext.PassType != VaRenderPassType.ForwardOpaque )
            return;

        VaRenderDeviceContextDX11 apiContext = (VaRenderDeviceContextDX11) drawContext.APIContext;
//        ID3D11DeviceContext * dx11Context = vaSaferStaticCast< vaRenderDeviceContextDX11 * >( &drawContext.APIContext )->GetDXImmediateContext( );

        {
            Settings settings = GetSettings( );
            SimpleSkyConstants consts = m_constants;

            Matrix4f view = drawContext.Camera.GetViewMatrix( );
            Matrix4f proj = drawContext.Camera.GetProjMatrix( );
            /*view.r3 = vaVector4( 0.0f, 0.0f, 0.0f, 1.0f );
            vaMatrix4x4 viewProj = view * proj;
            consts.ProjToWorld = viewProj.Inverse( );*/
            float transX = view.m30;
            float transY = view.m31;
            float transZ = view.m32;
            view.m30 = view.m31 = view.m32 = 0;
            Matrix4f.mul(proj, view, consts.ProjToWorld);
            view.m30 = transX;
            view.m31 = transY;
            view.m32 = transZ;
            consts.ProjToWorld.invert();

//            consts.SunDir = vaVector4( GetSunDir( ), 1.0f );
            consts.SunDir.set(GetSunDir( ));
            consts.SunDir.w = 1.0f;

            consts.SkyColorLowPow = settings.SkyColorLowPow;
            consts.SkyColorLowMul = settings.SkyColorLowMul;
            consts.SkyColorLow.set(settings.SkyColorLow);
            consts.SkyColorHigh.set(settings.SkyColorHigh);
            consts.SunColorPrimary.set(settings.SunColorPrimary);
            consts.SunColorSecondary.set(settings.SunColorSecondary);
            consts.SunColorPrimaryPow = settings.SunColorPrimaryPow;
            consts.SunColorPrimaryMul = settings.SunColorPrimaryMul;
            consts.SunColorSecondaryPow = settings.SunColorSecondaryPow;
            consts.SunColorSecondaryMul = settings.SunColorSecondaryMul;

            m_constantsBuffer.Update( /*dx11Context,*/ consts );
        }

        // make sure we're not overwriting anything else, and set our constant buffers
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ (BufferGL) null, VaShaderDefine.SIMPLESKY_CONSTANTS_BUFFERSLOT );
        m_constantsBuffer.SetToD3DContextAllShaderTypes( /*dx11Context,*/ VaShaderDefine.SIMPLESKY_CONSTANTS_BUFFERSLOT );

        if( drawContext.Camera.GetUseReversedZ() )
            m_screenTriangleVertexBufferReversedZ.SetToD3DContext( /*dx11Context*/ );
        else
            m_screenTriangleVertexBuffer.SetToD3DContext( /*dx11Context*/ );

//        dx11Context->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST );

        /*dx11Context->IASetInputLayout( m_vertexShader.GetInputLayout( ) );*/
        ID3D11InputLayout inputLayout = m_vertexShader.GetInputLayout( );
        inputLayout.bind();

        apiContext.VSSetShader( m_vertexShader.GetShader( )/*, NULL, 0*/ );
        apiContext.PSSetShader( m_pixelShader.GetShader( )/*, NULL, 0*/ );

        final GLStateTracker stateTracker = GLStateTracker.getInstance();

        /*dx11Context->RSSetState( NULL );
        dx11Context->OMSetDepthStencilState( (drawContext.Camera.GetUseReversedZ())?( vaDirectXTools::GetDSS_DepthEnabledGE_NoDepthWrite( ) ):( vaDirectXTools::GetDSS_DepthEnabledLE_NoDepthWrite( ) ), 0 );
        float blendFactor[4] = { 0, 0, 0, 0 };
        dx11Context->OMSetBlendState( NULL, blendFactor, 0xFFFFFFFF );*/
        /*stateTracker.setRasterizerState(null);
        stateTracker.setDepthStencilState((drawContext.Camera.GetUseReversedZ())?( VaDirectXTools.GetDSS_DepthEnabledGE_NoDepthWrite( ) ):( VaDirectXTools.GetDSS_DepthEnabledLE_NoDepthWrite( ) ));
        stateTracker.setBlendState(null);*/
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_DEPTH_TEST);
        gl.glDepthMask(false);

        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);
        inputLayout.unbind();
        gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        gl.glDepthMask(true);

        // make sure nothing messed with our constant buffers and nothing uses them after
        VaDirectXTools.AssertSetToD3DContextAllShaderTypes( /*dx11Context,*/ m_constantsBuffer.GetBuffer( ), VaShaderDefine.SIMPLESKY_CONSTANTS_BUFFERSLOT );
        VaDirectXTools.SetToD3DContextAllShaderTypes( /*dx11Context,*/ (BufferGL)null, VaShaderDefine.SIMPLESKY_CONSTANTS_BUFFERSLOT );
    }
}
