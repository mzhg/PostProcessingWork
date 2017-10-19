package jet.opengl.demos.intel.antialiasing;

import com.nvidia.developer.opengl.models.GLVAO;
import com.nvidia.developer.opengl.models.ModelGenerator;

import org.lwjgl.util.vector.Readable;
import org.lwjgl.util.vector.Vector4f;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.texture.RenderTargets;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureDataDesc;
import jet.opengl.postprocessing.texture.TextureGL;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.CommonUtil;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/9/23.
 */

final class SMAAEffect implements Disposeable{

    static final int AREATEX_WIDTH = 160;
    static final int AREATEX_HEIGHT = 560;
    static final int AREATEX_PITCH = (AREATEX_WIDTH * 2);
    static final int  AREATEX_SIZE = (AREATEX_HEIGHT * AREATEX_PITCH);

    static final int SEARCHTEX_WIDTH = 64;
    static final int SEARCHTEX_HEIGHT = 16;
    static final int SEARCHTEX_PITCH = SEARCHTEX_WIDTH;
    static final int SEARCHTEX_SIZE = (SEARCHTEX_HEIGHT * SEARCHTEX_PITCH);

    private float					m_ScreenWidth;
    private float					m_ScreenHeight;

    private GLSLProgram         m_SMAAEdgeDetectionProgram;
    private GLSLProgram         m_SMAABlendingWeightCalculationProgram;
    private GLSLProgram         m_SMAANeighborhoodBlendingProgram;

    private BufferGL            m_constantsBuffer;

    private Texture2D m_depthStencilTex;
    private Texture2D            m_workingColorTexture;
    private Texture2D           m_edgesTex;
    private Texture2D            m_blendTex;

    //ID3D11Texture2D*            m_areaTex;
    private Texture2D   m_areaTexSRV;

    //ID3D11Texture2D*            m_searchTex;
    private Texture2D   m_searchTexSRV;

//    ID3D11SamplerState*         m_samplerState;
//    class SMAAEffect_Quad *     m_quad;
    private GLVAO  m_quad;
    private GLFuncProvider gl;
    private final SMAAConstants m_constants = new SMAAConstants();
    private RenderTargets m_renderTargets;
    private boolean m_prinOnce = false;

    void OnCreate(int width, int height){
        final Macro[] macros = {
                new Macro("SMAA_RT_METRICS", "float4(1.0 / " + width + ", 1.0 / " + height + ", " + width + ", " + height + ")"),
                new Macro("SMAA_PRESET_HIGH", 1),
//                new Macro("SMAA_PREDICATION", 1)
        };
//        HRESULT hr = S_OK;
//        CPUTResult result;
//        cString FullPath, FinalPath;
//        ID3DBlob *pPSBlob = NULL;
        m_renderTargets = new RenderTargets();
        gl = GLFuncProviderFactory.getGLFuncProvider();
        m_areaTexSRV = CreateTextureFromRAWMemory( AREATEX_WIDTH, AREATEX_HEIGHT, AREATEX_PITCH, AREATEX_SIZE, GLenum.GL_RG8, "shader_libs/AntiAliasing/AreaTex.dat" );
        m_searchTexSRV = CreateTextureFromRAWMemory( SEARCHTEX_WIDTH, SEARCHTEX_HEIGHT, SEARCHTEX_PITCH, SEARCHTEX_SIZE, GLenum.GL_R8, "shader_libs/AntiAliasing/SearchTex.dat" );

        // Create constant buffer
        /*D3D11_BUFFER_DESC BufferDesc;
        ZeroMemory(&BufferDesc, sizeof(BufferDesc));
        BufferDesc.Usage          = D3D11_USAGE_DYNAMIC;
        BufferDesc.BindFlags      = D3D11_BIND_CONSTANT_BUFFER;
        BufferDesc.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;
        BufferDesc.ByteWidth      = sizeof( SMAAConstants );
        hr  = pD3DDevice->CreateBuffer( &BufferDesc, NULL, &m_constantsBuffer );*/
        m_constantsBuffer = new BufferGL();
        m_constantsBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, SMAAConstants.SIZE, null, GLenum.GL_DYNAMIC_COPY);
        m_constantsBuffer.unbind();

        /*CPUTAssetLibraryDX11 *pAssetLibrary = (CPUTAssetLibraryDX11*)CPUTAssetLibrary::GetAssetLibrary();
        cString OriginalAssetLibraryDirectory = pAssetLibrary->GetShaderDirectoryName();*/

        // vertex shaders
        {

            /*FullPath = OriginalAssetLibraryDirectory + L"SMAA.fx";
            CPUTFileSystem::ResolveAbsolutePathAndFilename(FullPath, &FinalPath);*/

            /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DX11_SMAAEdgeDetectionVS", L"vs_5_0", &pPSBlob, NULL );
            ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
            UNREFERENCED_PARAMETER(result);
            hr = pD3DDevice->CreateVertexShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_SMAAEdgeDetectionVS);*/
            m_SMAAEdgeDetectionProgram = createProgram("EdgeDetection", macros);
            /*m_quad = new SMAAEffect_Quad( pD3DDevice, pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize() );
            SAFE_RELEASE( pPSBlob );*/
            m_quad = ModelGenerator.genRect(-1,-1,+1,+1, true).genVAO();

            /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DX11_SMAABlendingWeightCalculationVS", L"vs_5_0", &pPSBlob, NULL );
            ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
            UNREFERENCED_PARAMETER(result);
            hr = pD3DDevice->CreateVertexShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_SMAABlendingWeightCalculationVS);
            SAFE_RELEASE( pPSBlob );*/
            m_SMAABlendingWeightCalculationProgram = createProgram("BlendingWeightCalculation", macros);

            /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DX11_SMAANeighborhoodBlendingVS", L"vs_5_0", &pPSBlob, NULL );
            ASSERT( CPUTSUCCESS(result), _L("Error compiling Vertex shader:\n\n") );
            UNREFERENCED_PARAMETER(result);
            hr = pD3DDevice->CreateVertexShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_SMAANeighborhoodBlendingVS);
            SAFE_RELEASE( pPSBlob );*/
            m_SMAANeighborhoodBlendingProgram = createProgram("NeighborhoodBlending", macros);
        }

        // pixel shaders
        {
            /*result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DX11_SMAAEdgeDetectionPS", L"ps_5_0", &pPSBlob, NULL );
            ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
            UNREFERENCED_PARAMETER(result);
            hr = pD3DDevice->CreatePixelShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_SMAAEdgeDetectionPS);
            SAFE_RELEASE( pPSBlob );

            result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DX11_SMAABlendingWeightCalculationPS", L"ps_5_0", &pPSBlob, NULL );
            ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
            UNREFERENCED_PARAMETER(result);
            hr = pD3DDevice->CreatePixelShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_SMAABlendingWeightCalculationPS);
            SAFE_RELEASE( pPSBlob );

            result = pAssetLibrary->CompileShaderFromFile(FinalPath, L"DX11_SMAANeighborhoodBlendingPS", L"ps_5_0", &pPSBlob, NULL );
            ASSERT( CPUTSUCCESS(result), _L("Error compiling Pixel shader:\n\n") );
            UNREFERENCED_PARAMETER(result);
            hr = pD3DDevice->CreatePixelShader( pPSBlob->GetBufferPointer(), pPSBlob->GetBufferSize(),NULL, &m_SMAANeighborhoodBlendingPS);
            SAFE_RELEASE( pPSBlob );*/
        }
    }

    static Texture2D CreateTextureFromRAWMemory(int width, int height, int pitch, int size, int format, String filename){
        try {
            byte[] data = FileUtils.loadBytes(filename);
            if(size != data.length)
                throw new IllegalArgumentException();
            Texture2DDesc desc = new Texture2DDesc(width,height, format);
            TextureDataDesc dataDesc = new TextureDataDesc(TextureUtils.measureFormat(format), TextureUtils.measureDataType(format), data);
            return TextureUtils.createTexture2D(desc, dataDesc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static GLSLProgram createProgram(String shaderName, Macro... macros){
        try {
            GLSLProgram program =  GLSLProgram.createFromFiles(
                    String.format("shader_libs/AntiAliasing/PostProcessingSMAA_%sVS.vert", shaderName),
                    String.format("shader_libs/AntiAliasing/PostProcessingSMAA_%sPS.frag", shaderName),
                    macros);
            program.setName(shaderName);
            return program;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void OnSize(/*ID3D11Device* pD3DDevice,*/ int width, int height){
        m_ScreenHeight = (float)height;
        m_ScreenWidth = (float)width;

        CommonUtil.safeRelease( m_workingColorTexture );
        CommonUtil.safeRelease( m_edgesTex );
        CommonUtil.safeRelease( m_blendTex );
        CommonUtil.safeRelease( m_depthStencilTex    );

        // Create working textures
        /*D3D11_TEXTURE2D_DESC td;
        DXGI_SAMPLE_DESC SampleDesc;
        SampleDesc.Count = 1;
        SampleDesc.Quality = 0;
        memset(&td, 0, sizeof(td));
        td.ArraySize = 1;
        td.Format = DXGI_FORMAT_R8G8B8A8_UNORM_SRGB;
        td.Height = (UINT)m_ScreenHeight;
        td.Width = (UINT)m_ScreenWidth;
        td.CPUAccessFlags = 0;
        td.MipLevels = 1;
        td.MiscFlags = 0;
        td.SampleDesc = SampleDesc;
        td.Usage = D3D11_USAGE_DEFAULT;
        assert( td.SampleDesc.Count == 1 ); // other not supported yet
        td.BindFlags = D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE;
        hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_workingColorTexture ) ;*/
        m_workingColorTexture =
                TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), null);

        /*{
            CD3D11_SHADER_RESOURCE_VIEW_DESC srvd( m_workingColorTexture, D3D11_SRV_DIMENSION_TEXTURE2D, td.Format );
            CD3D11_RENDER_TARGET_VIEW_DESC dsvDesc( m_workingColorTexture, D3D11_RTV_DIMENSION_TEXTURE2D, td.Format );
            hr =  pD3DDevice->CreateShaderResourceView( m_workingColorTexture, &srvd, &m_workingColorTextureSRV );
            hr = pD3DDevice->CreateRenderTargetView( m_workingColorTexture, &dsvDesc, &m_workingColorTextureRTV );
        }*/

        /*td.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
        hr = pD3DDevice->CreateTexture2D( &td, NULL, &m_edgesTex );
        {
            CD3D11_SHADER_RESOURCE_VIEW_DESC srvd( m_edgesTex, D3D11_SRV_DIMENSION_TEXTURE2D, td.Format );
            CD3D11_RENDER_TARGET_VIEW_DESC dsvDesc( m_edgesTex, D3D11_RTV_DIMENSION_TEXTURE2D, td.Format );
            hr = ( pD3DDevice->CreateShaderResourceView( m_edgesTex, &srvd, &m_edgesTexSRV ) );
            hr = ( pD3DDevice->CreateRenderTargetView( m_edgesTex, &dsvDesc, &m_edgesTexRTV ) );
        }*/
        m_edgesTex = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RG8), null);
        /*hr = ( pD3DDevice->CreateTexture2D( &td, NULL, &m_blendTex ) );
        {
            CD3D11_SHADER_RESOURCE_VIEW_DESC srvd( m_blendTex, D3D11_SRV_DIMENSION_TEXTURE2D, td.Format );
            CD3D11_RENDER_TARGET_VIEW_DESC dsvDesc( m_blendTex, D3D11_RTV_DIMENSION_TEXTURE2D, td.Format );
            hr = ( pD3DDevice->CreateShaderResourceView( m_blendTex, &srvd, &m_blendTexSRV ) );
            hr = ( pD3DDevice->CreateRenderTargetView( m_blendTex, &dsvDesc, &m_blendTexRTV ) );
        }*/
        m_blendTex = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_RGBA8), null);

        /*td.Format       = DXGI_FORMAT_D24_UNORM_S8_UINT;
        td.BindFlags    = D3D11_BIND_DEPTH_STENCIL;
        hr = ( pD3DDevice->CreateTexture2D( &td, NULL, &m_depthStencilTex ) );
        {
            CD3D11_DEPTH_STENCIL_VIEW_DESC dsvd( m_depthStencilTex, D3D11_DSV_DIMENSION_TEXTURE2D, td.Format );
            hr = ( pD3DDevice->CreateDepthStencilView( m_depthStencilTex, &dsvd, &m_depthStencilTexDSV ) );
        }*/
        m_depthStencilTex = TextureUtils.createTexture2D(new Texture2DDesc(width, height, GLenum.GL_DEPTH24_STENCIL8), null);
    }

    void    Draw(/*ID3D11DeviceContext* pD3DImmediateContext,*/ float PPAADEMO_gEdgeDetectionThreshold, Texture2D sourceColorSRV_SRGB,
                 Texture2D sourceColorSRV_UNORM, boolean showEdges ){
        /*ID3D11ShaderResourceView * nullSRVs[4] = { NULL };
        // Backup current render/stencil
        ID3D11RenderTargetView * oldRenderTargetViews[4] = { NULL };
        ID3D11DepthStencilView * oldDepthStencilView = NULL;
        context->OMGetRenderTargets( _countof(oldRenderTargetViews), oldRenderTargetViews, &oldDepthStencilView );*/
        gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);

        // update consts
        {
            /*HRESULT hr;

            D3D11_MAPPED_SUBRESOURCE MappedResource;
            hr = context->Map( m_constantsBuffer, 0, D3D11_MAP_WRITE_DISCARD, 0, &MappedResource );
            SMAAConstants *pCB = (SMAAConstants *) MappedResource.pData;*/

            SMAAConstants pCB = m_constants;
            pCB.c_PixelSize[ 0 ]   = 1.0f / m_ScreenWidth;
            pCB.c_PixelSize[ 1 ]   = 1.0f / m_ScreenHeight;

            pCB.c_Dummy[0]             = 0;
            pCB.c_Dummy[1]             = 0;

            pCB.c_SubsampleIndices[0]  = 0;
            pCB.c_SubsampleIndices[1]  = 0;
            pCB.c_SubsampleIndices[2]  = 0;
            pCB.c_SubsampleIndices[3]  = 0;

            /*context->Unmap( m_constantsBuffer, 0 );

            context->VSSetConstantBuffers( 0, 1, &m_constantsBuffer );
            context->PSSetConstantBuffers( 0, 1, &m_constantsBuffer );
            context->CSSetConstantBuffers( 0, 1, &m_constantsBuffer );*/
            ByteBuffer bytes = CacheBuffer.getCachedByteBuffer(SMAAConstants.SIZE);
            pCB.store(bytes).flip();
            m_constantsBuffer.update(0, bytes);
            gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0, m_constantsBuffer.getBuffer());
        }

        {
            // Setup the viewport to match the backbuffer
            /*D3D11_VIEWPORT vp;
            vp.Width = (float)m_ScreenWidth;
            vp.Height = (float)m_ScreenHeight;
            vp.MinDepth = 0;
            vp.MaxDepth = 1;
            vp.TopLeftX = 0;
            vp.TopLeftY = 0;
            context->RSSetViewports( 1, &vp );*/
            gl.glViewport(0,0, (int)m_ScreenWidth, (int)m_ScreenHeight);
        }


        /*float    f4zero[4] = { 0, 0, 0, 0 };
        context->ClearRenderTargetView( m_edgesTexRTV, f4zero );
        context->ClearRenderTargetView( m_blendTexRTV, f4zero );
        context->ClearDepthStencilView( m_depthStencilTexDSV, D3D11_CLEAR_STENCIL, 0.0f, 0 );*/
        gl.glClearTexImage(m_edgesTex.getTexture(), 0, TextureUtils.measureFormat(m_edgesTex.getFormat()), TextureUtils.measureDataType(m_edgesTex.getFormat()), null);
        gl.glClearTexImage(m_blendTex.getTexture(), 0, TextureUtils.measureFormat(m_blendTex.getFormat()), TextureUtils.measureDataType(m_blendTex.getFormat()), null);
        gl.glDisable(GLenum.GL_BLEND);
        gl.glDisable(GLenum.GL_CULL_FACE);
//        m_quad->setInputLayout( context );

        // common textures
        /*context->PSSetShaderResources( 4, 1, &m_areaTexSRV );
        context->PSSetShaderResources( 5, 1, &m_searchTexSRV ); TODO*/

        // common samplers
        /*ID3D11SamplerState * samplerStates[2] = { MySample::GetSS_LinearClamp(), MySample::GetSS_PointClamp() };
        context->PSSetSamplers( 0, 2, samplerStates );TODO*/

        // Detect edges
        {
            /*context->VSSetShader( m_SMAAEdgeDetectionVS, NULL, 0 );
            context->PSSetShader( m_SMAAEdgeDetectionPS, NULL, 0 );*/
            m_SMAAEdgeDetectionProgram.enable();

            /*context->OMSetRenderTargets( 1, &m_edgesTexRTV, m_depthStencilTexDSV );*/
            m_renderTargets.bind();
            m_renderTargets.setRenderTextures(new TextureGL[]{m_edgesTex, m_depthStencilTex}, null);
            gl.glClearBufferfi(GLenum.GL_DEPTH_STENCIL, 0, 1.0f, 0);
            /*context->PSSetShaderResources( 0, 1, &sourceColorSRV_SRGB );
            context->PSSetShaderResources( 1, 1, &sourceColorSRV_UNORM );*/
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(sourceColorSRV_SRGB.getTarget(), sourceColorSRV_SRGB.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(sourceColorSRV_UNORM.getTarget(), sourceColorSRV_UNORM.getTexture());

            /*context->OMSetDepthStencilState( MySample::GetDSS_DepthDisabled_ReplaceStencil(), 1 );
            context->OMSetBlendState( MySample::GetBS_Opaque(), f4zero, 0xFFFFFFFF );*/
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_REPLACE);
            gl.glStencilFunc(GLenum.GL_ALWAYS, 1, 1);

//            m_quad->draw( context );
            drawQuad();

            // this doesn't work unfortunately, there's some kind of mismatch - please don't change without confirming that before/after screen shots are pixel identical
            //MySample::FullscreenPassDraw( context, m_SMAAEdgeDetectionPS, m_SMAAEdgeDetectionVS, MySample::GetBS_Opaque(), MySample::GetDSS_DepthDisabled_ReplaceStencil(), 1, 0.0f );

//            context->PSSetShaderResources( 0, 2, nullSRVs );
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(sourceColorSRV_SRGB.getTarget(), 0);
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(sourceColorSRV_UNORM.getTarget(), 0);

            if(!m_prinOnce){
                m_SMAAEdgeDetectionProgram.printPrograminfo();
            }
        }

        // Blending weight calculation
        {
            /*context->VSSetShader( m_SMAABlendingWeightCalculationVS, NULL, 0 );
            context->PSSetShader( m_SMAABlendingWeightCalculationPS, NULL, 0 );*/
            m_SMAABlendingWeightCalculationProgram.enable();
            /*context->OMSetRenderTargets( 1, &m_blendTexRTV, m_depthStencilTexDSV );
            context->PSSetShaderResources( 2, 1, &m_edgesTexSRV );*/
            m_renderTargets.setRenderTextures(new TextureGL[]{m_blendTex, m_depthStencilTex}, null);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(m_edgesTex.getTarget(), m_edgesTex.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(m_areaTexSRV.getTarget(), m_areaTexSRV.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE2);
            gl.glBindTexture(m_searchTexSRV.getTarget(), m_searchTexSRV.getTexture());

            /*context->OMSetDepthStencilState( MySample::GetDSS_DepthDisabled_UseStencil(), 1 );  TODO
            context->OMSetBlendState( MySample::GetBS_Opaque(), f4zero, 0xFFFFFFFF );*/
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glEnable(GLenum.GL_STENCIL_TEST);
            gl.glStencilOp(GLenum.GL_KEEP, GLenum.GL_KEEP, GLenum.GL_REPLACE);
            gl.glStencilFunc(GLenum.GL_EQUAL, 1, 1);

//            m_quad->draw( context );
            drawQuad();

            if(!m_prinOnce){
                m_SMAABlendingWeightCalculationProgram.printPrograminfo();
            }

            // this doesn't work unfortunately, there's some kind of mismatch - please don't change without confirming that before/after screen shots are pixel identical
            //MySample::FullscreenPassDraw( context, m_SMAABlendingWeightCalculationPS, m_SMAABlendingWeightCalculationVS, MySample::GetBS_Opaque(), MySample::GetDSS_DepthDisabled_UseStencil(), 1, 0.0f );
        }

        // Neighborhood blending (apply)
        {
            /*context->VSSetShader( m_SMAANeighborhoodBlendingVS, NULL, 0 );
            context->PSSetShader( m_SMAANeighborhoodBlendingPS, NULL, 0 );*/
            m_SMAANeighborhoodBlendingProgram.enable();

            /*context->OMSetRenderTargets( 1, oldRenderTargetViews, m_depthStencilTexDSV );
            context->PSSetShaderResources( 0, 1, &sourceColorSRV_SRGB );
            context->PSSetShaderResources( 1, 1, &sourceColorSRV_UNORM );
            context->PSSetShaderResources( 3, 1, &m_blendTexSRV );*/
            gl.glBindFramebuffer(GLenum.GL_FRAMEBUFFER, 0);
            gl.glActiveTexture(GLenum.GL_TEXTURE0);
            gl.glBindTexture(sourceColorSRV_UNORM.getTarget(), sourceColorSRV_UNORM.getTexture());
            gl.glActiveTexture(GLenum.GL_TEXTURE1);
            gl.glBindTexture(m_blendTex.getTarget(), m_blendTex.getTexture());

            /*context->OMSetDepthStencilState( MySample::GetDSS_DepthDisabled_NoDepthWrite(), 1 );  TODO
            context->OMSetBlendState( MySample::GetBS_Opaque(), f4zero, 0xFFFFFFFF );*/
            gl.glDisable(GLenum.GL_DEPTH_TEST);
            gl.glDisable(GLenum.GL_STENCIL_TEST);
            gl.glDepthMask(false);

//            m_quad->draw( context );
            drawQuad();
            gl.glDepthMask(true);

            if(!m_prinOnce){
                m_SMAANeighborhoodBlendingProgram.printPrograminfo();
            }

            // this doesn't work unfortunately, there's some kind of mismatch - please don't change without confirming that before/after screen shots are pixel identical
            //MySample::FullscreenPassDraw( context, m_SMAANeighborhoodBlendingPS, m_SMAANeighborhoodBlendingVS, MySample::GetBS_Opaque(), MySample::GetDSS_DepthDisabled_NoDepthWrite(), 1, 0.0f );
        }

        /*ID3D11ShaderResourceView * nullSRV[8] = { NULL };
        context->PSSetShaderResources( 0, _countof(nullSRV), nullSRV );*/
        gl.glActiveTexture(GLenum.GL_TEXTURE0);
        gl.glBindTexture(sourceColorSRV_SRGB.getTarget(), 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE1);
        gl.glBindTexture(sourceColorSRV_UNORM.getTarget(), 0);
        gl.glActiveTexture(GLenum.GL_TEXTURE2);
        gl.glBindTexture(m_blendTex.getTarget(), 0);
        gl.glBindBufferBase(GLenum.GL_UNIFORM_BUFFER, 0,0);

        // Restore old stencil/depth
        /*context->OMSetRenderTargets( _countof(oldRenderTargetViews), oldRenderTargetViews, oldDepthStencilView );
        for( int i = 0; i < _countof(oldRenderTargetViews); i++ )
            SAFE_RELEASE( oldRenderTargetViews[i] );
        SAFE_RELEASE( oldDepthStencilView );*/
        m_prinOnce = true;
    }

    private void drawQuad(){
        gl.glDrawArrays(GLenum.GL_TRIANGLES, 0, 3);

//        m_quad.bind();
//        m_quad.draw(GLenum.GL_TRIANGLE_STRIP);
//        m_quad.unbind();
    }

    @Override
    public void dispose() {
        CommonUtil.safeRelease( m_SMAAEdgeDetectionProgram );
        CommonUtil.safeRelease( m_SMAABlendingWeightCalculationProgram );
        CommonUtil.safeRelease( m_SMAANeighborhoodBlendingProgram );

        CommonUtil.safeRelease( m_constantsBuffer );
        //SAFE_RELEASE( m_areaTex         );
        CommonUtil.safeRelease( m_areaTexSRV      );
        //SAFE_RELEASE( m_searchTex       );
        CommonUtil.safeRelease( m_searchTexSRV    );

        CommonUtil.safeRelease( m_workingColorTexture );
        CommonUtil.safeRelease( m_edgesTex );
        CommonUtil.safeRelease( m_blendTex );
        CommonUtil.safeRelease( m_depthStencilTex    );
    }

    public static class SMAAConstants implements Readable{
        static final int SIZE = Vector4f.SIZE * 2;
        final float[] c_PixelSize = new float[2];
        final float[] c_Dummy = new float[2];

        // This is only required for temporal modes (SMAA T2x).
        float[] c_SubsampleIndices = new float[4];

        @Override
        public ByteBuffer store(ByteBuffer buf) {
            CacheBuffer.put(buf, c_PixelSize);
            CacheBuffer.put(buf, c_Dummy);
            CacheBuffer.put(buf, c_SubsampleIndices);
            return buf;
        }

        //float threshld;
        //float maxSearchSteps;
        //float maxSearchStepsDiag;
        //float cornerRounding;
    }

    public static void main(String[] args){
        convertFiles("E:\\SDK\\smaa\\Textures\\AreaTex.h");
        convertFiles("E:\\SDK\\smaa\\Textures\\SearchTex.h");
    }

    private static void convertFiles(String filename){
        int dot = filename.lastIndexOf('.');
        String _filename = dot > 0 ? filename.substring(0, dot) : filename;
        File file = new File(filename);
        File outFile = new File(_filename + ".dat");

        int lineNumber = 0;
        String token = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));

            String line;
            boolean inArrayBlock = false;
            Tag: while((line = in.readLine()) != null){
                if(inArrayBlock){
                    StringTokenizer tokenizer = new StringTokenizer(line, " \t\f,");
                    while (tokenizer.hasMoreElements()){
                        token = tokenizer.nextToken().trim();
                        if(token.equals("") || token.equals("};"))
                            break Tag;
                        int value = Integer.parseInt(token.substring(2), 16);
                        out.write(value);
                    }
                }else if(line.startsWith("static const unsigned char")){
                    inArrayBlock = true;
                }

                lineNumber ++;
            }

            in.close();
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e){
            System.out.println("Error occored in the number: " + lineNumber + ", in the file: " + filename + ", the token is: " + token);
            e.printStackTrace();

        }
    }
}
