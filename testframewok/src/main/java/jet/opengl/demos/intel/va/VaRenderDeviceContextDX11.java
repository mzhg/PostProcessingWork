package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Vector4i;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgramPipeline;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.TextureGL;

/**
 * Created by mazhen'gui on 2017/11/20.
 */

final class VaRenderDeviceContextDX11 extends VaRenderDeviceContext implements VaDirectXNotifyTarget {

    private VaDirectXVertexShader   m_fullscreenVS;
    private BufferGL                m_fullscreenVB;
    private BufferGL                m_fullscreenVBDynamic;
    private GLFuncProvider          gl;
    private RenderTargets           m_renderTarget;
    private GLSLProgramPipeline     m_program;
    private final TextureGL[]       m_tempRTVs = new TextureGL[c_maxUAVs + 1];
    private int                     m_storeageIndex;

    private ShaderProgram           m_shaderVS;
    private ShaderProgram           m_shaderPS;

    VaRenderDeviceContextDX11( VaConstructorParamsBase params ){
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
    protected void UpdateViewport() {
        VaViewport  vavp = GetViewport();

        /*D3D11_VIEWPORT viewport;
        viewport.TopLeftX   = (float)vavp.X;
        viewport.TopLeftY   = (float)vavp.Y;
        viewport.Width      = (float)vavp.Width;
        viewport.Height     = (float)vavp.Height;
        viewport.MinDepth   = vavp.MinDepth;
        viewport.MaxDepth   = vavp.MaxDepth;
        m_deviceImmediateContext->RSSetViewports( 1, &viewport );*/
        gl.glViewport(vavp.X, vavp.Y, vavp.Width, vavp.Height);

        Vector4i scissorRect = new Vector4i();
        boolean scissorRectEnabled = GetScissorRect( scissorRect/*, scissorRectEnabled*/ );
        if( scissorRectEnabled )
        {
            /*D3D11_RECT rect;
            rect.left   = scissorRect.x;
            rect.top    = scissorRect.y;
            rect.right  = scissorRect.z;
            rect.bottom = scissorRect.w;
            m_deviceImmediateContext->RSSetScissorRects( 1, &rect );*/
            gl.glEnable(GLenum.GL_SCISSOR_TEST);
            gl.glScissor(scissorRect.x, scissorRect.y, scissorRect.z, scissorRect.w);
        }
        else
        {
            // set the scissor to viewport size, for rasterizer states that have it enabled
            /*D3D11_RECT rect;
            rect.left   = vavp.X;
            rect.top    = vavp.Y;
            rect.right  = vavp.Width;
            rect.bottom = vavp.Height;
            m_deviceImmediateContext->RSSetScissorRects( 1, &rect );*/
        }
    }

    @Override
    protected void UpdateRenderTargetsDepthStencilUAVs() {
        final VaTexture depthStencil = GetDepthStencil();

        TextureGL[] RTVs = m_tempRTVs;
        int count = 0;
        for( int i = 0; i < c_maxRTs; i++ ) {
            RTVs[count++] = (m_outputsState.RenderTargets[i] != null) ? ((VaTextureDX11) (m_outputsState.RenderTargets[i])).GetRTV() : null;
        }
        for( int i = 0; i < c_maxUAVs; i++ ) {
            /*UAVs[i] = (m_outputsState.UAVs[i] != nullptr) ? (vaSaferStaticCast < vaTextureDX11 * > (m_outputsState.UAVs[i].get())->
            GetUAV() ) :(nullptr);*/
            TextureGL uav = m_outputsState.UAVs[i] != null ? ((VaTextureDX11)m_outputsState.UAVs[i]).GetUAV() : null;
            gl.glBindImageTexture(m_outputsState.UAVsStartSlot + i, uav != null ? uav.getTexture() : 0, 0, false, 0, GLenum.GL_READ_WRITE,
                    uav != null ? uav.getFormat() : GLenum.GL_RGBA8);
        }

        if( depthStencil != null  ) {
//            DSV = vaSaferStaticCast<vaTextureDX11*>( depthStencil.get() )->GetDSV( );
            RTVs[count++] = ((VaTextureDX11)depthStencil).GetDSV();
        }

//        m_deviceImmediateContext->OMSetRenderTargetsAndUnorderedAccessViews( m_outputsState.RenderTargetCount, RTVs, DSV, m_outputsState.UAVsStartSlot, m_outputsState.UAVCount, UAVs, m_outputsState.UAVInitialCounts );
        m_renderTarget.bind();
        m_renderTarget.setRenderTextures(RTVs, null);
    }

    @Override
    protected void PrintShaderInfo() {
        if(m_shaderVS != null){
            m_shaderVS.printPrograminfo();
        }

        if(m_shaderPS != null){
            m_shaderPS.printPrograminfo();
        }
    }

    @Override
    public Object GetAPIImmediateContextPtr() {
        return null;
    }

    public static VaRenderDeviceContext Create( /*ID3D11DeviceContext * deviceContext*/ ){
        VaRenderDeviceContext canvas =  // VA_RENDERING_MODULE_CREATE( vaRenderDeviceContext );
                                    VaRenderingModuleRegistrar.CreateModuleTyped("vaRenderDeviceContext", null);

//        vaSaferStaticCast<vaRenderDeviceContextDX11*>( canvas )->Initialize( deviceContext );
        ((VaRenderDeviceContextDX11)canvas).Initialize();

        return canvas;
    }

    public void BeginRender(){
        gl.glUseProgram(0);
        m_program.enable();
    }

    public void VSSetShader(ShaderProgram shader){
        m_program.setVS(shader);
        m_shaderVS = shader;
    }

    public void PSSetShader(ShaderProgram shader){
        m_program.setPS(shader);
        m_shaderPS = shader;
    }

//    ID3D11DeviceContext *       GetDXImmediateContext( ) const                               { return m_deviceImmediateContext; }

    public final void FullscreenPassDraw( /*ID3D11DeviceContext * context,*/ ShaderProgram pixelShader){
        FullscreenPassDraw(pixelShader, null, null, 0, null, 0.0f);
    }

    public void FullscreenPassDraw( /*ID3D11DeviceContext * context,*/ ShaderProgram pixelShader, BlendState blendState /*= vaDirectXTools::GetBS_Opaque()*/,
                                    DepthStencilState  depthStencilState /*= vaDirectXTools::GetDSS_DepthDisabled_NoDepthWrite()*/,
                                    int stencilRef /*= 0*/, ShaderProgram  vertexShader /*= NULL*/, float ZDistance /*= 0.0f*/ ){
        if( vertexShader == null )
            vertexShader = m_fullscreenVS.GetShader();

        // Topology
//        context->IASetPrimitiveTopology( D3D11_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP );

        // Vertex buffer
        /*const u_int stride = sizeof( CommonSimpleVertex );
        UINT offsetInBytes = 0;
        if( ZDistance != 0.0f )
        {
            // update the vertex buffer dynamically - we should do _NO_OVERWRITE but these fullscreen passes don't happen often enough for it to matter
            HRESULT hr;

            D3D11_MAPPED_SUBRESOURCE subResMap;
            hr = context->Map( m_fullscreenVBDynamic, 0, D3D11_MAP_WRITE_DISCARD, 0, &subResMap );

            CommonSimpleVertex  * fsVertices = (CommonSimpleVertex *)subResMap.pData;
            fsVertices[0] = CommonSimpleVertex( -1.0f, 1.0f, ZDistance, 1.0f, 0.0f, 0.0f );
            fsVertices[1] = CommonSimpleVertex( 3.0f, 1.0f, ZDistance, 1.0f, 2.0f, 0.0f );
            fsVertices[2] = CommonSimpleVertex( -1.0f, -3.0f, ZDistance, 1.0f, 0.0f, 2.0f );

            context->Unmap( m_fullscreenVBDynamic, 0 );

            context->IASetVertexBuffers( 0, 1, &m_fullscreenVBDynamic, &stride, &offsetInBytes );
        }
        else
        {
            context->IASetVertexBuffers( 0, 1, &m_fullscreenVB, &stride, &offsetInBytes );
        }*/

        // Shaders and input layout

        /*context->IASetInputLayout( m_fullscreenVS.GetInputLayout() );
        context->VSSetShader( vertexShader, NULL, 0 );

        context->PSSetShader( pixelShader, NULL, 0 );

        float blendFactor[4] = { 0, 0, 0, 0 };
        context->OMSetBlendState( blendState, blendFactor, 0xFFFFFFFF );

        context->OMSetDepthStencilState( depthStencilState, stencilRef );

        context->RSSetState( vaDirectXTools::GetRS_CullNone_Fill() );

        context->Draw( 3, 0 );*/

        GLStateTracker stateTracker = GLStateTracker.getInstance();
        stateTracker.setBlendState(blendState);
        stateTracker.setDepthStencilState(depthStencilState);
        stateTracker.setRasterizerState(null);  // defualt is ok

        m_program.enable();
        m_program.setVS(vertexShader);
        m_program.setPS(pixelShader);

        gl.glBindVertexArray(0);
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

        if(VaRenderingCore.IsCanPrintLog()){
            pixelShader.printPrograminfo();
        }
    }

    private void Initialize( /*ID3D11DeviceContext * deviceContext*/ ){
        m_program = new GLSLProgramPipeline();
        m_renderTarget = new RenderTargets();
        gl = GLFuncProviderFactory.getGLFuncProvider();
    }
    void                        Destroy( ){

    }

    @Override
    public void OnDeviceCreated() {
        m_fullscreenVS = new VaDirectXVertexShader();
        m_fullscreenVS.CreateShaderAndILFromFile("shader_libs/PostProcessingDefaultScreenSpaceVS.vert", "vs_5_0", "main",
                (Macro[]) null, null);
    }

    @Override
    public void OnDeviceDestroyed() {
        SAFE_RELEASE(m_fullscreenVS);  m_fullscreenVS = null;
        SAFE_RELEASE(m_renderTarget);  m_renderTarget = null;
        SAFE_RELEASE(m_program);  m_program = null;
    }

    public void ClearShaders(){
        m_program.setVS(null);
        m_program.setPS(null);
        m_program.setGS(null);
        m_program.setTC(null);
        m_program.setTE(null);
        m_program.setCS(null);
    }

    /*private:
    // vaDirectXNotifyTarget
    virtual void                OnDeviceCreated( ID3D11Device* device, IDXGISwapChain* swapChain );
    virtual void                OnDeviceDestroyed( );*/

}
